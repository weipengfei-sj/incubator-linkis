#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#Actively load user env
source ~/.bash_profile
shellDir=`dirname $0`
workDir=`cd ${shellDir}/..;pwd`

#To be compatible with MacOS and Linux
txt=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    txt="''"
elif [[ "$OSTYPE" == "linux-gnu" ]]; then
    # linux
    txt=""
elif [[ "$OSTYPE" == "cygwin" ]]; then
    echo "linkis not support Windows operating system"
    exit 1
elif [[ "$OSTYPE" == "msys" ]]; then
    echo "linkis not support Windows operating system"
    exit 1
elif [[ "$OSTYPE" == "win32" ]]; then
    echo "linkis not support Windows operating system"
    exit 1
elif [[ "$OSTYPE" == "freebsd"* ]]; then

    txt=""
else
    echo "Operating system unknown, please tell us(submit issue) for better service"
    exit 1
fi

## import common.sh
source ${workDir}/bin/common.sh

##load config
echo "======= Step 1: Load deploy-config/* =========="
export LINKIS_CONFIG_PATH=${LINKIS_CONFIG_PATH:-"${workDir}/deploy-config/linkis-env.sh"}
export LINKIS_DB_CONFIG_PATH=${LINKIS_DB_CONFIG_PATH:-"${workDir}/deploy-config/db.sh"}

source ${LINKIS_CONFIG_PATH}
source ${LINKIS_DB_CONFIG_PATH}

isSuccess "load config"


echo "======= Step 2: Check env =========="
## check env
sh ${workDir}/bin/checkEnv.sh
isSuccess "check env"

until mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD  -e ";" ; do
     echo "try to connect to linkis mysql $MYSQL_HOST:$MYSQL_PORT/$MYSQL_DB failed, please check db configuration in:$LINKIS_DB_CONFIG_PATH"
     exit 1
done

echo "======= Step 3: Create necessary directory =========="

