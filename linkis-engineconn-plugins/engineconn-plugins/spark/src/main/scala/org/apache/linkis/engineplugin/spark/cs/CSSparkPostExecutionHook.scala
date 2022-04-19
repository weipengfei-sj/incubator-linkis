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

package org.apache.linkis.engineplugin.spark.cs

import org.apache.linkis.common.utils.Logging
import org.apache.linkis.cs.client.utils.ContextServiceUtils
import org.apache.linkis.engineconn.computation.executor.execute.EngineExecutionContext
import org.apache.linkis.engineplugin.spark.extension.SparkPostExecutionHook
import org.apache.linkis.scheduler.executer.ExecuteResponse
import javax.annotation.PostConstruct
import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component

@Component
class CSSparkPostExecutionHook extends SparkPostExecutionHook with Logging{

  @PostConstruct
  def  init(): Unit = {
    SparkPostExecutionHook.register(this)
  }

  override def hookName: String = {
    "CSSparkPostExecutionHook"
  }

  override def callPostExecutionHook(engineExecutorContext: EngineExecutionContext, executeResponse: ExecuteResponse, code: String): Unit = {
    val contextIDValueStr = ContextServiceUtils.getContextIDStrByMap(engineExecutorContext.getProperties)
    val nodeNameStr = ContextServiceUtils.getNodeNameStrByMap(engineExecutorContext.getProperties)

    if (StringUtils.isNotEmpty(contextIDValueStr) && StringUtils.isNotEmpty(nodeNameStr)) {
      info(s"Start to call CSSparkPostExecutionHook,contextID is $contextIDValueStr, nodeNameStr is $nodeNameStr")
      CSTableParser.clearCSTmpView(code, contextIDValueStr, nodeNameStr)
      info(s"Finished to call CSSparkPostExecutionHook,contextID is $contextIDValueStr, nodeNameStr is $nodeNameStr")
    }

  }
}
