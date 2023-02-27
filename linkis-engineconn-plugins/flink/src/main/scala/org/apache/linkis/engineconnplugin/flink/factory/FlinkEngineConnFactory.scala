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

package org.apache.linkis.engineconnplugin.flink.factory

import org.apache.linkis.common.utils.{ClassUtils, Logging}
import org.apache.linkis.engineconn.common.creation.EngineCreationContext
import org.apache.linkis.engineconnplugin.flink.client.config.Environment
import org.apache.linkis.engineconnplugin.flink.client.config.entries.ExecutionEntry
import org.apache.linkis.engineconnplugin.flink.client.context.ExecutionContext
import org.apache.linkis.engineconnplugin.flink.config.FlinkEnvConfiguration
import org.apache.linkis.engineconnplugin.flink.config.FlinkEnvConfiguration._
import org.apache.linkis.engineconnplugin.flink.config.FlinkResourceConfiguration._
import org.apache.linkis.engineconnplugin.flink.context.{EnvironmentContext, FlinkEngineConnContext}
import org.apache.linkis.engineconnplugin.flink.errorcode.FlinkErrorCodeSummary._
import org.apache.linkis.engineconnplugin.flink.exception.FlinkInitFailedException
import org.apache.linkis.engineconnplugin.flink.setting.Settings
import org.apache.linkis.engineconnplugin.flink.util.ClassUtil
import org.apache.linkis.manager.engineplugin.common.conf.EnvConfiguration
import org.apache.linkis.manager.engineplugin.common.creation.{
  ExecutorFactory,
  MultiExecutorEngineConnFactory
}
import org.apache.linkis.manager.label.entity.Label
import org.apache.linkis.manager.label.entity.engine._
import org.apache.linkis.manager.label.entity.engine.EngineType.EngineType

import org.apache.commons.lang3.StringUtils
import org.apache.flink.configuration._
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}

import java.io.File
import java.net.URL
import java.text.MessageFormat
import java.time.Duration
import java.util
import java.util.{Collections, Locale}

import scala.collection.JavaConverters._

import com.google.common.collect.{Lists, Sets}

class FlinkEngineConnFactory extends MultiExecutorEngineConnFactory with Logging {

  override protected def createEngineConnSession(
      engineCreationContext: EngineCreationContext
  ): Any = {
    var options = engineCreationContext.getOptions
    // Filter the options (startUpParams)
    options = options.asScala
      .mapValues {
        case value if value.contains(FLINK_PARAMS_BLANK_PLACEHOLER.getValue) =>
          logger.info(s"Transform option value: [$value]")
          value.replace(FLINK_PARAMS_BLANK_PLACEHOLER.getValue, " ")
        case v1 => v1
      }
      .toMap
      .asJava
    engineCreationContext.setOptions(options)
    val environmentContext = createEnvironmentContext(engineCreationContext)
    FlinkEngineConnFactory.settings.foreach(
      _.setEnvironmentContext(engineCreationContext, environmentContext)
    )
    val flinkEngineConnContext = createFlinkEngineConnContext(environmentContext)
    val executionContext = createExecutionContext(options, environmentContext)
    flinkEngineConnContext.setExecutionContext(executionContext)
    FlinkEngineConnFactory.settings.foreach(
      _.setExecutionContext(engineCreationContext, flinkEngineConnContext)
    )
    flinkEngineConnContext
  }

