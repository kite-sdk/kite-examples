# CDK End-to-End Demo

This module provides an example of logging application events from a webapp to Hadoop
via Flume (using log4j as the logging API), extracting session data from the events using
Crunch, and finally analysing the session data with SQL using Hive or Impala.

## Prequisites

Before trying this example, you need to have installed Flume (this is explained in the
`logging` example).

You will also need a Hadoop and Hive installation. If you are running from tarballs you
will need to set the `HADOOP_HOME` and `HIVE_HOME` environment variables.

```bash
export HADOOP_HOME=~/dev/hadoop-2.0.0-cdh4.2.1
export HIVE_HOME=~/dev/hive-0.10.0-cdh4.2.1
```

## Building

To build the project, type

```bash
mvn package
```

This creates two artifacts: a JAR file containging classes for creating datasets and
running Crunch jobs, and a WAR file for the webapp that logs application events.

## Running

### Start the Flume agent

Open a new terminal and start a Flume agent with:

```bash
$FLUME_HOME/bin/flume-ng agent -n agent -c conf -f flume.properties
```

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

### Create events

Next we can run the webapp. It can can be used in a Java EE 6 servlet
container; for this example we'll start an embedded Tomcat instance using Maven:

```bash
mvn tomcat7:run
```

Navigate to [http://localhost:8080/demo-webapp/](http://localhost:8080/demo-webapp/),
which presents you with a very simple web page for sending messages.

The message events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink. (Even though it is
called the HDFS sink, it can actually write to any Hadoop filesystem,
including the local filesystem.)

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

The `sessions` dataset is now populated with data, which you can analyse using SQL. For
example:

```bash
$HIVE_HOME/bin/hive -e 'DESCRIBE sessions'
$HIVE_HOME/bin/hive -e 'SELECT * FROM sessions'
$HIVE_HOME/bin/hive -e 'SELECT AVG(duration) FROM sessions'
```