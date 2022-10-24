/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.manager.rm.service.impl

import org.apache.linkis.common.ServiceInstance
import org.apache.linkis.common.exception.LinkisRetryException
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.governance.common.conf.GovernanceCommonConf
import org.apache.linkis.manager.common.conf.RMConfiguration
import org.apache.linkis.manager.common.entity.node.{AMEMNode, AMEngineNode, InfoRMNode}
import org.apache.linkis.manager.common.entity.persistence.{PersistenceLabel, PersistenceResource}
import org.apache.linkis.manager.common.entity.resource._
import org.apache.linkis.manager.common.exception.{RMErrorException, RMWarnException}
import org.apache.linkis.manager.common.utils.{ManagerUtils, ResourceUtils}
import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactoryContext
import org.apache.linkis.manager.label.constant.LabelKeyConstant
import org.apache.linkis.manager.label.entity.Label
import org.apache.linkis.manager.label.entity.em.EMInstanceLabel
import org.apache.linkis.manager.label.entity.engine.EngineInstanceLabel
import org.apache.linkis.manager.label.service.NodeLabelService
import org.apache.linkis.manager.persistence.{
  LabelManagerPersistence,
  NodeManagerPersistence,
  NodeMetricManagerPersistence,
  ResourceManagerPersistence
}
import org.apache.linkis.manager.rm.{
  AvailableResource,
  NotEnoughResource,
  ResourceInfo,
  ResultResource
}
import org.apache.linkis.manager.rm.domain.RMLabelContainer
import org.apache.linkis.manager.rm.exception.RMErrorCode
import org.apache.linkis.manager.rm.external.service.ExternalResourceService
import org.apache.linkis.manager.rm.service.{
  LabelResourceService,
  RequestResourceService,
  ResourceLockService,
  ResourceManager
}
import org.apache.linkis.manager.rm.utils.RMUtils
import org.apache.linkis.server.BDPJettyServerHelper

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.util
import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions._

import com.google.common.collect.Lists

@Component
class DefaultResourceManager extends ResourceManager with Logging with InitializingBean {

  @Autowired
  private var resourceManagerPersistence: ResourceManagerPersistence = _

  @Autowired
  private var nodeManagerPersistence: NodeManagerPersistence = _

  @Autowired
  private var resourceLockService: ResourceLockService = _

  @Autowired
  private var labelResourceService: LabelResourceService = _

  @Autowired
  private var externalResourceService: ExternalResourceService = _

  @Autowired
  private var resourceLogService: ResourceLogService = _

  @Autowired
  private var labelManagerPersistence: LabelManagerPersistence = _

  @Autowired
  private var nodeMetricManagerPersistence: NodeMetricManagerPersistence = _

  @Autowired
  private var nodeLabelService: NodeLabelService = _

  private var requestResourceServices: Array[RequestResourceService] = _

  private val gson = BDPJettyServerHelper.gson

  private val labelFactory = LabelBuilderFactoryContext.getLabelBuilderFactory

