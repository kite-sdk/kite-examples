CDK logging example
=========================

This module provides an example of logging application events to Hadoop via Flume, using
log4j as the logging API.

## Pre-requisites

Before trying this example, you need to have a Flume agent running.

First copy the CDK event serializer module into Flume's lib directory. This is necessary
since Flume 1.3.0 does not come with a HDFS sink that can write Avro data files.

```bash
sudo wget https://repository.cloudera.com/artifactory/libs-release-local/com/cloudera/cdk/cdk-flume-avro-event-serializer/0.4.0/cdk-flume-avro-event-serializer-0.4.0.jar \
  -P /usr/lib/flume-ng/lib/
```

Note that the HDFS sink in Flume 1.4.0 can write Avro data files so this step is not
needed for that version of Flume or later.

Next, start a Flume agent on the QuickStart VM. You can do this via Cloudera Manager by
selecting "View and Edit" under the Flume service Configuration tab, then clicking on the
"Agent (Default)" category, and pasting the contents of the `flume.properties` file in
this project into the text area for the "Configuration File" property.

If you are running this example from you machine and not from a QuickStart VM login,
then make sure you change the value of the `proxyUser` setting in the agent
configuration to the user that you are logged in as. Save changes,
then start the Flume agent.


## Running

To build the project, type

```bash
mvn package
```

The log data ends up in a dataset named "events". Before running the logger we need
to create the dataset on the filesystem with the following command:

```bash
java -cp target/*:target/jars/* com.cloudera.cdk.examples.logging.CreateDataset
```

You can see the dataset directory hierarchy in [`/tmp/data/events`](http://localhost:8888/filebrowser/#/tmp/data/events),
In particular, the schema for the events is stored in
[`/tmp/data/events/.metadata/schema.avsc`](http://localhost:8888/filebrowser/#/tmp/data/events/.metadata/schema.avsc).

Now we can run the application to do the logging.

```bash
java -cp target/*:target/jars/* com.cloudera.cdk.examples.logging.App
```

The program writes 10 log events to the logger. The events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink. (Even though it is
called the HDFS sink, it can actually write to any Hadoop filesystem,
including the local filesystem.)

The Flume sink will write a temporary file in [`/tmp/data/events`](http://localhost:8888/filebrowser/#/tmp/data/events).
After a few seconds the file will be renamed so it no longer has the _.tmp_ extension.
Run the following program to dump the contents of the dataset to the console:

```bash
java -cp target/*:target/jars/* com.cloudera.cdk.examples.logging.ReadDataset
```

