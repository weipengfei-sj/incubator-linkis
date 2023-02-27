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

package org.apache.linkis.manager.engineplugin.common.conf

import org.apache.linkis.common.conf.{ByteType, CommonVars, Configuration}

import org.apache.commons.lang3.{JavaVersion, SystemUtils}

object EnvConfiguration {

  val HIVE_CONF_DIR = CommonVars[String](
    "hive.config.dir",
    CommonVars[String]("HIVE_CONF_DIR", "/appcom/config/hive-config").getValue
  )

  val HADOOP_LIB_NATIVE =
    CommonVars[String]("linkis.hadoop.lib.native", "/appcom/Install/hadoop/lib/native")

  val HADOOP_CONF_DIR = CommonVars[String](
    "hadoop.config.dir",
    CommonVars[String]("HADOOP_CONF_DIR", "/appcom/config/hadoop-config").getValue
  )

  val ENGINE_CONN_JARS = CommonVars("wds.linkis.engineConn.jars", "", "engineConn额外的Jars")

  val ENGINE_CONN_CLASSPATH_FILES =
    CommonVars("wds.linkis.engineConn.files", "", "engineConn额外的配置文件")

  val metaspaceSize = if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8)) {
    "-XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=128m"
  } else {
    "-XX:MaxPermSize=256m -XX:PermSize=128m"
  }

  val ENGINE_CONN_DEFAULT_JAVA_OPTS = CommonVars[String](
    "wds.linkis.engineConn.javaOpts.default",
    s"-XX:+UseG1GC ${metaspaceSize} " +
      s"-Xloggc:%s -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Dwds.linkis.server.conf=linkis-engineconn.properties -Dwds.linkis.gateway.url=${Configuration.getGateWayURL()}"
  )

  val ENGINE_CONN_MEMORY = CommonVars(
    "wds.linkis.engineConn.memory",
    new ByteType("1g"),
    "Specify the memory size of the java client(指定java进程的内存大小)"
  )

  val ENGINE_CONN_JAVA_EXTRA_OPTS = CommonVars(
    "wds.linkis.engineConn.java.extraOpts",
    "",
    "Specify the option parameter of the java process (please modify it carefully!!!)"
  )

  val ENGINE_CONN_JAVA_EXTRA_CLASSPATH = CommonVars(
    "wds.linkis.engineConn.extra.classpath",
    "",
    "Specify the full path of the java classpath"
  )

  val ENGINE_CONN_MAX_RETRIES = CommonVars("wds.linkis.engineconn.retries.max", 3)

  val ENGINE_CONN_DEBUG_ENABLE = CommonVars("wds.linkis.engineconn.debug.enable", false)

  val LOG4J2_XML_FILE = CommonVars[String]("wds.linkis.engineconn.log4j2.xml.file", "log4j2.xml")

  val LINKIS_PUBLIC_MODULE_PATH = CommonVars(
    "wds.linkis.public_module.path",
    Configuration.getLinkisHome + "/lib/linkis-commons/public-module"
  )

  val LINKIS_CONF_DIR = CommonVars("LINKIS_CONF_DIR", Configuration.getLinkisHome() + "/conf")
}
