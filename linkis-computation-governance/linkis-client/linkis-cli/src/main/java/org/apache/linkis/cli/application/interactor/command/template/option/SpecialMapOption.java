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

package org.apache.linkis.cli.application.interactor.command.template.option;

import org.apache.linkis.cli.application.interactor.command.SpecialMap;

/** kv-pairs in SpecialMapOption will be excluded by varAccess */
public class SpecialMapOption extends MapOption implements Cloneable {
  public SpecialMapOption(
      String keyPrefix, String key, String[] paramNames, String description, boolean isOptional) {
    super(keyPrefix, key, paramNames, description, isOptional, new SpecialMap<>());
  }

  @Override
  public SpecialMapOption clone() throws CloneNotSupportedException {
    SpecialMapOption ret = (SpecialMapOption) super.clone();
    ret.value = new SpecialMap<>(this.value);
    return ret;
  }
}
