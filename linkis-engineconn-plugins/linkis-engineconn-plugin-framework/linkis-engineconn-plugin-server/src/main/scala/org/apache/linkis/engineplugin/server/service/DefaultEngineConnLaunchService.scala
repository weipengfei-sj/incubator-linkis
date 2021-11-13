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
 
package org.apache.linkis.engineplugin.server.service

import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.engineplugin.server.loader.EngineConnPluginsLoader
import org.apache.linkis.manager.engineplugin.common.exception.EngineConnPluginErrorException
import org.apache.linkis.manager.engineplugin.common.launch.EngineConnLaunchBuilder
import org.apache.linkis.manager.engineplugin.common.launch.entity.{EngineConnBuildRequest, EngineConnLaunchRequest}
import org.apache.linkis.manager.engineplugin.common.launch.process.{EngineConnResourceGenerator, JavaProcessEngineConnLaunchBuilder}
import org.apache.linkis.manager.label.entity.engine.EngineTypeLabel
import org.apache.linkis.message.annotation.Receiver
import org.apache.commons.lang.exception.ExceptionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConversions._


@Component
class DefaultEngineConnLaunchService extends EngineConnLaunchService with Logging {

  @Autowired
  private var engineConnResourceGenerator: EngineConnResourceGenerator = _

  private def getEngineLaunchBuilder(engineTypeLabel: EngineTypeLabel): EngineConnLaunchBuilder = {
    val engineConnPluginInstance = EngineConnPluginsLoader.getEngineConnPluginsLoader().getEngineConnPlugin(engineTypeLabel)
    val builder = engineConnPluginInstance.plugin.getEngineConnLaunchBuilder
    builder match {
      case javaProcessEngineConnLaunchBuilder: JavaProcessEngineConnLaunchBuilder =>
        javaProcessEngineConnLaunchBuilder.setEngineConnResourceGenerator(engineConnResourceGenerator)
    }
    builder
  }

  @Receiver
  override def createEngineConnLaunchRequest(engineBuildRequest: EngineConnBuildRequest): EngineConnLaunchRequest = {
    val engineTypeOption = engineBuildRequest.labels.find(_.isInstanceOf[EngineTypeLabel])
    if (engineTypeOption.isDefined) {
      val engineTypeLabel = engineTypeOption.get.asInstanceOf[EngineTypeLabel]
      Utils.tryCatch(getEngineLaunchBuilder(engineTypeLabel).buildEngineConn(engineBuildRequest)){ t =>
        error(s"Failed to createEngineConnLaunchRequest(${engineBuildRequest.ticketId})", t)
        throw new EngineConnPluginErrorException(10001, s"Failed to createEngineConnLaunchRequest, ${ExceptionUtils.getRootCauseMessage(t)}")
      }
    } else {
      throw new EngineConnPluginErrorException(10001, "EngineTypeLabel are requested")
    }
  }
}