  override def afterPropertiesSet(): Unit = {
    requestResourceServices = Array(
      new DefaultReqResourceService(labelResourceService),
      new DriverAndYarnReqResourceService(labelResourceService, externalResourceService)
    )
    // submit force release timeout lock job
    Utils.defaultScheduler.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = {
          logger.info("Start force release timeout locks")
          resourceLockService.clearTimeoutLock(RMConfiguration.LOCK_RELEASE_TIMEOUT.getValue.toLong)
        }
      },
      RMConfiguration.LOCK_RELEASE_CHECK_INTERVAL.getValue.toLong,
      RMConfiguration.LOCK_RELEASE_CHECK_INTERVAL.getValue.toLong,
      TimeUnit.MILLISECONDS
    )
  }

  /**
   * The registration method is mainly used to notify all RM nodes (including the node)
   * 该注册方法，主要是用于通知所有的RM节点（包括本节点）
   */
  override def register(serviceInstance: ServiceInstance, resource: NodeResource): Unit = {
    logger.info(s"Start processing registration of ServiceInstance: ${serviceInstance}")
    val eMInstanceLabel =
      LabelBuilderFactoryContext.getLabelBuilderFactory.createLabel(classOf[EMInstanceLabel])
    eMInstanceLabel.setServiceName(serviceInstance.getApplicationName)
    eMInstanceLabel.setInstance(serviceInstance.getInstance)
    val labelContainer = new RMLabelContainer(Lists.newArrayList(eMInstanceLabel))

    Utils.tryFinally {
      // lock labels
      labelContainer.getResourceLabels.foreach {
        case label: Label[_] =>
          labelContainer.setCurrentLabel(label.asInstanceOf[Label[_]])
          resourceLockService.tryLock(labelContainer)
        case _ =>
      }
      val emResource = labelResourceService.getLabelResource(eMInstanceLabel)
      if (emResource != null) {
        logger.warn(s"${serviceInstance} has been registered, now update resource.")
        if (!emResource.getResourceType.equals(resource.getResourceType)) {
          throw new RMErrorException(
            RMErrorCode.LABEL_DUPLICATED.getCode,
            s"${serviceInstance} has been registered in ${emResource.getResourceType}, cannot be updated to ${resource.getResourceType}"
          )
        }
      }
      // TODO get ID Label set label resource
      Utils.tryCatch {
        labelResourceService.setLabelResource(
          eMInstanceLabel,
          resource,
          eMInstanceLabel.getStringValue
        )
        resourceLogService.success(
          ChangeType.ECM_INIT,
          resource.getMaxResource,
          null,
          eMInstanceLabel
        )
      } {
        case exception: Exception => {
          resourceLogService.failed(
            ChangeType.ECM_INIT,
            resource.getMaxResource,
            null,
            eMInstanceLabel,
            exception
          )
          throw exception
        }
        case _ =>
      }
    } {
      // 5.Release lock(释放锁)
      resourceLockService.unLock(labelContainer)
      logger.info(s"finished processing registration of ${serviceInstance}")
    }
  }

  /**
   * The registration method is mainly used to notify all RM nodes (including the node), and the
   * instance is offline. 该注册方法，主要是用于通知所有的RM节点（包括本节点），下线该实例
   */
  override def unregister(serviceInstance: ServiceInstance): Unit = {
    // TODO get ID Label
    val eMInstanceLabel =
      LabelBuilderFactoryContext.getLabelBuilderFactory.createLabel(classOf[EMInstanceLabel])
    eMInstanceLabel.setServiceName(serviceInstance.getApplicationName)
    eMInstanceLabel.setInstance(serviceInstance.getInstance)
    val labelContainer = new RMLabelContainer(Lists.newArrayList(eMInstanceLabel))

    Utils.tryFinally {
      Utils.tryCatch {
        nodeManagerPersistence.getEngineNodeByEM(serviceInstance).foreach { node =>
          val engineInstanceLabel = LabelBuilderFactoryContext.getLabelBuilderFactory
            .createLabel(classOf[EngineInstanceLabel])
          engineInstanceLabel.setInstance(node.getServiceInstance.getInstance)
          engineInstanceLabel.setServiceName(node.getServiceInstance.getApplicationName)
          labelResourceService.removeResourceByLabel(engineInstanceLabel)
        }
        labelResourceService.removeResourceByLabel(eMInstanceLabel)
        labelContainer.setCurrentLabel(eMInstanceLabel)
        resourceLockService.unLock(labelContainer)
        resourceLogService.success(
          ChangeType.ECM_CLEAR,
          Resource.initResource(ResourceType.LoadInstance),
          null,
          eMInstanceLabel
        )
      } {
        case exception: Exception => {
          resourceLogService.failed(
            ChangeType.ECM_CLEAR,
            Resource.initResource(ResourceType.LoadInstance),
            null,
            eMInstanceLabel,
            exception
          )
        }
        case _ =>
      }

    } {
      logger.info(
        s"ECMResourceClear:${serviceInstance}, usedResource:${Resource.initResource(ResourceType.Default).toJson}"
      )
    }
  }

  /**
   * Request resources, if not successful, return directly 请求资源，如果不成功，直接返回
   *
   * @param labels
   * @param resource
   * @return
   */
  override def requestResource(
      labels: util.List[Label[_]],
      resource: NodeResource
  ): ResultResource = {
    requestResource(labels, resource, -1)
  }

  /**
   * Request resources and wait for a certain amount of time until the requested resource is met
   * 请求资源，并等待一定的时间，直到满足请求的资源
   *
   * @param labels
   * @param resource
   * @param wait
   * @return
   */
  override def requestResource(
      labels: util.List[Label[_]],
      resource: NodeResource,
      wait: Long
  ): ResultResource = {
    val labelContainer = labelResourceService.enrichLabels(labels)
    logger.debug(
      "start processing request resource for labels [" + labelContainer + "] and resource [" + resource + "]"
    )

    Utils.tryFinally {
      // lock labels
      if (wait <= 0) {
        tryLock(labelContainer)
      } else {
        tryLock(labelContainer, wait)
      }

      // check resource with lock
      val requestResourceService = getRequestResourceService(resource.getResourceType)
      resource.setLockedResource(resource.getMinResource)

      val labelResourceList = new util.HashMap[String, NodeResource]()
      labelContainer.getResourceLabels.foreach(label => {
        // check all resource of label
        Utils.tryCatch {
          labelContainer.setCurrentLabel(label)
          if (!requestResourceService.canRequest(labelContainer, resource)) {
            return NotEnoughResource(s"Labels:${label.getStringValue} not enough resource")
          }
        } { case exception: RMWarnException =>
          return NotEnoughResource(exception.getMessage)
        }
        val usedResource = labelResourceService.getLabelResource(label)
        if (usedResource == null) {
          val msg =
            s"Resource label: ${label.getStringValue} has no usedResource, please check, refuse request usedResource"
          logger.info(msg)
          throw new RMErrorException(110022, msg)
        }
        labelResourceList.put(label.getStringValue, usedResource)
      })
      val tickedId = UUID.randomUUID().toString
      labelContainer.getResourceLabels.foreach {
        case label: Label[_] =>
          val labelResource = labelResourceList.get(label.getStringValue)
          if (labelResource != null) {
            labelResource.setLeftResource(
              labelResource.getLeftResource - resource.getLockedResource
            )
            labelResource.setLockedResource(
              labelResource.getLockedResource + resource.getLockedResource
            )
            labelResourceService.setLabelResource(
              label,
              labelResource,
              labelContainer.getCombinedUserCreatorEngineTypeLabel.getStringValue
            )
            logger.info(s"ResourceChanged:${label.getStringValue} --> ${labelResource}")
            resourceCheck(label, labelResource)
            if (
                label.getClass.isAssignableFrom(
                  labelContainer.getCombinedUserCreatorEngineTypeLabel.getClass
                )
            ) {
              resourceLogService.recordUserResourceAction(
                labelContainer,
                tickedId,
                ChangeType.ENGINE_REQUEST,
                resource.getLockedResource
              )
            }
          }
        case _ =>
      }
      // record engine locked resource
      val emNode = new AMEMNode
      emNode.setServiceInstance(labelContainer.getEMInstanceLabel.getServiceInstance)
      val engineNode = new AMEngineNode
      engineNode.setEMNode(emNode)
      engineNode.setServiceInstance(ServiceInstance(labelContainer.getEngineServiceName, tickedId))
      engineNode.setNodeResource(resource)
      nodeManagerPersistence.addEngineNode(engineNode)

      val engineInstanceLabel =
        LabelBuilderFactoryContext.getLabelBuilderFactory.createLabel(classOf[EngineInstanceLabel])
      engineInstanceLabel.setServiceName(labelContainer.getEngineServiceName)
      engineInstanceLabel.setInstance(tickedId)

      nodeLabelService.addLabelToNode(engineNode.getServiceInstance, engineInstanceLabel)

      labelResourceService.setEngineConnLabelResource(
        engineInstanceLabel,
        resource,
        labelContainer.getCombinedUserCreatorEngineTypeLabel.getStringValue
      )

      val persistenceLabel =
        labelFactory.convertLabel(engineInstanceLabel, classOf[PersistenceLabel])
      val persistenceEngineLabel = labelManagerPersistence.getLabelByKeyValue(
        persistenceLabel.getLabelKey,
        persistenceLabel.getStringValue
      )

      // fire timeout check scheduled job
      if (RMConfiguration.RM_WAIT_EVENT_TIME_OUT.getValue > 0)
        Utils.defaultScheduler.schedule(
          new UnlockTimeoutResourceRunnable(labels, persistenceEngineLabel, tickedId),
          RMConfiguration.RM_WAIT_EVENT_TIME_OUT.getValue,
          TimeUnit.MILLISECONDS
        )
      AvailableResource(tickedId)
    } {
      // 5.Release lock(释放锁)
      resourceLockService.unLock(labelContainer)
      logger.debug(
        s"finished processing requestResource of labels ${labels} and resource ${resource}"
      )
    }
  }

  def resourceCheck(label: Label[_], labelResource: NodeResource): Unit = {
    if (labelResource != null && label != null) {
      val resourceInit = Resource.initResource(labelResource.getResourceType)
      if (
          labelResource.getLockedResource < resourceInit ||
          labelResource.getUsedResource < resourceInit ||
          labelResource.getLeftResource < resourceInit
      ) {
        logger.error(
          s"found error resource! resource label:${label.getStringValue}, resource:${labelResource}, please check!"
        )
      }
    }
  }

  /**
   * When the resource is instantiated, the total amount of resources actually occupied is returned.
   * 当资源被实例化后，返回实际占用的资源总量
   *
   * @param labels
   *   In general, resourceReleased will release the resources occupied by the user, but if the
   *   process that uses the resource does not have time to call the resourceReleased method to die,
   *   you need to unregister to release the resource.
   * @param usedResource
   */
  override def resourceUsed(labels: util.List[Label[_]], usedResource: NodeResource): Unit = {
    val labelContainer = labelResourceService.enrichLabels(labels)
    if (null == labelContainer.getEngineInstanceLabel) {
      throw new RMErrorException(
        RMErrorCode.LABEL_RESOURCE_NOT_FOUND.getCode,
        "engine instance label is null"
      )
    }
    var lockedResource: NodeResource = null
    var persistenceResource: PersistenceResource = null
    try {
      persistenceResource =
        labelResourceService.getPersistenceResource(labelContainer.getEngineInstanceLabel)
      lockedResource = ResourceUtils.fromPersistenceResource(persistenceResource)
    } catch {
      case e: NullPointerException =>
        logger.error(
          s"EngineInstanceLabel [${labelContainer.getEngineInstanceLabel}] cause NullPointerException"
        )
        throw e
    }
    val nodeInstance =
      nodeManagerPersistence.getEngineNode(labelContainer.getEngineInstanceLabel.getServiceInstance)
    if (nodeInstance == null) {
      throw new RMErrorException(
        RMErrorCode.LABEL_RESOURCE_NOT_FOUND.getCode,
        s"No serviceInstance found by engine ${labelContainer.getEngineInstanceLabel}, current label resource ${lockedResource}"
      )
    }
    if (
        lockedResource == null || lockedResource.getLockedResource <= Resource.initResource(
          lockedResource.getResourceType
        )
    ) {
      throw new RMErrorException(
        RMErrorCode.LABEL_RESOURCE_NOT_FOUND.getCode,
        s"No locked resource found by engine ${labelContainer.getEngineInstanceLabel}, current label resource ${lockedResource}"
      )
    }
    logger.info(
      s"resourceUsed ready:${labelContainer.getEMInstanceLabel.getServiceInstance}, used resource ${lockedResource.getLockedResource}"
    )
    Utils.tryFinally {
      // lock labels
      tryLock(labelContainer)
      // check again after lock resource
      lockedResource = labelResourceService.getLabelResource(labelContainer.getEngineInstanceLabel)
      if (
          lockedResource == null || lockedResource.getLockedResource <= Resource.initResource(
            lockedResource.getResourceType
          )
      ) {
        throw new RMErrorException(
          RMErrorCode.LABEL_RESOURCE_NOT_FOUND.getCode,
          s"No locked resource found by engine ${labelContainer.getEngineInstanceLabel}, current label resource ${lockedResource}"
        )
      }
      val addedResource =
        Resource.initResource(lockedResource.getResourceType) + lockedResource.getLockedResource
      labelContainer.getResourceLabels.foreach {
        case engineInstanceLabel: EngineInstanceLabel =>
          Utils.tryCatch {
            lockedResource.setUsedResource(lockedResource.getLockedResource)
            updateYarnApplicationID(usedResource, lockedResource)
            lockedResource.setLockedResource(
              Resource.getZeroResource(lockedResource.getLockedResource)
            )
            labelResourceService.setLabelResource(
              engineInstanceLabel,
              lockedResource,
              labelContainer.getCombinedUserCreatorEngineTypeLabel.getStringValue
            )
            resourceLogService.success(
              ChangeType.ENGINE_INIT,
              lockedResource.getLockedResource,
              engineInstanceLabel
            )
          } {
            case exception: Exception => {
              logger.error(
                s"${engineInstanceLabel.getStringValue} used resource failed!, resource: ${lockedResource}",
                exception
              )
            }
          }
        case label: Label[_] =>
          Utils.tryCatch {
            val labelResource = labelResourceService.getLabelResource(label)
            if (labelResource != null) {
              labelResource.setLockedResource(labelResource.getLockedResource - addedResource)
              if (null == labelResource.getUsedResource)
                labelResource.setUsedResource(Resource.initResource(labelResource.getResourceType))
              labelResource.setUsedResource(labelResource.getUsedResource + addedResource)
              labelResourceService.setLabelResource(
                label,
                labelResource,
                labelContainer.getCombinedUserCreatorEngineTypeLabel.getStringValue
              )
              label match {
                case emLabel: EMInstanceLabel =>
                  resourceLogService.success(
                    ChangeType.ECM_RESOURCE_ADD,
                    lockedResource.getUsedResource,
                    null,
                    emLabel
                  )
                case _ =>
              }
              if (
                  label.getClass.isAssignableFrom(
                    labelContainer.getCombinedUserCreatorEngineTypeLabel.getClass
                  )
              ) {
                resourceLogService.recordUserResourceAction(
                  labelContainer,
                  persistenceResource.getTicketId,
                  ChangeType.ENGINE_INIT,
                  addedResource
                )
              }
              resourceCheck(label, labelResource)
            }
          } {
            case exception: Exception => {
              logger.error(
                s"${label.getStringValue} used resource failed!, resource: ${lockedResource}",
                exception
              )
            }
          }
        case _ =>
      }
    } {
      resourceLockService.unLock(labelContainer)
    }
  }

  private def updateYarnApplicationID(
      nodeResource: NodeResource,
      lockedResource: NodeResource
  ): Unit = {
    lockedResource.getUsedResource match {
      case driverAndYarnResource: DriverAndYarnResource =>
        nodeResource.getUsedResource match {
          case resource: DriverAndYarnResource =>
            val newYarnResource = resource.yarnResource
            val applicationId: String = if (null != newYarnResource) {
              newYarnResource.applicationId
            } else {
              null
            }
            val oriYarnResource = driverAndYarnResource.yarnResource
            val tmpUsedResource = new DriverAndYarnResource(
              driverAndYarnResource.loadInstanceResource,
              new YarnResource(
                oriYarnResource.queueMemory,
                oriYarnResource.queueCores,
                oriYarnResource.queueInstances,
                oriYarnResource.queueName,
                applicationId
              )
            )
            lockedResource.setUsedResource(tmpUsedResource)
          case _ =>
        }
      case yarnResource: YarnResource =>
        nodeResource.getUsedResource match {
          case resource: YarnResource =>
            val tmpYarnResource = new YarnResource(
              yarnResource.queueMemory,
              yarnResource.queueCores,
              yarnResource.queueInstances,
              yarnResource.queueName,
              resource.applicationId
            )
            lockedResource.setUsedResource(tmpYarnResource)
          case _ =>
        }
      case _ =>
    }
  }

  def timeCheck(labelResource: NodeResource, usedResource: NodeResource): Unit = {
    if (labelResource.getCreateTime != null && usedResource.getCreateTime != null) {
      if (labelResource.getCreateTime.getTime > usedResource.getCreateTime.getTime) {
        throw new RMErrorException(
          10022,
          s"no need to clear this labelResource, labelResource:${labelResource} created time is after than usedResource:${usedResource}" +
            s"无需清理该标签的资源,该标签资源的创建时间晚于已用资源的创建时间"
        )
      }
    }
  }

  /**
   * Method called when the resource usage is released 当资源使用完成释放后，调用的方法
   *
   * @param labels
   */
  override def resourceReleased(labels: util.List[Label[_]]): Unit = {
    val labelContainer = labelResourceService.enrichLabels(labels)
    val persistenceResource: PersistenceResource =
      labelResourceService.getPersistenceResource(labelContainer.getEngineInstanceLabel)
    val usedResource = ResourceUtils.fromPersistenceResource(persistenceResource)
    if (usedResource == null) {
      throw new RMErrorException(
        RMErrorCode.LABEL_RESOURCE_NOT_FOUND.getCode,
        s"No used resource found by engine ${labelContainer.getEngineInstanceLabel}"
      )
    }
    logger.info(
      s"resourceRelease ready:${labelContainer.getEngineInstanceLabel.getServiceInstance},current node resource${usedResource}"
    )
    Utils.tryFinally {
      // lock labels
      tryLock(labelContainer, RMUtils.RM_RESOURCE_LOCK_WAIT_TIME.getValue)

      // To avoid concurrent problem, check resource again after lock label
      val usedResource =
        labelResourceService.getLabelResource(labelContainer.getEngineInstanceLabel)
      if (usedResource == null) {
        throw new RMErrorException(
          RMErrorCode.LABEL_RESOURCE_NOT_FOUND.getCode,
          s"No used resource found by engine ${labelContainer.getEngineInstanceLabel}"
        )
      }
      labelContainer.getResourceLabels.foreach {
        case label: Label[_] =>
          Utils.tryCatch {
            if (!label.isInstanceOf[EngineInstanceLabel]) {
              val labelResource = labelResourceService.getLabelResource(label)
              if (labelResource != null) {
                if (label.isInstanceOf[EMInstanceLabel]) timeCheck(labelResource, usedResource)
                if (
                    null != usedResource.getUsedResource && usedResource.getUsedResource != Resource
                      .initResource(usedResource.getResourceType)
                ) {
                  labelResource.setUsedResource(
                    labelResource.getUsedResource - usedResource.getUsedResource
                  )
                  labelResource.setLeftResource(
                    labelResource.getLeftResource + usedResource.getUsedResource
                  )
                }
                if (
                    null != usedResource.getLockedResource && usedResource.getLockedResource != Resource
                      .initResource(usedResource.getResourceType)
                ) {
                  labelResource.setLockedResource(
                    labelResource.getLockedResource - usedResource.getLockedResource
                  )
                  labelResource.setLeftResource(
                    labelResource.getLeftResource + usedResource.getLockedResource
                  )
                }
                labelResourceService.setLabelResource(
                  label,
                  labelResource,
                  labelContainer.getCombinedUserCreatorEngineTypeLabel.getStringValue
                )
                if (
                    label.getClass.isAssignableFrom(
                      labelContainer.getCombinedUserCreatorEngineTypeLabel.getClass
                    )
                ) {
                  if (usedResource.getUsedResource != null) {
                    resourceLogService.recordUserResourceAction(
                      labelContainer,
                      persistenceResource.getTicketId,
                      ChangeType.ENGINE_CLEAR,
                      usedResource.getUsedResource
                    )
                  } else if (usedResource.getLockedResource != null) {
                    resourceLogService.recordUserResourceAction(
                      labelContainer,
                      persistenceResource.getTicketId,
                      ChangeType.ENGINE_CLEAR,
                      usedResource.getLockedResource
                    )
                  }
                }
                label match {
                  case emLabel: EMInstanceLabel =>
                    resourceLogService.success(
                      ChangeType.ECM_Resource_MINUS,
                      labelResource.getUsedResource,
                      null,
                      emLabel
                    )
                  case _ =>
                }
                resourceCheck(label, labelResource)
              }
            }
          } { case exception: Exception =>
            logger.warn(s"Failed to release resource label ${label.getStringValue}", exception)
          }
        case _ =>
      }
      val tmpLabel = labelContainer.getLabels.find(_.isInstanceOf[EngineInstanceLabel]).orNull
      if (tmpLabel != null) {
        val engineInstanceLabel = tmpLabel.asInstanceOf[EngineInstanceLabel]
        Utils.tryCatch {
          labelResourceService.removeResourceByLabel(engineInstanceLabel)
          resourceLogService.success(
            ChangeType.ENGINE_CLEAR,
            usedResource.getUsedResource,
            engineInstanceLabel,
            null
          )
        } {
          case exception: Exception => {
            resourceLogService.failed(
              ChangeType.ENGINE_CLEAR,
              usedResource.getUsedResource,
              engineInstanceLabel,
              null,
              exception
            )
            throw exception
          }
          case _ =>
        }
      }
    } {
      resourceLockService.unLock(labelContainer)
    }
  }

  /**
   * If the IP and port are empty, return the resource status of all modules of a module   * Return
   * the use of this instance resource if there is an IP and port
   *
   * @param serviceInstances
   * @return
   */

  override def getResourceInfo(serviceInstances: Array[ServiceInstance]): ResourceInfo = {
    val resourceInfo = new ResourceInfo(Lists.newArrayList())
    serviceInstances.map { serviceInstance =>
      val rmNode = new InfoRMNode
      var aggregatedResource: NodeResource = null
      serviceInstance.getApplicationName match {
        case GovernanceCommonConf.ENGINE_CONN_SPRING_NAME.getValue => {
          val engineInstanceLabel = LabelBuilderFactoryContext.getLabelBuilderFactory.createLabel(
            classOf[EngineInstanceLabel]
          )
          engineInstanceLabel.setServiceName(serviceInstance.getApplicationName)
          engineInstanceLabel.setInstance(serviceInstance.getInstance)
          aggregatedResource = labelResourceService.getLabelResource(engineInstanceLabel)
        }
        case GovernanceCommonConf.ENGINE_CONN_MANAGER_SPRING_NAME.getValue => {
          val emInstanceLabel =
            LabelBuilderFactoryContext.getLabelBuilderFactory.createLabel(classOf[EMInstanceLabel])
          emInstanceLabel.setServiceName(serviceInstance.getApplicationName)
          emInstanceLabel.setInstance(serviceInstance.getInstance)
          aggregatedResource = labelResourceService.getLabelResource(emInstanceLabel)
        }
      }
      rmNode.setServiceInstance(serviceInstance)
      rmNode.setNodeResource(aggregatedResource)
      resourceInfo.resourceInfo.add(rmNode)
    }
    resourceInfo
  }

  def getRequestResourceService(resourceType: ResourceType): RequestResourceService = {
    val requestResourceService = requestResourceServices.find(_.resourceType == resourceType)
    requestResourceService.getOrElse(
      requestResourceServices.find(_.resourceType == ResourceType.Default).get
    )
  }

  class UnlockTimeoutResourceRunnable(
      labels: util.List[Label[_]],
      persistenceEngineLabel: PersistenceLabel,
      ticketId: String
  ) extends Runnable {

    override def run(): Unit = Utils.tryAndWarnMsg {
      logger.info(
        s"check locked resource of ${persistenceEngineLabel.getStringValue}, ticketId: $ticketId"
      )
      val persistResource = resourceManagerPersistence.getNodeResourceByTicketId(ticketId)
      if (null == persistResource) {
        logger.info(s" ticketId $ticketId relation resource not exists")
        return
      }
      val usedResource = ResourceUtils.fromPersistenceResource(persistResource)
      if (
          usedResource != null
          && usedResource.getLockedResource != null
          && usedResource.getLockedResource > Resource.getZeroResource(
            usedResource.getLockedResource
          )
      ) {
        val dbEngineInstanceLabel = labelManagerPersistence.getLabel(persistenceEngineLabel.getId)
        val currnentEngineInstanceLabel =
          if (null == dbEngineInstanceLabel) persistenceEngineLabel else dbEngineInstanceLabel
        if (
            LabelKeyConstant.ENGINE_INSTANCE_KEY.equalsIgnoreCase(
              currnentEngineInstanceLabel.getLabelKey
            )
        ) {
          ManagerUtils.persistenceLabelToRealLabel(currnentEngineInstanceLabel) match {
            case engineInstanceLabel: EngineInstanceLabel => {
              labels.add(engineInstanceLabel)
              logger.warn(
                s"serviceInstance ${engineInstanceLabel.getServiceInstance} lock resource timeout, clear resource"
              )
              resourceReleased(labels)
            }
            case _ =>
          }
        }

      }
      logger.info(s"Finished to check unlock resource of ${ticketId}")
    }(s"Failed to UnlockTimeoutResourceRunnable $ticketId")

  }

  override def resourceReport(labels: util.List[Label[_]], reportResource: NodeResource): Unit = {
    // TODO

  }

  private def tryLock(labelContainer: RMLabelContainer, timeOut: Long = -1): Unit = {
    labelContainer.getResourceLabels.foreach {
      case label: Label[_] =>
        labelContainer.setCurrentLabel(label)
        val locked = resourceLockService.tryLock(labelContainer, timeOut)
        if (!locked) {
          throw new LinkisRetryException(
            110022,
            s"try to lock resource label ${labelContainer.getCurrentLabel} over $timeOut ms, please wait a moment and try again!"
          )
        }
      case _ =>
    }
  }

}