  protected def createEnvironmentContext(
      engineCreationContext: EngineCreationContext
  ): EnvironmentContext = {
    val options = engineCreationContext.getOptions
    val defaultEnv =
      Environment.parse(this.getClass.getClassLoader.getResource("flink-sql-defaults.yaml"))
    val hadoopConfDir = EnvConfiguration.HADOOP_CONF_DIR.getValue(options)
    val flinkHome = FLINK_HOME.getValue(options)
    val flinkConfDir = FLINK_CONF_DIR.getValue(options)
    val flinkProvidedLibPath = FLINK_PROVIDED_LIB_PATH.getValue(options)
    val flinkDistJarPath = FLINK_DIST_JAR_PATH.getValue(options)
    // Local lib path
    val providedLibDirsArray = FLINK_LIB_LOCAL_PATH.getValue(options).split(",")
    // Ship directories
    val shipDirsArray = getShipDirectories(options)
    val context = new EnvironmentContext(
      defaultEnv,
      new Configuration,
      hadoopConfDir,
      flinkConfDir,
      flinkHome,
      flinkDistJarPath,
      flinkProvidedLibPath,
      providedLibDirsArray,
      shipDirsArray,
      new util.ArrayList[URL]
    )
    // Step1: environment-level configurations
    val jobName = options.getOrDefault("flink.app.name", "EngineConn-Flink")
    val yarnQueue = LINKIS_QUEUE_NAME.getValue(options)
    val parallelism = FLINK_APP_DEFAULT_PARALLELISM.getValue(options)
    val jobManagerMemory = LINKIS_FLINK_JOB_MANAGER_MEMORY.getValue(options) + "M"
    val taskManagerMemory = LINKIS_FLINK_TASK_MANAGER_MEMORY.getValue(options) + "M"
    val numberOfTaskSlots = LINKIS_FLINK_TASK_SLOTS.getValue(options)
    logger.info(
      s"Use yarn queue $yarnQueue, and set parallelism = $parallelism, jobManagerMemory = $jobManagerMemory, taskManagerMemory = $taskManagerMemory, numberOfTaskSlots = $numberOfTaskSlots."
    )
    // Step2: application-level configurations
    // construct app-config
    val flinkConfig = context.getFlinkConfig
    // construct jar-dependencies
    val flinkUserProvidedLibPath = FLINK_PROVIDED_USER_LIB_PATH.getValue(options).split(",")
    val providedLibDirList =
      Lists.newArrayList(flinkUserProvidedLibPath.filter(StringUtils.isNotBlank): _*)
    val flinkProvidedLibPathList =
      Lists.newArrayList(flinkProvidedLibPath.split(",").filter(StringUtils.isNotBlank): _*)
    // Add the global lib path to user lib path list
    if (flinkProvidedLibPathList != null && flinkProvidedLibPathList.size() > 0) {
      providedLibDirList.addAll(flinkProvidedLibPathList)
    }
    // if(StringUtils.isNotBlank(flinkLibRemotePath)) providedLibDirList.add(flinkLibRemotePath)
    flinkConfig.set(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibDirList)
    // construct jar-dependencies
    flinkConfig.set(YarnConfigOptions.SHIP_FILES, context.getShipDirs)
    // set user classpaths
    val classpaths = FLINK_APPLICATION_CLASSPATH.getValue(options)
    if (StringUtils.isNotBlank(classpaths)) {
      logger.info(s"Add $classpaths to flink application classpath.")
      flinkConfig.set(PipelineOptions.CLASSPATHS, util.Arrays.asList(classpaths.split(","): _*))
    }
    // yarn application name
    flinkConfig.set(YarnConfigOptions.APPLICATION_NAME, jobName)
    // yarn queue
    flinkConfig.set(YarnConfigOptions.APPLICATION_QUEUE, yarnQueue)
    // Configure resource/concurrency
    flinkConfig.setInteger(CoreOptions.DEFAULT_PARALLELISM, parallelism)
    flinkConfig.set(JobManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse(jobManagerMemory))
    flinkConfig.set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse(taskManagerMemory))
    flinkConfig.setInteger(TaskManagerOptions.NUM_TASK_SLOTS, numberOfTaskSlots)
    // set extra configs
    options.asScala.filter { case (key, _) => key.startsWith(FLINK_CONFIG_PREFIX) }.foreach {
      case (key, value) => flinkConfig.setString(key.substring(FLINK_CONFIG_PREFIX.length), value)
    }
    // set kerberos config
    if (FLINK_KERBEROS_ENABLE.getValue(options)) {
      flinkConfig.set(
        SecurityOptions.KERBEROS_LOGIN_CONTEXTS,
        FLINK_KERBEROS_LOGIN_CONTEXTS.getValue(options)
      )
      flinkConfig.set(
        SecurityOptions.KERBEROS_KRB5_PATH,
        FLINK_KERBEROS_CONF_PATH.getValue(options)
      )
      flinkConfig.set(
        SecurityOptions.KERBEROS_LOGIN_PRINCIPAL,
        FLINK_KERBEROS_LOGIN_PRINCIPAL.getValue(options)
      )
      flinkConfig.set(
        SecurityOptions.KERBEROS_LOGIN_KEYTAB,
        FLINK_KERBEROS_LOGIN_KEYTAB.getValue(options)
      )
    }
    if (FLINK_REPORTER_ENABLE.getValue(options)) {
      flinkConfig.set(MetricOptions.REPORTER_CLASS, FLINK_REPORTER_CLASS.getValue(options))
      flinkConfig.set(
        MetricOptions.REPORTER_INTERVAL,
        Duration.ofMillis(FLINK_REPORTER_INTERVAL.getValue(options).toLong)
      )
    }
    // set savePoint
    val savePointPath = FLINK_SAVE_POINT_PATH.getValue(options)
    if (StringUtils.isNotBlank(savePointPath)) {
      val allowNonRestoredState = FLINK_APP_ALLOW_NON_RESTORED_STATUS.getValue(options).toBoolean
      val savepointRestoreSettings =
        SavepointRestoreSettings.forPath(savePointPath, allowNonRestoredState)
      SavepointRestoreSettings.toConfiguration(savepointRestoreSettings, flinkConfig)
    }
    // Configure user-entrance jar. Can be HDFS file, but only support 1 jar
    val flinkMainClassJar = FLINK_APPLICATION_MAIN_CLASS_JAR.getValue(options)
    if (StringUtils.isNotBlank(flinkMainClassJar)) {
      val flinkMainClassJarPath =
        if (new File(flinkMainClassJar).exists()) flinkMainClassJar
        else getClass.getClassLoader.getResource(flinkMainClassJar).getPath
      logger.info(
        s"Ready to use $flinkMainClassJarPath as main class jar to submit application to Yarn."
      )
      flinkConfig.set(PipelineOptions.JARS, Collections.singletonList(flinkMainClassJarPath))
      flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
      flinkConfig.setBoolean(DeploymentOptions.ATTACHED, FLINK_EXECUTION_ATTACHED.getValue(options))
      context.setDeploymentTarget(YarnDeploymentTarget.APPLICATION)
      addApplicationLabels(engineCreationContext)
    } else if (isOnceEngineConn(engineCreationContext.getLabels())) {
      flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName)
    } else {
      flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
    }
    context
  }

  protected def isOnceEngineConn(labels: util.List[Label[_]]): Boolean = {
    val engineConnModeLabel = getEngineConnModeLabel(labels)
    engineConnModeLabel != null && (EngineConnMode.toEngineConnMode(
      engineConnModeLabel.getEngineConnMode
    ) match {
      case EngineConnMode.Once | EngineConnMode.Once_With_Cluster => true
      case _ => false
    })
  }

  private def getShipDirectories(options: util.Map[String, String]): Array[String] = {
    // Local ship directories
    var shipDirsArray = (FLINK_SHIP_DIRECTORIES.getValue(options) + "," +
      FLINK_SHIP_DIRECTORIES.getValue).split(",")
    shipDirsArray = shipDirsArray match {
      case pathArray: Array[String] =>
        pathArray
          .filter(StringUtils.isNotBlank)
          .map(dir => {
            if (new File(dir).exists()) {
              dir
            } else {
              Option(getClass.getClassLoader.getResource(dir)) match {
                case Some(url) => url.getPath
                case _ =>
                  logger.warn(s"Local file/directory [$dir] not found")
                  null
              }
            }
          })
          .dropWhile(StringUtils.isBlank)
      case _ => new Array[String](0)
    }
    val shipDirs: util.List[String] =
      new util.ArrayList[String](Sets.newHashSet(shipDirsArray: _*))
    // Remote ship directories
    FLINK_SHIP_REMOTE_DIRECTORIES.getValue(options).split(",") match {
      case pathArray: Array[String] =>
        pathArray
          .filter(StringUtils.isNotBlank)
          .foreach(remotePath =>
            Option(remotePath) match {
              case Some(path) =>
                // TODO Check if the path is already exists
                Option(new org.apache.flink.core.fs.Path(path).toUri.getScheme).foreach(schema =>
                  if ("viewfs".equals(schema) || "hdfs".equals(schema)) {
                    shipDirs.add(path)
                  } else {
                    logger
                      .warn(s"Unrecognized schema [$schema] for remote file/directory [$path]")
                  }
                )
              case _ =>
            }
          )
    }
    shipDirs.toArray(new Array[String](0))
  }

  private def addApplicationLabels(engineCreationContext: EngineCreationContext): Unit = {
    val labels = engineCreationContext.getLabels().asScala
    if (!labels.exists(_.isInstanceOf[CodeLanguageLabel])) {
      val codeLanguageLabel = new CodeLanguageLabel
      codeLanguageLabel.setCodeType(RunType.JAR.toString)
      engineCreationContext.getLabels().add(codeLanguageLabel)
    }
    if (!labels.exists(_.isInstanceOf[EngineConnModeLabel])) {
      val engineConnModeLabel = new EngineConnModeLabel
      engineConnModeLabel.setEngineConnMode(EngineConnMode.Once.toString)
      engineCreationContext.getLabels().add(engineConnModeLabel)
    }
  }

  def createExecutionContext(
      options: util.Map[String, String],
      environmentContext: EnvironmentContext
  ): ExecutionContext = {
    val environment = environmentContext.getDeploymentTarget match {
      case YarnDeploymentTarget.PER_JOB | YarnDeploymentTarget.SESSION =>
        val planner = FlinkEnvConfiguration.FLINK_SQL_PLANNER.getValue(options)
        if (!ExecutionEntry.AVAILABLE_PLANNERS.contains(planner.toLowerCase(Locale.getDefault))) {
          throw new FlinkInitFailedException(
            MessageFormat.format(
              PLANNER_MUST_THESE.getErrorDesc,
              String.join(", ", ExecutionEntry.AVAILABLE_PLANNERS)
            )
          )
        }
        val executionType = FlinkEnvConfiguration.FLINK_SQL_EXECUTION_TYPE.getValue(options)
        if (
            !ExecutionEntry.AVAILABLE_EXECUTION_TYPES.contains(
              executionType.toLowerCase(Locale.getDefault)
            )
        ) {
          throw new FlinkInitFailedException(
            MessageFormat.format(
              EXECUTION_MUST_THESE.getErrorDesc,
              String.join(", ", ExecutionEntry.AVAILABLE_EXECUTION_TYPES)
            )
          )
        }
        val properties = new util.HashMap[String, String]
        properties.put(
          Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_PLANNER,
          planner
        )
        properties.put(
          Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_TYPE,
          executionType
        )
        if (executionType.equalsIgnoreCase(ExecutionEntry.EXECUTION_TYPE_VALUE_BATCH)) {
          // for batch mode we ensure that results are provided in materialized form
          properties.put(
            Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_RESULT_MODE,
            ExecutionEntry.EXECUTION_RESULT_MODE_VALUE_TABLE
          )
        } else {
          // for streaming mode we ensure that results are provided in changelog form
          properties.put(
            Environment.EXECUTION_ENTRY + "." + ExecutionEntry.EXECUTION_RESULT_MODE,
            ExecutionEntry.EXECUTION_RESULT_MODE_VALUE_CHANGELOG
          )
        }
        Environment.enrich(environmentContext.getDefaultEnv, properties, Collections.emptyMap())
      case YarnDeploymentTarget.APPLICATION => null
      case t =>
        logger.error(s"Not supported YarnDeploymentTarget ${t.getName}.")
        throw new FlinkInitFailedException(
          NOT_SUPPORTED_YARNTARGET.getErrorDesc + s" ${t.getName}."
        )
    }
    val executionContext = ExecutionContext
      .builder(
        environmentContext.getDefaultEnv,
        environment,
        environmentContext.getDependencies,
        environmentContext.getFlinkConfig
      )
      .build()
    if (FLINK_CHECK_POINT_ENABLE.getValue(options)) {
      val checkpointInterval = FLINK_CHECK_POINT_INTERVAL.getValue(options)
      val checkpointMode = FLINK_CHECK_POINT_MODE.getValue(options)
      val checkpointTimeout = FLINK_CHECK_POINT_TIMEOUT.getValue(options)
      val checkpointMinPause = FLINK_CHECK_POINT_MIN_PAUSE.getValue(options)
      logger.info(
        s"checkpoint is enabled, checkpointInterval is $checkpointInterval, checkpointMode is $checkpointMode, checkpointTimeout is $checkpointTimeout."
      )
      executionContext.getTableEnvironment // This line is need to initialize the StreamExecutionEnvironment.
      executionContext.getStreamExecutionEnvironment.enableCheckpointing(checkpointInterval)
      val checkpointConfig = executionContext.getStreamExecutionEnvironment.getCheckpointConfig
      checkpointMode match {
        case "EXACTLY_ONCE" =>
          checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
        case "AT_LEAST_ONCE" =>
          checkpointConfig.setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE)
        case _ =>
          throw new FlinkInitFailedException(
            MessageFormat.format(UNKNOWN_CHECKPOINT_MODE.getErrorDesc, checkpointMode)
          )
      }
      checkpointConfig.setCheckpointTimeout(checkpointTimeout)
      checkpointConfig.setMinPauseBetweenCheckpoints(checkpointMinPause)
      checkpointConfig.enableExternalizedCheckpoints(
        ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
      )
      checkpointConfig.configure(environmentContext.getFlinkConfig)
    }
    executionContext
  }

  protected def createFlinkEngineConnContext(
      environmentContext: EnvironmentContext
  ): FlinkEngineConnContext =
    new FlinkEngineConnContext(environmentContext)

  override protected def getDefaultExecutorFactoryClass: Class[_ <: ExecutorFactory] =
    classOf[FlinkCodeExecutorFactory]

  override protected def getEngineConnType: EngineType = EngineType.FLINK

  private val executorFactoryArray = Array[ExecutorFactory](
    ClassUtil.getInstance(classOf[FlinkSQLExecutorFactory], new FlinkSQLExecutorFactory),
    ClassUtil
      .getInstance(classOf[FlinkApplicationExecutorFactory], new FlinkApplicationExecutorFactory),
    ClassUtil.getInstance(classOf[FlinkCodeExecutorFactory], new FlinkCodeExecutorFactory)
  )

  override def getExecutorFactories: Array[ExecutorFactory] = executorFactoryArray
}

object FlinkEngineConnFactory extends Logging {

  private val settings = ClassUtils.reflections
    .getSubTypesOf(classOf[Settings])
    .asScala
    .filterNot(ClassUtils.isInterfaceOrAbstract)
    .map(_.newInstance())
    .toList

  logger.info(s"Settings list: ${settings.map(_.getClass.getSimpleName)}.")
}
