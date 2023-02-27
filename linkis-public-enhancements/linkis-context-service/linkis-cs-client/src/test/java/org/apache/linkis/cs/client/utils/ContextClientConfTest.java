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

package org.apache.linkis.cs.client.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContextClientConfTest {

  @Test
  @DisplayName("constTest")
  public void constTest() {

    String contextClientAuthKey = ContextClientConf.CONTEXT_CLIENT_AUTH_KEY().getValue();
    String contextClientAuthValue = ContextClientConf.CONTEXT_CLIENT_AUTH_VALUE().getValue();
    String urlPrefix = ContextClientConf.URL_PREFIX().getValue();
    String hearBeatEnabled = ContextClientConf.HEART_BEAT_ENABLED().getValue();

    Assertions.assertEquals("Token-Code", contextClientAuthKey);
    Assertions.assertEquals("BML-AUTH", contextClientAuthValue);
    Assertions.assertEquals("/api/rest_j/v1/contextservice", urlPrefix);
    Assertions.assertEquals("true", hearBeatEnabled);
  }
}
