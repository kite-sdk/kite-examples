# CDK End-to-End Demo

This module provides an example of logging application events from a webapp to Hadoop
via Flume (using log4j as the logging API), extracting session data from the events using
Crunch, running the Crunch job periodically using Oozie, and analyzing the
session data with SQL using Hive.

## Pre-requisites

Before trying this example, you need to have installed the CDK event serializer module in
Flume (this is explained in the `logging` example).

Then, in the `demo` directory, start a Flume agent with the configuration specified in
`flume.properties`.

```bash
sudo flume-ng agent -n agent -c /etc/flume-ng/conf -f flume.properties
```

For Oozie you need to have Oozie's sharelib installed (which is taken care of already in
the QuickStart VM) and the Oozie service must be running - so start it using Cloudera
Manager.

Finally add the HCatalog Core JAR to the Hive Oozie sharelib:

```bash
sudo -u oozie hadoop fs -put \
  /usr/lib/hcatalog/share/hcatalog/hcatalog-core-0.5.0-cdh4.3.0.jar \
  /user/oozie/share/lib/hive
```

## Building

To build the project, type

```bash
mvn package
```

This creates the following artifacts:

* a JAR file containing the compiled Avro specific schema `session.avsc` (in `demo-core`)
* a JAR file containing classes for creating datasets (in `demo-admin`)
* a WAR file for the webapp that logs application events (in `demo-webapp`)
* a Hadoop job JAR file for running the Crunch job to transform events into sessions
(in `demo-crunch`)
* a Oozie application for running the Crunch job on a periodic basis (in `demo-oozie`)

## Running

### Create the datasets

First we need to create the datasets: one called `events` for the raw events,
and `sessions` for the derived sessions.

We store the raw events metadata in HDFS so Flume can find the schema (it would be nice
if we could store it using HCatalog, so we may lift this restriction in the future).
The sessions dataset metadata is stored using HCatalog, so we can query via Hive.

```bash
java -cp demo-admin/target/*:demo-admin/target/jars/* com.cloudera.cdk.examples.demo.CreateStandardEventDataset
java -cp demo-admin/target/*:demo-admin/target/jars/* com.cloudera.cdk.examples.demo.CreateSessionDataset

```
To allow Flume to write to our dataset we need to change the directory
permissions appropriately:

```bash
hadoop fs -chmod +w /tmp/data/events
```

### Create events

Next we can run the webapp. It can be used in a Java EE 6 servlet
container; for this example we'll start an embedded Tomcat instance using Maven:

```bash
cd demo-webapp
mvn tomcat7:run
```

Navigate to [http://localhost:8080/demo-webapp/](http://localhost:8080/demo-webapp/),
which presents you with a very simple web page for sending messages.

The message events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink.

Rather than creating lots of events manually, it's easier to simulate two users with
a script as follows:

```bash
./bin/simulate-activity.sh 1 10 > /dev/null &
./bin/simulate-activity.sh 2 10 > /dev/null &
```

### Generate the derived sessions

Wait about 30 seconds for Flume to flush the events to the filesystem,
then run the Crunch job to generate derived session data from the events:

```bash
export HADOOP_CLASSPATH=/usr/lib/hive/lib/*:/usr/lib/hcatalog/share/hcatalog/*
hadoop jar demo-crunch/target/demo-crunch-*-job.jar com.cloudera.cdk.examples.demo.CreateSessions
```

### Run session analysis

The `sessions` dataset is now populated with data, which you can analyze using SQL. For
example:

```bash
hive -e 'DESCRIBE sessions'
hive -e 'SELECT * FROM sessions'
hive -e 'SELECT AVG(duration) FROM sessions'
```

### Use Oozie to create derived sessions periodically

There are two parts to the Oozie piece - a workflow application that runs the
Crunch job, and a coordinator application that runs the workflow application once
every minute.

Oozie applications must be stored in HDFS for Oozie to access and run them, so
the first thing we do is deploy the assembled application (which contains both
a workflow and a coordinator application) to HDFS.

```bash
hadoop fs -rm -f -r create-sessions-workflow
hadoop fs -put demo-oozie/target/demo-oozie*-bundle/create-sessions-workflow/ create-sessions-workflow
```

Next we need to create events continuously, which we do by running the
user simulation script again. This time we don't specify a limit on the number
of events to create, so it runs indefinitely.

```bash
./bin/simulate-activity.sh 1
```

Now we can run the Oozie coordinator application.

```bash
export OOZIE_URL=http://localhost:11000/oozie
START=$(date -u +"%Y-%m-%dT%H:%MZ")
oozie job -config ./demo-oozie/src/main/workflow/job.properties \
  -D start=$START \
  -D initialDataset=$START \
  -D end="2013-12-31T00:00Z" -run
```

Monitor the coordinator and workflow jobs from the console with

```bash
oozie jobs -jobtype coordinator
oozie jobs # list workflow jobs
oozie job -info <job-id>
```

Alternatively, you can visit the Oozie web console at
[http://localhost:11000/oozie](http://localhost:11000/oozie), although it is not
enabled by default.

After a minute or two you should see new files appear in the `sessions` dataset.

```bash
hadoop fs -ls /tmp/data/sessions
```

When you see new files appear, then try running the session analysis from above.

When you have finished, stop the user simulation script by killing the process
(with Ctrl-C). Kill the Oozie job with:

```bash
oozie job -kill <job-id> 
```

### Troubleshooting Oozie

* Use the `-dryrun` command instead of `-run` when starting the Oozie
  application to see the workflow jobs that Oozie would launch.
  In particular, check the times that jobs are scheduled for. Note that
  times are in UTC so you may have to apply a conversion from the local
  timezone.
* Use the Oozie web console to find the MapReduce jobs that Oozie runs. By
  looking at them on the Hadoop web UI you can drill down to the task
  output to find any errors that have occurred.