Kite logging example
=========================

This module provides an example of logging application events to Hadoop via Flume, using
log4j as the logging API.

## Pre-requisites

Before trying this example, you need to have a Flume agent running.

*   __Enable Flume user impersonation__ Flume needs to be able to impersonate the owner
 of the dataset it is writing to. (This is like Unix `sudo`, see
[Configuring Flume's Security Properties](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Security-Guide/cdh4sg_topic_4_2.html)
for further information.) 
    * In Cloudera Manager, for the [HDFS service](http://localhost:7180/cmf/services/status),
      click "View and Edit" under the Configuration tab then
      search for "Cluster-wide Configuration Safety Valve for core-site.xml"
      and add the following XML snippet, then save changes. This is already configured
      for Cloudera Manager 5 onwards.
    * If not using Cloudera Manager, just add the following XML snippet to your `core-site.xml` file 
      and then restart the HDFS daemons.

```
<property>
  <name>hadoop.proxyuser.flume.groups</name>
  <value>*</value>
</property>
<property>
  <name>hadoop.proxyuser.flume.hosts</name>
  <value>*</value>
</property>
```

* If you are running __Flume 1.3.0 or earlier,__ you must instead use the Kite event serializer module to support writing
   Flume events as Avro data files in HDFS.
    * [Download the JAR file](https://repository.cloudera.com/artifactory/libs-release-local/com/cloudera/cdk/cdk-flume-avro-event-serializer/0.8.1/cdk-flume-avro-event-serializer-0.8.1.jar) (you may need to change the URL to reflect the Kite version you're using).
    * Install that JAR file by copying it to the `/usr/lib/flume-ng/lib/` directory
    * Change the value of the `tier1.sinks.sink-1.serializer` property from `org.apache.flume.sink.hdfs.AvroEventSerializer$Builder` to `org.apache.flume.serialization.AvroEventSerializer$Builder` since this version of Flume has a built-in HDFS sink for writing Avro data files.

*   __Start a Flume agent__ 
    * First, check the value of the `tier1.sinks.sink-1.hdfs.proxyUser` in the `flume.properties` 
      file to ensure it matches your login username. The default value is `cloudera`, which is correct
      for the QuickStart VM, but you'll likely need to change this when running the example from another system.
    * If using Cloudera Manager, configure the Flume agent by following these steps:
        * Select "View and Edit" under the Flume service Configuration tab
        * Click on the "Agent (Default)" category
        * Paste the contents of the `flume.properties` file into the text area for the "Configuration File" property. 
        * Save your change
        * Start (or restart) the Flume agent
    * If not using Cloudera Manager, configure the Flume agent by following these steps:
        * Edit the `/etc/default/flume-ng-agent` file and add a line containing `FLUME_AGENT_NAME=tier1` 
          (this sets the default Flume agent name to match the one defined in the `flume.properties` file).
        * Run `sudo cp flume.properties /etc/flume-ng/conf/flume.conf` so the Flume agent uses our configuration file.
        * Run `sudo /etc/init.d/flume-ng-agent restart` to restart the Flume agent with this new configuration

## Running

To build the project, type

```bash
mvn package
```

The log data ends up in a dataset named "events". Before running the logger we need
to create the dataset on the filesystem with the following command:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.CreateDataset"
```

You can see the dataset directory hierarchy in [`/tmp/data/events`](http://localhost:8888/filebrowser/#/tmp/data/events),
In particular, the schema for the events is stored in
[`/tmp/data/events/.metadata/schema.avsc`](http://localhost:8888/filebrowser/#/tmp/data/events/.metadata/schema.avsc).

Now we can run the application to do the logging.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.App"
```

The program writes 10 log events to the logger. The events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink. (Even though it is
called the HDFS sink, it can actually write to any Hadoop filesystem,
including the local filesystem.) Log4j is using Kite's Flume
[`Log4jAppender`](https://github.com/kite-sdk/kite/blob/master/kite-data/kite-data-flume/src/main/java/org/kitesdk/data/flume/Log4jAppender.java)
in the project's [`log4j.properties`](https://github.com/kite-sdk/kite-examples/blob/master/logging/src/main/resources/log4j.properties)

The Flume sink will write a temporary file in [`/tmp/data/events`](http://localhost:8888/filebrowser/#/tmp/data/events).
After a few seconds the file will be renamed so it no longer has the _.tmp_
extension. If you don't see new files, make sure you have followed the [Setting up the QuickStart VM](https://github.com/kite-sdk/kite-examples#setting-up-the-quickstart-vm)
directions.

Run the following program to dump the contents of the dataset to the console:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.ReadDataset"
```

When you're done, you can delete the dataset:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.DeleteDataset"
```
