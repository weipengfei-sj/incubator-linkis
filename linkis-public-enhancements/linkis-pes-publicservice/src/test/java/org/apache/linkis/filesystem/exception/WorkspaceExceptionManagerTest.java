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

package org.apache.linkis.filesystem.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceExceptionManagerTest {

  private static final Logger LOG = LoggerFactory.getLogger(WorkspaceExceptionManagerTest.class);

  @Test
  @DisplayName("createExceptionTest")
  public void createExceptionTest() {

    WorkSpaceException exception = WorkspaceExceptionManager.createException(80021, "");
    Assertions.assertTrue(80021 == exception.getErrCode());
    Assertions.assertNotNull(exception.getDesc());

    Exception nullPointerException =
        Assertions.assertThrows(
            NullPointerException.class,
            () -> WorkspaceExceptionManager.createException(8002100, ""));
    LOG.info("assertThrows pass, the error message: {} ", nullPointerException.getMessage());
  }
}
