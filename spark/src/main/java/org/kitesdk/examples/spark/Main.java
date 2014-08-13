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
package org.kitesdk.examples.spark;

import java.util.Arrays;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;

public class Main {


  public static void main(String[] args) throws Exception {
    String command = "";
    List<String> argsList = Arrays.asList(args);
    for (int i = 0; i < argsList.size(); i++) {
      if ("create".equals(argsList.get(i)) ||
          "correlate".equals(argsList.get(i))) {
        command = argsList.get(i);
        argsList.remove(i);
        break;
      }
    }
    args = argsList.toArray(new String[argsList.size()]);

    int rc;

    if ("create".equals(command)) {
      rc = ToolRunner.run(new Configuration(), new CreateEvents(), args);
    } else if ("correlate".equals(command)) {
      rc = ToolRunner.run(new Configuration(), new CorrelateEvents(), args);
    } else {
      rc = -1;
      System.err.println("Error: Unknown command: " + command);
      System.err.println("Usage: create|correlate|delete [<options>]");
    }

    System.exit(rc);
  }
}
