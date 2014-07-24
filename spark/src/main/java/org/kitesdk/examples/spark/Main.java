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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.kitesdk.cli.Command;
import org.kitesdk.examples.spark.commands.CorrelateEventsCommand;
import org.kitesdk.examples.spark.commands.CreateEventsCommand;
import org.kitesdk.examples.spark.commands.DeleteEventsCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Configured implements Tool {

  @Parameter(names = {"-v", "--verbose", "--debug"},
  description = "Print extra debugging information")
  private boolean debug = false;
  @VisibleForTesting
  static final String PROGRAM_NAME = "kite-spark-demo";
  private final Logger console;
  @VisibleForTesting
  final JCommander jc;

  Main(Logger console) {
    this.console = console;
    this.jc = new JCommander(this);
    jc.setProgramName(PROGRAM_NAME);
    jc.addCommand("create", new CreateEventsCommand(console));
    jc.addCommand("delete", new DeleteEventsCommand(console));
    jc.addCommand("correlate", new CorrelateEventsCommand(console));
  }

  @Override
  public int run(String[] args) throws Exception {
    try {
      jc.parse(args);
    } catch (Exception e) {
      console.error(e.getMessage());
      return 1;
    }

    // configure log4j
    if (debug) {
      org.apache.log4j.Logger mainLogger = org.apache.log4j.Logger.getLogger(Main.class);
      mainLogger.setLevel(Level.DEBUG);
    }

    String parsed = jc.getParsedCommand();
    if (parsed == null) {
      console.error("Unknown command");
      return 1;
    }

    Command command = (Command) jc.getCommands().get(parsed).getObjects().get(0);
    if (command == null) {
      console.error("Unknown command");
      return 1;
    }

    try {
      if (command instanceof Configurable) {
        ((Configurable) command).setConf(getConf());
      }
      return command.run();
    } catch (Exception e) {
      if (debug) {
        console.error("Error", e);
      } else {
        console.error("Error: {}", e.getMessage());
      }
      return 1;
    }
  }

  public static void main(String[] args) throws Exception {
    // reconfigure logging with the kite CLI configuration
    PropertyConfigurator.configure(
        Main.class.getResource("/spark-cli-logging.properties"));
    Logger console = LoggerFactory.getLogger(Main.class);
    int rc = ToolRunner.run(new Configuration(), new Main(console), args);
    System.exit(rc);
  }
}
