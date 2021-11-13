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
 
package org.apache.linkis.cli.application.driver.transformer;

import org.apache.linkis.cli.common.entity.execution.jobexec.JobExec;
import org.apache.linkis.cli.common.entity.execution.jobexec.JobStatus;
import org.apache.linkis.cli.common.exception.LinkisClientRuntimeException;
import org.apache.linkis.cli.core.presenter.model.JobExecModel;
import org.apache.linkis.httpclient.dws.response.DWSResult;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @description: transform data to be compatible with {@link JobExec}
 */
public interface DriverTransformer {
    String convertJobID(String taskID);

    JobStatus convertJobStatus(String jobStatus);

    JobExec convertAndUpdateExecData(JobExec execData, DWSResult result) throws LinkisClientRuntimeException;

    JobExecModel convertAndUpdateModel(JobExecModel model, DWSResult result) throws LinkisClientRuntimeException;

    List<LinkedHashMap<String, String>> convertResultMeta(Object rawMetaData) throws LinkisClientRuntimeException;

    List<List<String>> convertResultContent(Object rawContent) throws LinkisClientRuntimeException;
}