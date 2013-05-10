CDK logging example
=========================

This module provides an example of logging application events to Hadoop via Flume, using
log4j as the logging API.

To build the project, type

```bash
mvn package
```

This creates a single JAR with all the dependencies inside for convenience.

Next, start a Flume agent running on localhost. The agent will listen for events sent
by the logger and write them to a Flume filesystem sink.

If you haven't already installed Flume, then do so first. You can use Cloudera's
downloadable tarballs from:

http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDHTarballs/3.25.2013/CDH4-Downloadable-Tarballs/CDH4-Downloadable-Tarballs.html)

or via packages from:

https://ccp.cloudera.com/display/SUPPORT/CDH+Downloads

Copy the CDK event serializer module into Flume's lib directory. This is necessary
since Flume 1.3.0 does not come with a HDFS sink that can write Avro data files.

TODO

Open a new terminal and start a Flume agent with:

```bash
$FLUME_HOME/bin/flume-ng agent -n agent -c conf -f flume.properties
```

The log data ends up in a dataset named "events". Before running the logger we need
to create the dataset on the filesystem with the following command:

```bash
java -cp target/logging-*-jar-with-dependencies.jar com.cloudera.cdk.examples.logging.CreateDataset
```

You can see the dataset directory hierarchy by running

```bash
find /tmp/data/events
```

In particular, the schema for the events is stored in a _.metadata_ directory:

```bash
cat /tmp/data/events/.metadata/schema.avsc
```

Now we can run the application to do the logging.

```bash
java -cp target/logging-*-jar-with-dependencies.jar com.cloudera.cdk.examples.logging.App
```

The program writes 10 log events to the logger. The events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink. (Even though it is
called the HDFS sink, it can actually write to any Hadoop filesystem,
including the local filesystem.)

The Flume sink will write a temporary file in _/tmp/data/events/_. After a few seconds
the file will be renamed so it no longer has the _.tmp_ extension. Run the following
program to dump the contents of the dataset to the console:

```bash
java -cp target/logging-*-jar-with-dependencies.jar com.cloudera.cdk.examples.logging.ReadDataset
```

