CDK logging example
=========================

This module provides an example of logging application events to Hadoop via Flume, using
log4j as the logging API.

## Pre-requisites

Before trying this example, you need to have a Flume agent running.

*   __Enable Flume user impersonation__ Flume needs to be able to impersonate the owner
 of the dataset it is writing to. (This is like Unix `sudo`, see
[Configuring Flume's Security Properties](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Security-Guide/cdh4sg_topic_4_2.html)
for further information.) In Cloudera Manager, for the [HDFS service](http://localhost:7180/cmf/services/status),
click "View and Edit" under the Configuration tab then
search for "Cluster-wide Configuration Safety Valve for core-site.xml"
and add the following XML snippet, then save changes.

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
*   __Install the CDK event serializer module__ This is necessary
since Flume 1.3.0 does not come with a HDFS sink that can write Avro data files.
Note that the HDFS sink in Flume 1.4.0 can write Avro data files so this step is not
needed for that version of Flume or later.

```bash
sudo wget https://repository.cloudera.com/artifactory/libs-release-local/com/cloudera/cdk/cdk-flume-avro-event-serializer/0.4.0/cdk-flume-avro-event-serializer-0.4.0.jar \
  -P /usr/lib/flume-ng/lib/
# or if wget is not available:
( cd /usr/lib/flume-ng/lib/ ; sudo curl -O https://repository.cloudera.com/artifactory/libs-release-local/com/cloudera/cdk/cdk-flume-avro-event-serializer/0.4.0/cdk-flume-avro-event-serializer-0.4.0.jar ; )
```
*   __Start a Flume agent__ You can do this via Cloudera Manager by
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
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.logging.CreateDataset"
```

You can see the dataset directory hierarchy in [`/tmp/data/events`](http://localhost:8888/filebrowser/#/tmp/data/events),
In particular, the schema for the events is stored in
[`/tmp/data/events/.metadata/schema.avsc`](http://localhost:8888/filebrowser/#/tmp/data/events/.metadata/schema.avsc).

Now we can run the application to do the logging.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.logging.App"
```

The program writes 10 log events to the logger. The events are sent to the Flume agent
over IPC, and the agent writes the events to the HDFS file sink. (Even though it is
called the HDFS sink, it can actually write to any Hadoop filesystem,
including the local filesystem.) Log4j is using the CDK's Flume [`Log4jAppender`](https://github.com/cloudera/cdk/blob/master/cdk-data/cdk-data-flume/src/main/java/org/apache/flume/clients/log4jappender/Log4jAppender.java)
in the project's [`log4j.properties`](https://github.com/cloudera/cdk-examples/blob/master/logging/src/main/resources/log4j.properties)

The Flume sink will write a temporary file in [`/tmp/data/events`](http://localhost:8888/filebrowser/#/tmp/data/events).
After a few seconds the file will be renamed so it no longer has the _.tmp_
extension. If you don't see new files, make sure you have followed the [Setting up the QuickStart VM](https://github.com/cloudera/cdk-examples#setting-up-the-quickstart-vm)
directions.

Run the following program to dump the contents of the dataset to the console:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.logging.ReadDataset"
```

