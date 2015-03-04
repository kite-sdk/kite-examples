/*
 * Copyright 2014 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.examples.data;

import java.util.LinkedList;
import java.util.List;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;

public abstract class BaseEventsTool extends Configured implements Tool {

  protected String uri = "dataset:hive:events";

  @Override
  public int run(String[] args) throws Exception {
    int start = 0;
    if (args.length >= 1 && !args[0].startsWith("--")) {
      uri = args[0];
      start = 1;
    }

    List<String> argsList = new LinkedList<String>();
    for (int i = start; i < args.length; i++) {
      argsList.add(args[i]);
    }

    return run(argsList);
  }

  public abstract int run(List<String> args) throws Exception;
}
