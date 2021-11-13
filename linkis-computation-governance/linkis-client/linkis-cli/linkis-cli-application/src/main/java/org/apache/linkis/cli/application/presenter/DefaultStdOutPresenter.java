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
 
package org.apache.linkis.cli.application.presenter;

import org.apache.linkis.cli.core.presenter.Presenter;
import org.apache.linkis.cli.core.presenter.display.DisplayDriver;
import org.apache.linkis.cli.core.presenter.display.StdOutDriver;
import org.apache.linkis.cli.core.presenter.model.PresenterModel;
import org.apache.linkis.cli.core.utils.CommonUtils;


public class DefaultStdOutPresenter implements Presenter {
    DisplayDriver driver = new StdOutDriver();

    @Override
    public void checkInit() {

    }

    @Override
    public void present(PresenterModel model) {
        driver.doOutput(CommonUtils.GSON.toJson(model));
    }
}
