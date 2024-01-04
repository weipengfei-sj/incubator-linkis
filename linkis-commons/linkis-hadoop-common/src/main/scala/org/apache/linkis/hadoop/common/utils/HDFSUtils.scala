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

package org.apache.linkis.hadoop.common.utils

import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.hadoop.common.conf.HadoopConf
import org.apache.linkis.hadoop.common.conf.HadoopConf._
import org.apache.linkis.hadoop.common.entity.HDFSFileSystemContainer

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.security.UserGroupInformation

import java.io.File
import java.nio.file.Paths
import java.security.PrivilegedExceptionAction
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._

object HDFSUtils extends Logging {

  private val fileSystemCache: java.util.Map[String, HDFSFileSystemContainer] =
    new java.util.HashMap[String, HDFSFileSystemContainer]()

  private val LOCKER_SUFFIX = "_HDFS"
  private val DEFAULT_CACHE_LABEL = "default"
  private val JOINT = "_"

  if (HadoopConf.HDFS_ENABLE_CACHE) {
    logger.info("HDFS Cache enabled ")
    Utils.defaultScheduler.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = Utils.tryAndWarn {
          fileSystemCache
            .values()
            .asScala
            .filter { hdfsFileSystemContainer =>
              hdfsFileSystemContainer.canRemove() && StringUtils.isNotBlank(
                hdfsFileSystemContainer.getUser
              )
            }
            .foreach { hdfsFileSystemContainer =>
              val locker =
                hdfsFileSystemContainer.getUser + JOINT + hdfsFileSystemContainer.getLabel + LOCKER_SUFFIX
              locker.intern() synchronized {
                if (hdfsFileSystemContainer.canRemove()) {
                  fileSystemCache.remove(
                    hdfsFileSystemContainer.getUser + JOINT + hdfsFileSystemContainer.getLabel
                  )
                  IOUtils.closeQuietly(hdfsFileSystemContainer.getFileSystem)
                  logger.info(
                    s"user${hdfsFileSystemContainer.getUser} to remove hdfsFileSystemContainer,because hdfsFileSystemContainer can remove"
                  )
                }
              }
            }
        }
      },
      3 * 60 * 1000,
      60 * 1000,
      TimeUnit.MILLISECONDS
    )
  }

  def getConfiguration(user: String): Configuration = getConfiguration(user, hadoopConfDir)

  def getConfigurationByLabel(user: String, label: String): Configuration = {
    getConfiguration(user, getHadoopConDirByLabel(label))
  }

  private def getHadoopConDirByLabel(label: String): String = {
    if (StringUtils.isBlank(label)) {
      hadoopConfDir
    } else {
      val prefix = if (HadoopConf.HADOOP_EXTERNAL_CONF_DIR_PREFIX.getValue.endsWith("/")) {
        HadoopConf.HADOOP_EXTERNAL_CONF_DIR_PREFIX.getValue
      } else {
        HadoopConf.HADOOP_EXTERNAL_CONF_DIR_PREFIX.getValue + "/"
      }
      prefix + label
    }
  }

  def getConfiguration(user: String, hadoopConfDir: String): Configuration = {
    val confPath = new File(hadoopConfDir)
    if (!confPath.exists() || confPath.isFile) {
      throw new RuntimeException(
        s"Create hadoop configuration failed, path $hadoopConfDir not exists."
      )
    }
    val conf = new Configuration()
    conf.addResource(
      new Path(Paths.get(hadoopConfDir, "core-site.xml").toAbsolutePath.toFile.getAbsolutePath)
    )
    conf.addResource(
      new Path(Paths.get(hadoopConfDir, "hdfs-site.xml").toAbsolutePath.toFile.getAbsolutePath)
    )
    conf
  }

  def getHDFSRootUserFileSystem: FileSystem = getHDFSRootUserFileSystem(
    getConfiguration(HADOOP_ROOT_USER.getValue)
  )

  def getHDFSRootUserFileSystem(conf: org.apache.hadoop.conf.Configuration): FileSystem =
    getHDFSUserFileSystem(HADOOP_ROOT_USER.getValue, conf)

  def getHDFSUserFileSystem(userName: String): FileSystem =
    getHDFSUserFileSystem(userName, getConfiguration(userName))

  def getHDFSUserFileSystem(
      userName: String,
      conf: org.apache.hadoop.conf.Configuration
  ): FileSystem = getHDFSUserFileSystem(userName, null, conf)

  def getHDFSUserFileSystem(
      userName: String,
      label: String,
      conf: org.apache.hadoop.conf.Configuration
  ): FileSystem = if (HadoopConf.HDFS_ENABLE_CACHE) {
    val cacheLabel = if (label == null) DEFAULT_CACHE_LABEL else label
    val cacheKey = userName + JOINT + cacheLabel
    val locker = cacheKey + LOCKER_SUFFIX
    locker.intern().synchronized {
      val hdfsFileSystemContainer = if (fileSystemCache.containsKey(cacheKey)) {
        fileSystemCache.get(cacheKey)
      } else {
        // we use cacheLabel to create HDFSFileSystemContainer, and in the rest part of HDFSUtils, we consistently
        // use the same cacheLabel to operate HDFSFileSystemContainer, like close or remove.
        // At the same time, we don't want to change the behavior of createFileSystem which is out of HDFSUtils,
        // so we continue to use the original label to createFileSystem.
        val newHDFSFileSystemContainer =
          new HDFSFileSystemContainer(createFileSystem(userName, label, conf), userName, cacheLabel)
        fileSystemCache.put(cacheKey, newHDFSFileSystemContainer)
        newHDFSFileSystemContainer
      }
      hdfsFileSystemContainer.addAccessCount()
      hdfsFileSystemContainer.updateLastAccessTime
      hdfsFileSystemContainer.getFileSystem
    }
  } else {
    createFileSystem(userName, label, conf)
  }

  def createFileSystem(userName: String, conf: org.apache.hadoop.conf.Configuration): FileSystem =
    createFileSystem(userName, null, conf)

  def createFileSystem(
      userName: String,
      label: String,
      conf: org.apache.hadoop.conf.Configuration
  ): FileSystem =
    getUserGroupInformation(userName, label)
      .doAs(new PrivilegedExceptionAction[FileSystem] {
        // scalastyle:off FileSystemGet
        def run: FileSystem = FileSystem.get(conf)
        // scalastyle:on FileSystemGet
      })

  def closeHDFSFIleSystem(fileSystem: FileSystem, userName: String): Unit =
    closeHDFSFIleSystem(fileSystem, userName, null, false)

  def closeHDFSFIleSystem(fileSystem: FileSystem, userName: String, label: String): Unit =
    closeHDFSFIleSystem(fileSystem, userName, label, false)

  def closeHDFSFIleSystem(fileSystem: FileSystem, userName: String, isForce: Boolean): Unit =
    closeHDFSFIleSystem(fileSystem, userName, null, isForce)

  def closeHDFSFIleSystem(
      fileSystem: FileSystem,
      userName: String,
      label: String,
      isForce: Boolean
  ): Unit =
    if (null != fileSystem && StringUtils.isNotBlank(userName)) {
      if (HadoopConf.HDFS_ENABLE_CACHE) {
        val cacheLabel = if (label == null) DEFAULT_CACHE_LABEL else label
        val cacheKey = userName + JOINT + cacheLabel
        val hdfsFileSystemContainer = fileSystemCache.get(cacheKey)
        if (null != hdfsFileSystemContainer) {
          val locker = cacheKey + LOCKER_SUFFIX
          if (isForce) {
            locker synchronized fileSystemCache.remove(cacheKey)
            IOUtils.closeQuietly(hdfsFileSystemContainer.getFileSystem)
            logger.info(
              s"user${hdfsFileSystemContainer.getUser} to Force remove hdfsFileSystemContainer"
            )
          } else {
            locker synchronized hdfsFileSystemContainer.minusAccessCount()
          }
        }
      } else {
        IOUtils.closeQuietly(fileSystem)
      }
    }

  def getUserGroupInformation(userName: String): UserGroupInformation = {
    getUserGroupInformation(userName, null);
  }

  def getUserGroupInformation(userName: String, label: String): UserGroupInformation = {
    if (isKerberosEnabled(label)) {
      if (!isKeytabProxyUserEnabled(label)) {
        val path = new File(getKeytabPath(label), userName + ".keytab").getPath
        val user = getKerberosUser(userName, label)
        UserGroupInformation.setConfiguration(getConfigurationByLabel(userName, label))
        UserGroupInformation.loginUserFromKeytabAndReturnUGI(user, path)
      } else {
        val superUser = getKeytabSuperUser(label)
        val path = new File(getKeytabPath(label), superUser + ".keytab").getPath
        val user = getKerberosUser(superUser, label)
        UserGroupInformation.setConfiguration(getConfigurationByLabel(superUser, label))
        UserGroupInformation.createProxyUser(
          userName,
          UserGroupInformation.loginUserFromKeytabAndReturnUGI(user, path)
        )
      }
    } else {
      UserGroupInformation.createRemoteUser(userName)
    }
  }

  def isKerberosEnabled(label: String): Boolean = {
    if (label == null) {
      KERBEROS_ENABLE.getValue
    } else {
      kerberosValueMapParser(KERBEROS_ENABLE_MAP.getValue).get(label).contains("true")
    }
  }

  def isKeytabProxyUserEnabled(label: String): Boolean = {
    if (label == null) {
      KEYTAB_PROXYUSER_ENABLED.getValue
    } else {
      kerberosValueMapParser(KEYTAB_PROXYUSER_SUPERUSER_MAP.getValue).contains(label)
    }
  }

  def getKerberosUser(userName: String, label: String): String = {
    var user = userName
    if (label == null) {
      if (KEYTAB_HOST_ENABLED.getValue) {
        user = user + "/" + KEYTAB_HOST.getValue
      }
    } else {
      val hostMap = kerberosValueMapParser(KEYTAB_HOST_MAP.getValue)
      if (hostMap.contains(label)) {
        user = user + "/" + hostMap(label)
      }
    }
    user
  }

  def getKeytabSuperUser(label: String): String = {
    if (label == null) {
      KEYTAB_PROXYUSER_SUPERUSER.getValue
    } else {
      kerberosValueMapParser(KEYTAB_PROXYUSER_SUPERUSER_MAP.getValue)(label)
    }
  }

  def getKeytabPath(label: String): String = {
    if (label == null) {
      KEYTAB_FILE.getValue
    } else {
      val prefix = if (EXTERNAL_KEYTAB_FILE_PREFIX.getValue.endsWith("/")) {
        EXTERNAL_KEYTAB_FILE_PREFIX.getValue
      } else {
        EXTERNAL_KEYTAB_FILE_PREFIX.getValue + "/"
      }
      prefix + label
    }
  }

  private def kerberosValueMapParser(configV: String): Map[String, String] = {
    val confDelimiter = ","
    if (configV == null || "".equals(configV)) {
      Map()
    } else {
      configV
        .split(confDelimiter)
        .filter(x => x != null && !"".equals(x))
        .map(x => {
          val confArr = x.split("=")
          if (confArr.length == 2) {
            (confArr(0).trim, confArr(1).trim)
          } else null
        })
        .filter(kerberosValue =>
          kerberosValue != null && StringUtils.isNotBlank(
            kerberosValue._1
          ) && null != kerberosValue._2
        )
        .toMap
    }
  }

}
