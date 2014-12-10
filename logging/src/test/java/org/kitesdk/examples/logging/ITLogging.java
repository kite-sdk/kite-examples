/**
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.examples.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.flume.clients.log4jappender.Log4jAppender;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kitesdk.minicluster.FlumeService;
import org.kitesdk.minicluster.HdfsService;
import org.kitesdk.minicluster.HiveService;
import org.kitesdk.minicluster.MiniCluster;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ITLogging {

  @Rule
  public static TemporaryFolder folder = new TemporaryFolder();

  private static MiniCluster cluster;

  @BeforeClass
  public static void startCluster() throws Exception {
    cluster = new MiniCluster.Builder().workDir("target/kite-minicluster").clean(true)
        .addService(HdfsService.class)
        .addService(HiveService.class)
        .addService(FlumeService.class).flumeAgentName("tier1")
          .flumeConfiguration("resource:flume.properties")
        .build();
    cluster.start();
    Thread.sleep(5000L);
    configureLog4j();
  }

  @Before
  public void setUp() {
    try {
      // delete dataset in case it already exists
      run(any(Integer.class), any(String.class), new DeleteDataset());
    } catch (Exception e) {
      // ignore - TODO: need to make sure DeleteDataset does not throw exception
    }
  }

  @AfterClass
  public static void stopCluster() throws Exception {
    try {
      cluster.stop();
    } catch (Exception e) {
      // ignore problems during shutdown
    }
  }

  private static void configureLog4j() throws Exception {
    // configuration is done programmatically and not in log4j.properties so that so we
    // can defer initialization to after the Flume Avro RPC source port is running
    Log4jAppender appender = new Log4jAppender();
    appender.setName("flume");
    appender.setHostname("localhost");
    appender.setPort(41415);
    appender.setUnsafeMode(true);
    appender.activateOptions();

    Logger.getLogger(App.class).addAppender(appender);
    Logger.getLogger(App.class).setLevel(Level.INFO);
  }

  @Test
  public void test() throws Exception {
    run(new CreateDataset());
    run(new App());
    Thread.sleep(40000); // wait for events to be flushed to HDFS
    run(containsString("{\"id\": 9, \"message\": \"Hello 9\"}"), new ReadDataset());
    run(new DeleteDataset());
  }

  public static void run(Tool tool, String... args) throws Exception {
    run(equalTo(0), any(String.class), tool, args);
  }

  public static void run(Matcher<String> stdOutMatcher, Tool tool,
      String... args) throws Exception {
    run(equalTo(0), stdOutMatcher, tool, args);
  }

  public static void run(Matcher<Integer> exitCodeMatcher,
      Matcher<String> stdOutMatcher,
      Tool tool,
      String... args) throws Exception {
    PrintStream oldStdOut = System.out;
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      System.setOut(new PrintStream(out));
      int rc = tool.run(args);
      assertThat(rc, exitCodeMatcher);
      assertThat(out.toString(), stdOutMatcher);
    } finally {
      System.setOut(oldStdOut);
    }
  }

}
