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
 
package org.apache.linkis.engineplugin.hive.conf

import org.apache.linkis.common.conf.CommonVars

object HiveEngineConfiguration {

  val HIVE_LIB_HOME = CommonVars[String]("hive.lib", CommonVars[String]("HIVE_LIB", "/appcom/Install/hive/lib").getValue)
  val ENABLE_FETCH_BASE64 = CommonVars[Boolean]("wds.linkis.hive.enable.fetch.base64",false).getValue
  val BASE64_SERDE_CLASS =  CommonVars[String]("wds.linkis.hive.base64.serde.class","org.apache.linkis.engineplugin.hive.serde.CustomerDelimitedJSONSerDe").getValue
}
