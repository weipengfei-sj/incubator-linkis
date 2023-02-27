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

package org.apache.linkis.engineconnplugin.flink.ql

import org.apache.linkis.common.utils.ClassUtils
import org.apache.linkis.engineconnplugin.flink.context.FlinkEngineConnContext
import org.apache.linkis.engineconnplugin.flink.errorcode.FlinkErrorCodeSummary._
import org.apache.linkis.engineconnplugin.flink.exception.SqlExecutionException

import java.text.MessageFormat

import scala.collection.convert.WrapAsScala._

object GrammarFactory {

  private val grammars = ClassUtils.reflections
    .getSubTypesOf(classOf[Grammar])
    .filterNot(ClassUtils.isInterfaceOrAbstract)
    .map(_.newInstance)
    .toArray

  def getGrammars: Array[Grammar] = grammars

  def apply(sql: String, context: FlinkEngineConnContext): Grammar = getGrammar(sql, context)
    .getOrElse(
      throw new SqlExecutionException(MessageFormat.format(NOT_SUPPORT_GRAMMAR.getErrorDesc, sql))
    )

  def getGrammar(sql: String, context: FlinkEngineConnContext): Option[Grammar] =
    grammars.find(_.canParse(sql)).map(_.copy(context, sql))

}
