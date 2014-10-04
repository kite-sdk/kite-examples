Kite logging example
=========================

This module provides an example of logging application events to Hadoop via Flume, using
log4j as the logging API.

## Pre-requisites

Before trying this example, you need to have a Flume agent running.

### __Enable Flume user impersonation__
Flume needs to be able to impersonate the owner of the dataset it is writing to.
(This is like Unix `sudo`, see
[Configuring Flume's Security Properties](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Security-Guide/cdh4sg_topic_4_2.html)
for further information.) 
    * If you're using Cloudera Manager (the QuickStart VM ships with Cloudera Manager,
      but by default it is not enabled) then this is already configured for you.
    * If you're not using Cloudera Manager, just add the following XML snippet to your
      `/etc/hadop/conf/core-site.xml` file and then restart the NameNode with
      `sudo service hadoop-hdfs-namenode restart`.

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

### __Configure the Flume agent__
* First, check the value of the `tier1.sinks.sink-1.auth.proxyUser` in the
  [flume.properties](flume.properties), file to ensure it matches your login
  username. The default value is `cloudera`, which is correct for the
  QuickStart VM, but you'll likely need to change this when running the example
  from another system.
* Next we need to add a `plugins.d` directory to configure Flume with some additional
  dependencies. To do this, run the [configure-flume.sh script](../configure-flume.sh)
  from the [root of the kite-examples repository](http://github.com/kite-sdk/kite-examples)
  using sudo:
```bash
sudo ../configure-flume.sh
```
* If using Cloudera Manager, configure the Flume agent by following these steps:
    * Select "View and Edit" under the Flume service Configuration tab
    * Click on the "Agent (Default)" category
    * Paste the contents of the [flume.properties](flume.properties) file into
      the text area for the "Configuration File" property.
    * Save your change
* If you're not using Cloudera Manager, configure the Flume agent by following
  these steps:
    * Edit the `/etc/default/flume-ng-agent` file and add a line containing
      `FLUME_AGENT_NAME=tier1` (this sets the default Flume agent name to match
      the one defined in the `flume.properties` file).
    * Edit the `/etc/default/flume-ng-agent` file and add a line containing
      `export FLUME_JAVA_OPTS=-Xmx100m` (this sets the heap size to 100MB).
    * Run `sudo cp flume.properties /etc/flume-ng/conf/flume.conf` so the Flume
      agent uses our configuration file.
    * Create a `/etc/flume-ng/conf/flume-env.sh` file with the following contents:
```
# Note that the Flume conf directory is always included in the classpath.
FLUME_CLASSPATH="/etc/hive/conf"
```

__NOTE:__ Don't start Flume immediately after updating the configuration. Flume
requires that the dataset already exist before it will start correctly.

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

Since this is a Hive dataset, you can see the table definition in [Hue's metastore browser](http://quickstart.cloudera:8888/metastore/table/default/events).

Before we start our application, start the Flume agent:

* If using Cloudera Manager:
    * Start (or restart) the Flume agent
* If not using Cloudera Manager:
    * Run `sudo /etc/init.d/flume-ng-agent restart` to restart the Flume agent.

Now we can run the application to do the logging.

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.App"
```

The program writes 10 log events to the logger. The events are sent to the Flume agent
over IPC, and the agent writes the events to the Kite Dataset sink. Log4j is using Flume's
[`Log4jAppender`](https://github.com/apache/flume/blob/trunk/flume-ng-clients/flume-ng-log4jappender/src/main/java/org/apache/flume/clients/log4jappender/Log4jAppender.java)
in the project's [`log4j.properties`](src/main/resources/log4j.properties)

The Flume sink will write a temporary file in [`/tmp/data/default/events`](http://quickstart.cloudera:8888/filebrowser/#/tmp/data/default/events).
After a few seconds the file will be renamed so it no longer has the _.tmp_
extension. If you don't see new files, make sure you have followed the [Setting up the QuickStart VM](https://github.com/kite-sdk/kite-examples#setting-up-the-quickstart-vm)
directions.

Run the following program to dump the contents of the dataset to the console:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.ReadDataset"
```

You can also browse the data using
[Hue's metastore browser](http://quickstart.cloudera:8888/metastore/table/default/events/read).

If you want to move on to the Kite [logging-webapp](../logging-webapp) example,
then keep the dataset you just created otherwise, you can delete the dataset:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.logging.DeleteDataset"
```
