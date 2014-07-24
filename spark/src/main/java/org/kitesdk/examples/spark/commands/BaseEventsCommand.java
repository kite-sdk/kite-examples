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

package org.kitesdk.examples.spark.commands;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import org.kitesdk.cli.commands.BaseCommand;
import org.slf4j.Logger;

public abstract class BaseEventsCommand extends BaseCommand {

  @Parameter(names = {"--use-local"},
      description = "Store data in local files")
  boolean local = false;

  @Parameter(names = {"--use-hdfs"},
      description = "Store data in HDFS files")
  boolean hdfs = false;

  @Parameter(names = {"--use-hive"},
      description = "Store data in Hive managed tables (default)")
  boolean hive = true;

  protected final Logger console;

  public BaseEventsCommand(Logger console) {
    this.console = console;
  }

  public String createUri() {
    return createUri("events");
  }

  public String createUri(String name) {
    String uri;
    if (local) {
      Preconditions.checkArgument(!(hdfs || hive),
          "Only one storage implementation can be selected");
      uri = "dataset:file:/tmp/data/";
    } else if (hdfs) {
      Preconditions.checkArgument(!(hive),
          "Only one storage implementation can be selected");
      uri = "dataset:hdfs:/tmp/data/";
    }   else {
      uri = "dataset:hive?dataset=";
    }

    return uri + name;
  }

}