echo "[WORKSPACE_USER_ROOT_PATH] try to create directory"
if [ "$WORKSPACE_USER_ROOT_PATH" != "" ]
then
  localRootDir=$WORKSPACE_USER_ROOT_PATH
  if [[ $WORKSPACE_USER_ROOT_PATH == file://* ]];then
    localRootDir=${WORKSPACE_USER_ROOT_PATH#file://}
    echo "[WORKSPACE_USER_ROOT_PATH] try to create local dir,cmd is: mkdir -p $localRootDir/$deployUser"
    mkdir -p $localRootDir/$deployUser
    sudo chmod -R 775 $localRootDir/$deployUser
  elif [[ $WORKSPACE_USER_ROOT_PATH == hdfs://* ]];then
    localRootDir=${WORKSPACE_USER_ROOT_PATH#hdfs://}
    echo "[WORKSPACE_USER_ROOT_PATH] try to create hdfs dir,cmd is: hdfs dfs -mkdir -p $localRootDir/$deployUser"
    hdfs dfs -mkdir -p $localRootDir/$deployUser
    hdfs dfs -chmod -R 775 $localRootDir/$deployUser
  else
    echo "[WORKSPACE_USER_ROOT_PATH] does not support $WORKSPACE_USER_ROOT_PATH filesystem types"
  fi
  isSuccess "create WORKSPACE_USER_ROOT_PATH: $WORKSPACE_USER_ROOT_PATH directory"
fi



########################  init hdfs and db  ################################
echo "[HDFS_USER_ROOT_PATH] try to create directory"
 if [ "$HDFS_USER_ROOT_PATH" != "" ]
 then
     localRootDir=$HDFS_USER_ROOT_PATH
   if [[ $HDFS_USER_ROOT_PATH == file://* ]];then
     localRootDir=${HDFS_USER_ROOT_PATH#file://}
     echo "[HDFS_USER_ROOT_PATH] try to create local dir,cmd is: mkdir -p $localRootDir/$deployUser"
     mkdir -p $localRootDir/$deployUser
     sudo chmod -R 775 $localRootDir/$deployUser
   elif [[ $HDFS_USER_ROOT_PATH == hdfs://* ]];then
     localRootDir=${HDFS_USER_ROOT_PATH#hdfs://}
     echo "[HDFS_USER_ROOT_PATH] try to create hdfs dir,cmd is: hdfs dfs -mkdir -p $localRootDir/$deployUser"
     hdfs dfs -mkdir -p $localRootDir/$deployUser
   else
     echo "[HDFS_USER_ROOT_PATH] does not support $HDFS_USER_ROOT_PATH filesystem types"
   fi

   isSuccess "create HDFS_USER_ROOT_PATH: $HDFS_USER_ROOT_PATH directory"

 fi



echo "[RESULT_SET_ROOT_PATH] try to create directory"
 if [ "$RESULT_SET_ROOT_PATH" != "" ]
 then
   localRootDir=$RESULT_SET_ROOT_PATH
   if [[ $RESULT_SET_ROOT_PATH == file://* ]];then
     localRootDir=${RESULT_SET_ROOT_PATH#file://}
     echo "[RESULT_SET_ROOT_PATH] try to create local dir,cmd is: mkdir -p $localRootDir/$deployUser"
     mkdir -p $localRootDir/$deployUser
     sudo chmod -R 775 $localRootDir/$deployUser
   elif [[ $RESULT_SET_ROOT_PATH == hdfs://* ]];then
     localRootDir=${RESULT_SET_ROOT_PATH#hdfs://}
     echo "[RESULT_SET_ROOT_PATH] try to create hdfs dir,cmd is: hdfs dfs -mkdir -p $localRootDir"
     hdfs dfs -mkdir -p $localRootDir
     hdfs dfs -chmod 775 $localRootDir
   else
     echo "[RESULT_SET_ROOT_PATH] does not support $RESULT_SET_ROOT_PATH filesystem types"
   fi

   isSuccess "create RESULT_SET_ROOT_PATH: $RESULT_SET_ROOT_PATH directory"

 fi


if [ "$LINKIS_HOME" = "" ]
then
  export LINKIS_HOME=${workDir}/LinkisInstall
fi

if [  -d $LINKIS_HOME ] && [ "$LINKIS_HOME" != "$workDir" ];then
   echo "LINKIS_HOME: $LINKIS_HOME is alread exists and will be backed up"
   if [  -d $LINKIS_HOME-bak ];then
    rm -r $LINKIS_HOME-bak
   fi

   echo "mv  $LINKIS_HOME  $LINKIS_HOME-bak"
   mv  $LINKIS_HOME  $LINKIS_HOME-bak
   isSuccess "back up old LINKIS_HOME:$LINKIS_HOME to $LINKIS_HOME-bak"
fi
echo "try to create dir LINKIS_HOME: $LINKIS_HOME"
sudo mkdir -p $LINKIS_HOME;sudo chown -R $deployUser:$deployUser $LINKIS_HOME
isSuccess "create the dir of LINKIS_HOME:$LINKIS_HOME"

LINKIS_PACKAGE=${workDir}/linkis-package

if ! test -d ${LINKIS_PACKAGE}; then
    echo "**********${RED}Error${NC}: please put ${LINKIS_PACKAGE} in $workDir! "
    exit 1
else
    echo "Start to cp ${LINKIS_PACKAGE} to $LINKIS_HOME."
    cp -r $LINKIS_PACKAGE/* $LINKIS_HOME
    isSuccess "cp ${LINKIS_PACKAGE} to $LINKIS_HOME"
fi

cp ${LINKIS_CONFIG_PATH} $LINKIS_HOME/conf



echo "======= Step 4: Create linkis table =========="
## sql init
if [ "$YARN_RESTFUL_URL" != "" ]
then
  sed -i ${txt}  "s#@YARN_RESTFUL_URL#$YARN_RESTFUL_URL#g" $LINKIS_HOME/db/linkis_dml.sql
fi
if [ "$KERBEROS_ENABLE" != "" ]
then
  sed -i ${txt}  "s#@KERBEROS_ENABLE#$KERBEROS_ENABLE#g" $LINKIS_HOME/db/linkis_dml.sql
  sed -i ${txt}  "s#@PRINCIPAL_NAME#$PRINCIPAL_NAME#g" $LINKIS_HOME/db/linkis_dml.sql
  sed -i ${txt}  "s#@KEYTAB_PATH#$KEYTAB_PATH#g" $LINKIS_HOME/db/linkis_dml.sql
  sed -i ${txt}  "s#@KRB5_PATH#$KRB5_PATH#g" $LINKIS_HOME/db/linkis_dml.sql
else
  sed -i ${txt}  "s#@KERBEROS_ENABLE#false#g" $LINKIS_HOME/db/linkis_dml.sql
fi

common_conf=$LINKIS_HOME/conf/linkis.properties
SERVER_IP=$local_host

##Label set start
if [ "$SPARK_VERSION" != "" ]
then
  sed -i ${txt}  "s#spark-2.4.3#spark-$SPARK_VERSION#g" $LINKIS_HOME/db/linkis_dml.sql
  sed -i ${txt}  "s#\#wds.linkis.spark.engine.version.*#wds.linkis.spark.engine.version=$SPARK_VERSION#g" $common_conf
fi

if [ "$HIVE_VERSION" != "" ]
then
  sed -i ${txt}  "s#hive-2.3.3#hive-$HIVE_VERSION#g" $LINKIS_HOME/db/linkis_dml.sql
  sed -i ${txt}  "s#\#wds.linkis.hive.engine.version.*#wds.linkis.hive.engine.version=$HIVE_VERSION#g" $common_conf
fi

if [ "$PYTHON_VERSION" != "" ]
then
  sed -i ${txt}  "s#python-python2#python-$PYTHON_VERSION#g" $LINKIS_HOME/db/linkis_dml.sql
  sed -i ${txt}  "s#\#wds.linkis.python.engine.version.*#wds.linkis.python.engine.version=$PYTHON_VERSION#g" $common_conf
fi

echo "Linkis mysql DB will use this config: $MYSQL_HOST:$MYSQL_PORT/$MYSQL_DB"
echo "Do you want to clear Linkis table information in the database?"
echo " 1: Do not execute table-building statements"
echo -e "${RED} 2: Dangerous! Clear all data and rebuild the tables${NC}"
echo -e " other: exit\n"

MYSQL_INSTALL_MODE=1

read -p "[Please input your choice]:"  idx
if [[ '2' = "$idx" ]];then
  MYSQL_INSTALL_MODE=2
  echo "You chose Rebuild the table"
elif [[ '1' = "$idx" ]];then
  MYSQL_INSTALL_MODE=1
  echo "You chose not execute table-building statements"
else
  echo "no choice,exit!"
  exit 1
fi

##Label set end

#Deal special symbol '#'
HIVE_META_PASSWORD=$(echo ${HIVE_META_PASSWORD//'#'/'\#'})
MYSQL_PASSWORD=$(echo ${MYSQL_PASSWORD//'#'/'\#'})

#init db
if [[ '2' = "$MYSQL_INSTALL_MODE" ]];then
     mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD --default-character-set=utf8 -e "CREATE DATABASE IF NOT EXISTS $MYSQL_DB DEFAULT CHARSET utf8 COLLATE utf8_general_ci;"
  mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD -D$MYSQL_DB  --default-character-set=utf8 -e "source ${LINKIS_HOME}/db/linkis_ddl.sql"
  isSuccess "source linkis_ddl.sql"
  mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASSWORD -D$MYSQL_DB  --default-character-set=utf8 -e "source ${LINKIS_HOME}/db/linkis_dml.sql"
  isSuccess "source linkis_dml.sql"
  echo "Rebuild the table"
fi
###########################################################################


#Deal common config
echo ""
echo "======= Step 5: Update config =========="

if test -z "$EUREKA_INSTALL_IP"
then
  export EUREKA_INSTALL_IP=$SERVER_IP
fi
if [ "true" != "$EUREKA_PREFER_IP" ]
then
  export EUREKA_HOSTNAME=$EUREKA_INSTALL_IP
fi
export EUREKA_URL=http://$EUREKA_INSTALL_IP:$EUREKA_PORT/eureka/

if test -z "$GATEWAY_INSTALL_IP"
then
  export GATEWAY_INSTALL_IP=$SERVER_IP
fi

##eureka
sed -i ${txt}  "s#defaultZone:.*#defaultZone: $EUREKA_URL#g" $LINKIS_HOME/conf/application-eureka.yml
sed -i ${txt}  "s#port:.*#port: $EUREKA_PORT#g" $LINKIS_HOME/conf/application-eureka.yml

##server application.yml
sed -i ${txt}  "s#defaultZone:.*#defaultZone: $EUREKA_URL#g" $LINKIS_HOME/conf/application-linkis.yml

echo "update conf $common_conf"
sed -i ${txt}  "s#wds.linkis.server.version.*#wds.linkis.server.version=$LINKIS_SERVER_VERSION#g" $common_conf
sed -i ${txt}  "s#wds.linkis.gateway.url.*#wds.linkis.gateway.url=http://$GATEWAY_INSTALL_IP:$GATEWAY_PORT#g" $common_conf
sed -i ${txt}  "s#wds.linkis.eureka.defaultZone.*#wds.linkis.eureka.defaultZone=$EUREKA_URL#g" $common_conf
sed -i ${txt}  "s#wds.linkis.server.mybatis.datasource.url.*#wds.linkis.server.mybatis.datasource.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?characterEncoding=UTF-8#g" $common_conf
sed -i ${txt}  "s#wds.linkis.server.mybatis.datasource.username.*#wds.linkis.server.mybatis.datasource.username=$MYSQL_USER#g" $common_conf
sed -i ${txt}  "s#wds.linkis.server.mybatis.datasource.password.*#wds.linkis.server.mybatis.datasource.password=$MYSQL_PASSWORD#g" $common_conf
# hadoop config
sed -i ${txt}  "s#\#hadoop.config.dir.*#hadoop.config.dir=$HADOOP_CONF_DIR#g" $common_conf
#hive config
sed -i ${txt}  "s#\#hive.config.dir.*#hive.config.dir=$HIVE_CONF_DIR#g" $common_conf
#spark config
sed -i ${txt}  "s#\#spark.config.dir.*#spark.config.dir=$SPARK_CONF_DIR#g" $common_conf

sed -i ${txt}  "s#wds.linkis.home.*#wds.linkis.home=$LINKIS_HOME#g" $common_conf

sed -i ${txt}  "s#wds.linkis.filesystem.root.path.*#wds.linkis.filesystem.root.path=$WORKSPACE_USER_ROOT_PATH#g" $common_conf
sed -i ${txt}  "s#wds.linkis.filesystem.hdfs.root.path.*#wds.linkis.filesystem.hdfs.root.path=$HDFS_USER_ROOT_PATH#g" $common_conf

sed -i ${txt}  "s#wds.linkis.bml.hdfs.prefix.*#wds.linkis.bml.hdfs.prefix=$HDFS_USER_ROOT_PATH#g" $common_conf

##gateway
gateway_conf=$LINKIS_HOME/conf/linkis-mg-gateway.properties
echo "update conf $gateway_conf"
defaultPwd=`date +%s%N | md5sum |cut -c 1-9`
sed -i ${txt}  "s#wds.linkis.ldap.proxy.url.*#wds.linkis.ldap.proxy.url=$LDAP_URL#g" $gateway_conf
sed -i ${txt}  "s#wds.linkis.ldap.proxy.baseDN.*#wds.linkis.ldap.proxy.baseDN=$LDAP_BASEDN#g" $gateway_conf
sed -i ${txt}  "s#wds.linkis.ldap.proxy.userNameFormat.*#wds.linkis.ldap.proxy.userNameFormat=$LDAP_USER_NAME_FORMAT#g" $gateway_conf
sed -i ${txt}  "s#wds.linkis.admin.user.*#wds.linkis.admin.user=$deployUser#g" $gateway_conf
sed -i ${txt}  "s#\#wds.linkis.admin.password.*#wds.linkis.admin.password=$defaultPwd#g" $gateway_conf
if [ "$GATEWAY_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$GATEWAY_PORT#g" $gateway_conf
fi


manager_conf=$LINKIS_HOME/conf/linkis-cg-linkismanager.properties
if [ "$MANAGER_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$MANAGER_PORT#g" $manager_conf
fi

# ecm install
ecm_conf=$LINKIS_HOME/conf/linkis-cg-engineconnmanager.properties
if test -z $ENGINECONN_ROOT_PATH
then
  ENGINECONN_ROOT_PATH=$LINKIS_HOME/engineroot
fi
sed -i ${txt}  "s#wds.linkis.engineconn.root.dir.*#wds.linkis.engineconn.root.dir=$ENGINECONN_ROOT_PATH#g" $ecm_conf

if [ ! -d $ENGINECONN_ROOT_PATH ] ;then
    echo "create dir ENGINECONN_ROOT_PATH: $ENGINECONN_ROOT_PATH"
    mkdir -p $ENGINECONN_ROOT_PATH
fi
sudo chmod -R 771 $ENGINECONN_ROOT_PATH

if [ "$ENGINECONNMANAGER_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$ENGINECONNMANAGER_PORT#g" $ecm_conf
fi

ecp_conf=$LINKIS_HOME/conf/linkis-cg-engineplugin.properties
if [ "$ENGINECONN_PLUGIN_SERVER_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$ENGINECONN_PLUGIN_SERVER_PORT#g" $ecp_conf
fi

entrance_conf=$LINKIS_HOME/conf/linkis-cg-entrance.properties
if [ "$ENTRANCE_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$ENTRANCE_PORT#g" $entrance_conf
fi
if [ "$RESULT_SET_ROOT_PATH" != "" ]
then
  sed -i ${txt}  "s#wds.linkis.resultSet.store.path.*wds.linkis.resultSet.store.path=$RESULT_SET_ROOT_PATH#g" $entrance_conf
fi

publicservice_conf=$LINKIS_HOME/conf/linkis-ps-publicservice.properties
if [ "$PUBLICSERVICE_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$PUBLICSERVICE_PORT#g" $publicservice_conf
fi

metadatamanage_conf=$LINKIS_HOME/conf/linkis-ps-metadatamanager.properties
if [ "$METADATA_MANAGER_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$METADATA_MANAGER_PORT#g" $metadatamanage_conf
fi


##datasource
datasource_conf=$LINKIS_HOME/conf/linkis-ps-data-source-manager.properties
if [ "$DATASOURCE_MANAGER_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$DATASOURCE_MANAGER_PORT#g" $datasource_conf
fi

echo "update conf $datasource_conf"
if [ "$HIVE_META_URL" != "" ]
then
  sed -i ${txt}  "s#hive.meta.url.*#hive.meta.url=$HIVE_META_URL#g" $datasource_conf
  sed -i ${txt}  "s#hive.meta.url.*#hive.meta.url=$HIVE_META_URL#g" $publicservice_conf
fi
if [ "$HIVE_META_USER" != "" ]
then
  sed -i ${txt}  "s#hive.meta.user.*#hive.meta.user=$HIVE_META_USER#g" $datasource_conf
  sed -i ${txt}  "s#hive.meta.user.*#hive.meta.user=$HIVE_META_USER#g" $publicservice_conf
fi
if [ "$HIVE_META_PASSWORD" != "" ]
then
  HIVE_META_PASSWORD=$(echo ${HIVE_META_PASSWORD//'#'/'\#'})
  sed -i ${txt}  "s#hive.meta.password.*#hive.meta.password=$HIVE_META_PASSWORD#g" $datasource_conf
  sed -i ${txt}  "s#hive.meta.password.*#hive.meta.password=$HIVE_META_PASSWORD#g" $publicservice_conf
fi

cs_conf=$LINKIS_HOME/conf/linkis-ps-cs.properties
if [ "$CS_PORT" != "" ]
then
  sed -i ${txt}  "s#spring.server.port.*#spring.server.port=$CS_PORT#g" $cs_conf
fi

echo -e "\n"

echo -e "${GREEN}Congratulations!${NC} You have installed Linkis $LINKIS_VERSION successfully, please use sh $LINKIS_HOME/sbin/linkis-start-all.sh to start it!"
echo -e "Your default account/password is ${GREEN}[$deployUser/$defaultPwd]${NC}, you can find in $LINKIS_HOME/conf/linkis-mg-gateway.properties"
