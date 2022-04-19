/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.linkis.engineconn.acessible.executor.execution

import java.util.concurrent.TimeUnit
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.engineconn.acessible.executor.conf.AccessibleExecutorConfiguration
import org.apache.linkis.engineconn.acessible.executor.entity.AccessibleExecutor
import org.apache.linkis.engineconn.common.creation.EngineCreationContext
import org.apache.linkis.engineconn.common.engineconn.EngineConn
import org.apache.linkis.engineconn.common.execution.EngineConnExecution
import org.apache.linkis.engineconn.core.EngineConnObject
import org.apache.linkis.engineconn.core.executor.ExecutorManager
import org.apache.linkis.engineconn.core.hook.ShutdownHook
import org.apache.linkis.engineconn.executor.entity.{Executor, LabelExecutor, ResourceExecutor}
import org.apache.linkis.engineconn.executor.listener.ExecutorListenerBusContext
import org.apache.linkis.engineconn.executor.service.ManagerService
import org.apache.linkis.manager.common.entity.enumeration.NodeStatus
import org.apache.linkis.manager.common.protocol.engine.{ECCanKillRequest, EngineConnReleaseRequest}
import org.apache.linkis.manager.common.protocol.resource.ResourceUsedProtocol
import org.apache.linkis.rpc.Sender
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.linkis.manager.label.utils.LabelUtil


class AccessibleEngineConnExecution extends EngineConnExecution with Logging {

  protected def findReportExecutor(engineCreationContext: EngineCreationContext,
                                   engineConn: EngineConn): Executor =
    ExecutorManager.getInstance.getReportExecutor

  protected def beforeReportToLinkisManager(executor: Executor,
                                            engineCreationContext: EngineCreationContext,
                                            engineConn: EngineConn): Unit = {}

  protected def afterReportToLinkisManager(executor: Executor,
                                           engineCreationContext: EngineCreationContext,
                                           engineConn: EngineConn): Unit = {}

  override def execute(engineCreationContext: EngineCreationContext, engineConn: EngineConn): Unit = {
    init(engineCreationContext)
    val executor = findReportExecutor(engineCreationContext, engineConn)
    info(s"Created a report executor ${executor.getClass.getSimpleName}(${executor.getId}).")
    beforeReportToLinkisManager(executor, engineCreationContext, engineConn)
    reportUsedResource(executor, engineCreationContext)
    reportLabel(executor)
    executorStatusChecker
    afterReportToLinkisManager(executor, engineCreationContext, engineConn)
  }

  protected def init(engineCreationContext: EngineCreationContext): Unit = {
    val listenerBusContext = ExecutorListenerBusContext.getExecutorListenerBusContext()
    listenerBusContext.getEngineConnAsyncListenerBus.start()
  }


