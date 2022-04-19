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

shellDir=`dirname $0`
workDir=`cd ${shellDir}/..;pwd`
source ${workDir}/bin/common.sh

say() {
    printf 'check command fail \n %s\n' "$1"
}

err() {
    say "$1" >&2
    exit 1
}

function checkPythonAndJava(){
    python --version > /dev/null 2>&1
    isSuccess "execute cmd: python --version"
    java -version > /dev/null 2>&1
    isSuccess "execute cmd: java --version"
}

function checkHadoopAndHive(){
    hadoopVersion="`hdfs version`"
    defaultHadoopVersion="2.7"
    checkversion "$hadoopVersion" $defaultHadoopVersion hadoop
    checkversion "$(whereis hive)" "2.3" hive
}

function checkversion(){
versionStr=$1
defaultVersion=$2
module=$3

result=$(echo $versionStr | grep "$defaultVersion")
if [ -n "$result" ]; then
    echo -e "Your [$module] version may match default support version: $defaultVersion\n"
else
   echo "WARN: Your [$module] version is not match default support version: $defaultVersion, there may be compatibility issues:"
   echo " 1: Continue installation, there may be compatibility issues"
   echo " 2: Exit installation"
   echo -e " other: exit\n"

   read -p "[Please input your choice]:"  idx
   if [[ '1' != "$idx" ]];then
    echo -e "You chose  Exit installation\n"
    exit 1
   fi
   echo ""
fi
}

function checkSpark(){
 spark-submit --version > /dev/null 2>&1
 isSuccess "execute cmd: spark-submit --version "
}


check_cmd() {
    command -v "$1" > /dev/null 2>&1
}

need_cmd() {
    if ! check_cmd "$1"; then
        err "need '$1' (your linux command not found)"
    fi
}


echo "<-----start to check used cmd---->"
echo "check yum"
need_cmd yum
echo "check java"
need_cmd java
echo "check mysql"
need_cmd mysql
echo "check telnet"
need_cmd telnet
echo "check tar"
need_cmd tar
echo "check sed"
need_cmd sed
echo "<-----end to check used cmd---->"

checkSpark
checkPythonAndJava
checkHadoopAndHive

