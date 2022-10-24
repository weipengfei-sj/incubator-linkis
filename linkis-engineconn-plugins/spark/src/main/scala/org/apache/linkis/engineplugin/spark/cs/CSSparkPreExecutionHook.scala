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

package org.apache.linkis.engineplugin.spark.cs

import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.cs.client.utils.ContextServiceUtils
import org.apache.linkis.engineconn.computation.executor.execute.EngineExecutionContext
import org.apache.linkis.engineconn.core.exception.{EngineConnErrorCode, ExecutorHookFatalException}
import org.apache.linkis.engineplugin.spark.extension.SparkPreExecutionHook

import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class CSSparkPreExecutionHook extends SparkPreExecutionHook with Logging {

  @PostConstruct
  def init(): Unit = {
    SparkPreExecutionHook.register(this)
  }

  override def hookName: String = "CSSparkPreExecutionHook"

  override def callPreExecutionHook(
      engineExecutionContext: EngineExecutionContext,
      code: String
  ): String = {

    var parsedCode = code
    val contextIDValueStr =
      ContextServiceUtils.getContextIDStrByMap(engineExecutionContext.getProperties)
    val nodeNameStr =
      ContextServiceUtils.getNodeNameStrByMap(engineExecutionContext.getProperties)
    logger.info(
      s"Start to call CSSparkPreExecutionHook,contextID is $contextIDValueStr, nodeNameStr is $nodeNameStr"
    )
    parsedCode = Utils.tryCatch {
      CSTableParser.parse(engineExecutionContext, parsedCode, contextIDValueStr, nodeNameStr)
    } { case t: Throwable =>
      val msg = if (null != t) {
        t.getMessage
      } else {
        "null message"
      }
      logger.info("Failed to parser cs table because : ", msg)
      throw new ExecutorHookFatalException(
        EngineConnErrorCode.ENGINE_CONN_EXECUTOR_INIT_ERROR,
        s"Cannot parse cs table for node : ${nodeNameStr}."
      )
    }
    logger.info(
      s"Finished to call CSSparkPreExecutionHook,contextID is $contextIDValueStr, nodeNameStr is $nodeNameStr"
    )
    parsedCode
  }

}
