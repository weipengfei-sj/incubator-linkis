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

package org.apache.linkis.cli.application.constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AppKeysTest {

  @Test
  @DisplayName("constTest")
  public void constTest() {

    String adminUsers = AppKeys.ADMIN_USERS;
    String linkisClientNoncustomizable = AppKeys.LINKIS_CLIENT_NONCUSTOMIZABLE;
    String logPathKey = AppKeys.LOG_PATH_KEY;
    String logFileKey = AppKeys.LOG_FILE_KEY;
    String clientConfigRootKey = AppKeys.CLIENT_CONFIG_ROOT_KEY;
    String defaultConfigFileNameKey = AppKeys.DEFAULT_CONFIG_FILE_NAME_KEY;
    String linuxUserKey = AppKeys.LINUX_USER_KEY;
    String jobExec = AppKeys.JOB_EXEC;
    String jobExecCode = AppKeys.JOB_EXEC_CODE;
    String jobContent = AppKeys.JOB_CONTENT;
    String jobSource = AppKeys.JOB_SOURCE;
    String jobParamConf = AppKeys.JOB_PARAM_CONF;
    String jobParamRuntime = AppKeys.JOB_PARAM_RUNTIME;
    String jobParamVar = AppKeys.JOB_PARAM_VAR;
    String jobLabel = AppKeys.JOB_LABEL;

    Assertions.assertEquals("hadoop,root,shangda", adminUsers);
    Assertions.assertEquals("wds.linkis.client.noncustomizable", linkisClientNoncustomizable);
    Assertions.assertEquals("log.path", logPathKey);
    Assertions.assertEquals("log.file", logFileKey);
    Assertions.assertEquals("conf.root", clientConfigRootKey);
    Assertions.assertEquals("conf.file", defaultConfigFileNameKey);
    Assertions.assertEquals("user.name", linuxUserKey);

    Assertions.assertEquals("wds.linkis.client.exec", jobExec);
    Assertions.assertEquals("wds.linkis.client.exec.code", jobExecCode);
    Assertions.assertEquals("wds.linkis.client.jobContent", jobContent);
    Assertions.assertEquals("wds.linkis.client.source", jobSource);

    Assertions.assertEquals("wds.linkis.client.param.conf", jobParamConf);
    Assertions.assertEquals("wds.linkis.client.param.runtime", jobParamRuntime);
    Assertions.assertEquals("wds.linkis.client.param.var", jobParamVar);

    Assertions.assertEquals("wds.linkis.client.label", jobLabel);
  }
}
