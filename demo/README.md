# CDK End-to-End Demo

This module provides an example of logging application events from a webapp to Hadoop
via Flume (using log4j as the logging API), extracting session data from the events using
Crunch, and finally analyzing the session data with SQL using Hive or Impala.

## Pre-requisites

Before trying this example, you need to have installed the CDK event serializer module in
Flume (this is explained in the `logging` example).

Then, in the `demo` directory, start a Flume agent with the configuration specified in
`flume.properties`.

```
sudo flume-ng agent -n agent -c /etc/flume-ng/conf -f flume.properties
```

## Building

To build the project, type

```bash
mvn package
```

This creates two artifacts: a JAR file containing classes for creating datasets and
running Crunch jobs, and a WAR file for the webapp that logs application events.

## Running

### Create the datasets

First we need to create the datasets: one called `events` for the raw events,
and `sessions` for the derived sessions.

We store the raw events metadata in HDFS so Flume can find the schema (it would be nice
if we could store it using HCatalog, so we may lift this restriction in the future).
The sessions dataset metadata is stored using HCatalog, so we can query via Hive or
Impala.

```bash
java -cp demo-core/target/*:demo-core/target/jars/* com.cloudera.cdk.examples.demo.CreateStandardEventDataset
java -cp demo-core/target/*:demo-core/target/jars/* com.cloudera.cdk.examples.demo.CreateSessionDataset

```
To allow Flume to write to our dataset we need to change the directory
permissions appropriately:

```
hadoop fs -chmod +w /tmp/data/events
```

### Create events

Next we can run the webapp. It can can be used in a Java EE 6 servlet
container; for this example we'll start an embedded Tomcat instance using Maven:

```bash
mvn tomcat7:run
```

Navigate to [http://localhost:8080/demo-webapp/](http://localhost:8080/demo-webapp/),
which presents you with a very simple web page for sending messages.

The message events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink.

Rather than creating lots of events manually, it's easier to simulate two users using
a script as follows:

```bash
./bin/simulate-activity.sh 1 10 > /dev/null &
./bin/simulate-activity.sh 2 10 > /dev/null &
```

### Generate the derived sessions

Wait about 30 seconds for Flume to flush the events to the filesystem,
then run the Crunch job to generate derived session data from the events:

```bash
java -Xmx1024m -cp demo-core/target/*:demo-core/target/jars/* com.cloudera.cdk.examples.demo.CreateSessions
```

### Run session analysis

The `sessions` dataset is now populated with data, which you can analyze using SQL. For
example:

```bash
hive -e 'DESCRIBE sessions'
hive -e 'SELECT * FROM sessions'
hive -e 'SELECT AVG(duration) FROM sessions'
```