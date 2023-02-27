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

package org.apache.linkis.manager.label.entity.engine;

import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactory;
import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactoryContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** EngineTypeLabel Tester */
public class EngineTypeLabelTest {

  @Test
  public void testSetStringValue() {
    String engineType = "hive";
    String version = "1.1.0-cdh5.12.0";

    String engineType1 = "*";
    String version1 = "*";

    LabelBuilderFactory labelBuilderFactory = LabelBuilderFactoryContext.getLabelBuilderFactory();
    EngineTypeLabel engineTypeLabel = labelBuilderFactory.createLabel(EngineTypeLabel.class);
    engineTypeLabel.setStringValue(engineType + "-" + version);
    Assertions.assertEquals(engineTypeLabel.getEngineType(), engineType);
    Assertions.assertEquals(engineTypeLabel.getVersion(), version);

    engineTypeLabel.setStringValue(engineType1 + "-" + version1);
    Assertions.assertEquals(engineTypeLabel.getEngineType(), engineType1);
    Assertions.assertEquals(engineTypeLabel.getVersion(), version1);
  }
}
