CDK webapp logging example
==========================

This module provides an example of logging application events from a webapp to Hadoop
via Flume, using log4j as the logging API.

Before trying this example, run the `logging` example to install Flume,
start a Flume agent and create a dataset.

To build the project, type

```bash
mvn package
```

This creates a WAR file, which can be used in a Java EE 6 servlet container. For
this example we'll start an embedded Tomcat instance using Maven:

```
mvn tomcat7:run
```

Navigate to http://localhost:8080/logging-webapp/, which presents you with a very
simple web page for sending messages.

The message events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink.

The Flume sink will write a temporary file in _/tmp/data/events/_. Send 10 messages
using the web form. Then wait a few seconds for the file to be renamed so it no
longer has the _.tmp_ extension.

Then run the following program from the `logging` example to dump the contents of the
dataset to the console:

```bash
java -cp target/*:target/jars/* com.cloudera.cdk.examples.logging.ReadDataset
```
