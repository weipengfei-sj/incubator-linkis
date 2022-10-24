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

package org.apache.linkis.ecm.server.service.impl

import org.apache.linkis.common.ServiceInstance
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.ecm.core.engineconn.{EngineConn, EngineConnInfo}
import org.apache.linkis.ecm.core.launch._
import org.apache.linkis.ecm.server.LinkisECMApplication
import org.apache.linkis.ecm.server.conf.ECMConfiguration._
import org.apache.linkis.ecm.server.engineConn.DefaultEngineConn
import org.apache.linkis.ecm.server.hook.ECMHook
import org.apache.linkis.ecm.server.listener.{EngineConnAddEvent, EngineConnStatusChangeEvent}
import org.apache.linkis.ecm.server.service.{EngineConnLaunchService, ResourceLocalizationService}
import org.apache.linkis.ecm.server.util.ECMUtils
import org.apache.linkis.governance.common.conf.GovernanceCommonConf
import org.apache.linkis.governance.common.utils.JobUtils
import org.apache.linkis.manager.common.entity.enumeration.NodeStatus
import org.apache.linkis.manager.common.entity.enumeration.NodeStatus.Failed
import org.apache.linkis.manager.common.entity.node.{AMEngineNode, EngineNode}
import org.apache.linkis.manager.common.protocol.engine.EngineConnStatusCallbackToAM
import org.apache.linkis.manager.engineplugin.common.launch.entity.EngineConnLaunchRequest
import org.apache.linkis.rpc.Sender

import org.apache.commons.lang3.exception.ExceptionUtils

import scala.concurrent.{ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

abstract class AbstractEngineConnLaunchService extends EngineConnLaunchService with Logging {

  protected implicit val executor: ExecutionContextExecutorService =
    Utils.newCachedExecutionContext(ECM_LAUNCH_MAX_THREAD_SIZE, "EngineConn-Manager-Thread-")

  protected var resourceLocalizationService: ResourceLocalizationService = _

  def setResourceLocalizationService(service: ResourceLocalizationService): Unit =
    this.resourceLocalizationService = service

  def beforeLaunch(request: EngineConnLaunchRequest, conn: EngineConn, duration: Long): Unit = {
    Utils.tryAndWarn(getECMHooks(request).foreach(_.beforeLaunch(request, conn)))
  }

  def afterLaunch(request: EngineConnLaunchRequest, conn: EngineConn, duration: Long): Unit = {
    getECMHooks(request).foreach(_.afterLaunch(conn))
  }

  override def launchEngineConn(request: EngineConnLaunchRequest, duration: Long): EngineNode = {
    // 1.创建engineConn和runner,launch 并设置基础属性
    val taskId = JobUtils.getJobIdFromStringMap(request.creationDesc.properties)
    logger.info(s"TaskId: $taskId try to launch a new EngineConn with $request.")
    val conn = createEngineConn
    val runner = createEngineConnLaunchRunner
    val launch = createEngineConnLaunch
    launch.setEngineConnLaunchRequest(request)
    runner.setEngineConnLaunch(launch)
    conn.setEngineConnLaunchRunner(runner)
    conn.setLabels(request.labels)
    conn.setCreationDesc(request.creationDesc)
    conn.setResource(request.nodeResource)
    conn.setTickedId(request.ticketId)
    conn.setStatus(NodeStatus.Starting)
    conn.setEngineConnInfo(new EngineConnInfo)
    conn.setEngineConnManagerEnv(launch.getEngineConnManagerEnv())
    // 2.资源本地化，并且设置ecm的env环境信息
    getResourceLocalizationServie.handleInitEngineConnResources(request, conn)
    // 3.添加到list
    LinkisECMApplication.getContext.getECMSyncListenerBus.postToAll(EngineConnAddEvent(conn))
    // 4.run
    Utils.tryCatch {
      beforeLaunch(request, conn, duration)
      runner.run()
      launch match {
        case pro: ProcessEngineConnLaunch =>
          val serviceInstance = ServiceInstance(
            GovernanceCommonConf.ENGINE_CONN_SPRING_NAME.getValue,
            ECMUtils.getInstanceByPort(pro.getEngineConnPort)
          )
          conn.setServiceInstance(serviceInstance)
        case _ =>
      }
      afterLaunch(request, conn, duration)

      val future = Future {
        logger.info(s"wait engineConn  ${conn.getServiceInstance} start")
        waitEngineConnStart(request, conn, duration)
      }

      future onComplete {
        case Failure(t) =>
          logger.error(
            s"TaskId: $taskId init ${conn.getServiceInstance} failed.${conn.getEngineConnLaunchRunner.getEngineConnLaunch.getEngineConnManagerEnv().engineConnWorkDir}"
          )
          LinkisECMApplication.getContext.getECMSyncListenerBus.postToAll(
            EngineConnStatusChangeEvent(conn.getTickedId, Failed)
          )
        case Success(_) =>
          logger.info(
            s"TaskId: $taskId init ${conn.getServiceInstance} succeed.${conn.getEngineConnLaunchRunner.getEngineConnLaunch.getEngineConnManagerEnv().engineConnWorkDir}"
          )
      }
    } { t =>
      logger.error(
        s"TaskId: $taskId init ${conn.getServiceInstance} failed, ${conn.getEngineConnLaunchRunner.getEngineConnLaunch
          .getEngineConnManagerEnv()
          .engineConnWorkDir}, now stop and delete it. message: ${t.getMessage}",
        t
      )
      conn.getEngineConnLaunchRunner.stop()
      Sender
        .getSender(MANAGER_SPRING_NAME)
        .send(
          EngineConnStatusCallbackToAM(
            conn.getServiceInstance,
            NodeStatus.ShuttingDown,
            " wait init failed , reason " + ExceptionUtils.getRootCauseMessage(t)
          )
        )
      LinkisECMApplication.getContext.getECMSyncListenerBus.postToAll(
        EngineConnStatusChangeEvent(conn.getTickedId, Failed)
      )
      throw t
    }
    val engineNode = new AMEngineNode()
    engineNode.setLabels(conn.getLabels)

    engineNode.setServiceInstance(conn.getServiceInstance)
    engineNode.setOwner(request.user)
    engineNode.setMark("process")
    engineNode
  }

  def waitEngineConnStart(request: EngineConnLaunchRequest, conn: EngineConn, duration: Long): Unit

  def createEngineConn: EngineConn = new DefaultEngineConn

  def createEngineConnLaunchRunner: EngineConnLaunchRunner = new EngineConnLaunchRunnerImpl

  def createEngineConnLaunch: EngineConnLaunch

  def getResourceLocalizationServie: ResourceLocalizationService = {
    // TODO: null 抛出异常
    this.resourceLocalizationService
  }

  def getECMHooks(request: EngineConnLaunchRequest): Array[ECMHook] = {
    ECMHook.getECMHooks.filter(h =>
      if (null != request.engineConnManagerHooks) {
        request.engineConnManagerHooks.contains(h.getName)
      } else {
        false
      }
    )
  }

}
