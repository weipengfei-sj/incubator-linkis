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

package org.apache.linkis.engineplugin.trino.factory

import org.apache.linkis.engineconn.common.creation.EngineCreationContext
import org.apache.linkis.engineconn.common.engineconn.EngineConn
import org.apache.linkis.engineconn.computation.executor.creation.ComputationSingleExecutorEngineConnFactory
import org.apache.linkis.engineconn.executor.entity.LabelExecutor
import org.apache.linkis.engineplugin.trino.conf.TrinoConfiguration
import org.apache.linkis.engineplugin.trino.executor.TrinoEngineConnExecutor
import org.apache.linkis.manager.label.entity.engine.{EngineType, RunType}
import org.apache.linkis.manager.label.entity.engine.EngineType.EngineType
import org.apache.linkis.manager.label.entity.engine.RunType.RunType

class TrinoEngineConnFactory extends ComputationSingleExecutorEngineConnFactory {

  override def newExecutor(
      id: Int,
      engineCreationContext: EngineCreationContext,
      engineConn: EngineConn
  ): LabelExecutor = {
    new TrinoEngineConnExecutor(TrinoConfiguration.DEFAULT_LIMIT.getValue, id)
  }

  override protected def getEngineConnType: EngineType = EngineType.TRINO

  override protected def getRunType: RunType = RunType.TRINO_SQL

}