  private def executorStatusChecker(): Unit = {
    val context = EngineConnObject.getEngineCreationContext
    val maxFreeTimeVar = AccessibleExecutorConfiguration.ENGINECONN_MAX_FREE_TIME.getValue(context.getOptions)
    val maxFreeTimeStr = maxFreeTimeVar.toString
    val maxFreeTime = maxFreeTimeVar.toLong
    logger.info("executorStatusChecker created， maxFreeTimeMills is " + maxFreeTime)
    Utils.defaultScheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = Utils.tryAndWarn {
        val accessibleExecutor = ExecutorManager.getInstance.getReportExecutor match {
          case executor: AccessibleExecutor => executor
          case executor: Executor =>
            logger.warn(s"Executor(${executor.getId}) is not a AccessibleExecutor, do noting when reached max free time .")
            return
        }
        if (NodeStatus.isCompleted(accessibleExecutor.getStatus)) {
          error(s"${accessibleExecutor.getId} has completed with status ${accessibleExecutor.getStatus}, now stop it.")
          ShutdownHook.getShutdownHook.notifyStop()
        } else if (accessibleExecutor.getStatus == NodeStatus.ShuttingDown) {
          logger.warn(s"${accessibleExecutor.getId} is ShuttingDown...")
          ShutdownHook.getShutdownHook.notifyStop()
        } else if (maxFreeTime > 0 && (NodeStatus.Unlock.equals(accessibleExecutor.getStatus) || NodeStatus.Idle.equals(accessibleExecutor.getStatus))
          && System.currentTimeMillis - accessibleExecutor.getLastActivityTime > maxFreeTime) {
          if (ifECCanMaintain()) {
            logger.info("ec will not be killed at this time")
            accessibleExecutor.updateLastActivityTime()
          } else {
            logger.warn(s"${accessibleExecutor.getId} has not been used for $maxFreeTimeStr, now try to shutdown it.")
            requestManagerReleaseExecutor(" idle release")
            ShutdownHook.getShutdownHook.notifyStop()
          }

        }
      }
    }, 3 * 60 * 1000, AccessibleExecutorConfiguration.ENGINECONN_HEARTBEAT_TIME.getValue.toLong, TimeUnit.MILLISECONDS)
  }

  def requestManagerReleaseExecutor(msg: String): Unit = {
    val engineReleaseRequest = new EngineConnReleaseRequest(Sender.getThisServiceInstance, Utils.getJvmUser, msg, EngineConnObject.getEngineCreationContext.getTicketId)
    ManagerService.getManagerService.requestReleaseEngineConn(engineReleaseRequest)
  }

  private def isMaintainSupported(): Boolean = {
    if (! AccessibleExecutorConfiguration.ENABLE_MAINTAIN.getValue) return false
    val userCreator = LabelUtil.getUserCreatorLabel(EngineConnObject.getEngineCreationContext.getLabels())
    if (null != userCreator && AccessibleExecutorConfiguration.ENABLE_MAINTAIN_CREATORS.getValue.contains(userCreator.getCreator)) {
      logger.info(s"${userCreator.getStringValue} maintain enabled")
      true
    } else {
      false
    }
  }

  private def ifECCanMaintain(): Boolean = {
    if (! isMaintainSupported()) return false
    val engineTypeLabel = LabelUtil.getEngineTypeLabel(EngineConnObject.getEngineCreationContext.getLabels())
    val userCreator = LabelUtil.getUserCreatorLabel(EngineConnObject.getEngineCreationContext.getLabels())
    if (null == engineTypeLabel) return false
    val ecCanKillRequest = new ECCanKillRequest
    ecCanKillRequest.setEngineConnInstance(Sender.getThisServiceInstance)
    ecCanKillRequest.setUser(userCreator.getUser)
    ecCanKillRequest.setUserCreatorLabel(userCreator)
    ecCanKillRequest.setEngineTypeLabel(engineTypeLabel)
    Utils.tryCatch{
      val response = ManagerService.getManagerService.ecCanKillRequest(ecCanKillRequest)
      logger.info(s"From manager get $response")
      ! response.getFlag
    } { throwable: Throwable =>
      logger.warn("Failed to ecCanKillRequestManager, will be default exit", throwable)
      false
    }
  }

  protected def reportUsedResource(executor: Executor, engineCreationContext: EngineCreationContext): Unit = executor match {
    case resourceExecutor: ResourceExecutor =>
      ManagerService.getManagerService
        .reportUsedResource(ResourceUsedProtocol(Sender.getThisServiceInstance,
          resourceExecutor.getCurrentNodeResource(), engineCreationContext.getTicketId))
      info("In the first time, report usedResources to LinkisManager succeed.")
    case _ =>
      info("Do not need to report usedResources.")
  }

  protected def reportLabel(executor: Executor): Unit = executor match {
    case labelExecutor: LabelExecutor =>
      ManagerService.getManagerService.labelReport(labelExecutor.getExecutorLabels())
      info("In the first time, report all labels to LinkisManager succeed.")
    case _ =>
      info("Do not need to report labels.")
  }

  /**
    * Accessible should be executed by the first, because it will instance the report executor.
    *
    * @return
    */
  override def getOrder: Int = 10
}
