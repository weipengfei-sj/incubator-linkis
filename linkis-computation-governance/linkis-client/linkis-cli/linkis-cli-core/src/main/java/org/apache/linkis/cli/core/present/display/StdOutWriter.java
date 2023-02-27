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

package org.apache.linkis.cli.core.present.display;

import org.apache.linkis.cli.common.exception.error.ErrorLevel;
import org.apache.linkis.cli.core.exception.PresenterException;
import org.apache.linkis.cli.core.exception.error.CommonErrMsg;
import org.apache.linkis.cli.core.present.display.data.DisplayData;
import org.apache.linkis.cli.core.present.display.data.StdoutDisplayData;
import org.apache.linkis.cli.core.utils.LogUtils;

import org.slf4j.Logger;

public class StdOutWriter implements DisplayOperator {
  @Override
  public void doOutput(DisplayData data) {
    if (!(data instanceof StdoutDisplayData)) {
      throw new PresenterException(
          "PST0008",
          ErrorLevel.ERROR,
          CommonErrMsg.PresentDriverErr,
          "input data is not instance of StdoutDisplayData");
    }
    String content = ((StdoutDisplayData) data).getContent();
    Logger logger = LogUtils.getPlaintTextLogger();
    logger.info(content);
  }
}
