# Kite example: storing JSON records

This example covers demonstrates how to take incoming JSON records and
efficiently store them in a Kite Dataset -- no code required!

Data doesn't always come conveniently formatted as avro records, but it's
generally a good idea to use avro as the storage format for a dataset. We want
to convert the records to avro as they are delivered so they can be handled the
same way that records arriving in avro format are. And, preferably, we want to
configure existing components rather than maintaining a custom project. This
example demonstrates how to configure [Morphlines][morphlines], working inside
flume, to convert JSON records to avro and store them in a dataset.

While this guide is focused on converting JSON records inside flume, Morphlines
can be configured to handle a variety of tasks and transforms, documented in
its [Morphlines Reference Guide][refguide].

__Note__: Unlike the other examples, this example requires a CDH5.0 or later
cluster (such as the [Cloudera Quickstart VM][getvm]).

[morphlines]: http://kitesdk.org/docs/current/kite-morphlines/index.html
[refguide]: http://kitesdk.org/docs/current/kite-morphlines/morphlinesReferenceGuide.html
[getvm]: http://www.cloudera.com/content/support/en/downloads/quickstart_vms.html

## Configuration

The JSON records we want to tranform arrive through a flume source, called
`listener`. To transform the records to avro, we attach a flume morphline
interceptor to the source that runs before records are sent through its output
channel and eventually stored in HDFS (just like in the [demo
example](../demo/)). Morphlines has its own configuration, which can contain
several reusable sequences of built-in commands.

### Dataset

Before configuring flume and morphlines, we need to create the Dataset that
flume will populate. We will use the Kite maven plugin to do this. First, copy
the schema we will use, `user.avsc` to `/etc/flume-ng/schemas/user.avsc`, where
morphlines and flume will be able to find it:

```bash
sudo mkdir /etc/flume-ng/schemas
sudo cp user.avsc /etc/flume-ng/schemas/user.avsc
```

Next, run the maven command to create a Dataset using that schema:

```bash
mvn kite:create-dataset \
  -Dkite.rootDirectory=/tmp/data \
  -Dkite.datasetName=users \
  -Dkite.avroSchemaFile=/etc/flume-ng/schemas/user.avsc
```

The schema we are using defines a simple `User` object with a username, a
timestamp for when the user was created, and a favorite color.

### Morphlines

A sequence of commands in morphlines is called a morphline. The one we will use
to convert JSON to avro is in [`morphline.conf`](morphline.conf) and looks like
the config below. Some boilerplate that names the morphline `convertJsonToAvro`
has been removed, which can be seen in the file.

__Morphline config__:

```
# read the JSON blob
{ readJson: {} }
# extract JSON objects into fields
{ extractJsonPaths {
  flatten: true
  paths: {
    username: /username
    favoriteColor: /color
  }
} }
# add a creation timestamp to the record
{ addCurrentTime {
  field: timestamp
  preserveExisting: true
} }
# convert the extracted fields to an avro object
# described by the schema in this field
{ toAvro {
  schemaFile: /etc/flume-ng/schemas/user.avsc
} }
# serialize the object as avro
{ writeAvroToByteArray: {
  format: containerlessBinary
} }
```

These commands are run in sequence on each record, updating the flume event as
they go:

1. `readJson` parses the event's body as a JSON string

2. `extractJsonPaths` creates event headers from data in the JSON object
   according to the `paths` specified: for example, the `username` header gets
   the value from the JSON record's root "username" field

3. `addCurrentTime` adds a `timestamp` event header if one isn't already there

4. `toAvro` creates an avro record using the given schema, `user.avsc`, and
   adds data to it from the header fields we created in steps 2 and 3

5. `writeAvroToByteArray` serializes the new avro record and sets it as the
   event body for further processing in flume

After this morphline runs, the JSON body of a flume event has been turned into
an avro record. And, it's easy to see how this configuration could do other
transforms: changing the configuration for `extractJsonPaths` changes what is
used from the JSON record, and other read commands are used to convert
different payload formats.

To use this configuration, copy the `morphline.conf` file to
`/etc/flume-ng/conf/morphline.conf` on the host with your flume agent:

```bash
sudo cp morphline.conf /etc/flume-ng/conf/morphline.conf
```

### Flume

The last step is to create a flume configuration that uses our morphline. The
full configuration is in [flume.properties](flume.properties), which can be
copied as the full flume agent config.

Flume
has two ways of calling a morphline, either in a sink or in an interceptor. We
will use the interceptor, which can transform an event before putting it in a
source's outgoing channel. The source has been configured as
`tier1.sources.listener`, so interceptors are added underneath its configuration.

In addition to the morphline interceptor, we need to add an interceptor that
puts the avro record's schema in the flume event's headers for the HDFS sink.
This is done with a static interceptor because the schema doesn't change.

__Interceptor config__:

```
# attach the schema to the record, then convert it to avro
tier1.sources.listener.interceptors = attach-schema morphline

# add the schema for our record sink
tier1.sources.listener.interceptors.attach-schema.type = static
tier1.sources.listener.interceptors.attach-schema.key = flume.avro.schema.url
tier1.sources.listener.interceptors.attach-schema.value = file:/etc/flume-ng/schemas/user.avsc

# morphline interceptor config
tier1.sources.listener.interceptors.morphline.type = org.apache.flume.sink.solr.morphline.MorphlineInterceptor$Builder
tier1.sources.listener.interceptors.morphline.morphlineFile = /etc/flume-ng/conf/morphline.conf
tier1.sources.listener.interceptors.morphline.morphlineId = convertJsonToAvro
```

The flume part is straight-forward: build an interceptor object included in
morphlines, and tell it where the morphlines configuration lives. The
`morphlineId` identifies which morphline in the configuration file to use and
is optional if there is only one.

Lastly, configure the HDFS sink to write to the Dataset.

__HDFS sink config__:

```
# store the users in the users Dataset
tier1.sinks.user-dataset.type = hdfs
tier1.sinks.user-dataset.channel = mem-channel
tier1.sinks.user-dataset.hdfs.path = /tmp/data/default/users
tier1.sinks.user-dataset.hdfs.batchSize = 10
tier1.sinks.user-dataset.hdfs.fileType = DataStream
tier1.sinks.user-dataset.hdfs.proxyUser = cloudera
tier1.sinks.user-dataset.serializer = org.apache.flume.sink.hdfs.AvroEventSerializer$Builder
```

To configure flume, copy the `flume.properties` file as the flume agent's
configuration and restart the flume Agent.

## Testing

The flume source set in this example's configuration is a `netcat` source,
which listens on a port and creates an event with each line it receives, as the
event body. This is a simple way to test our transformation process: type JSON
records and see them appear as avro.

__Flume source config__:

```
tier1.sources.listener.type = netcat
tier1.sources.listener.channels = mem-channel
tier1.sources.listener.bind = 0.0.0.0
tier1.sources.listener.port = 41415
```

Once the flume agent is running, you can send events by connecting with netcat,
`nc`, and typing in JSON records. `CTRL+D` will close the connection.

```
blue@work:~$ nc -v localhost 41415
Connection to localhost 41415 port [tcp/*] succeeded!
{"username": "blue", "color": "green"}
OK
{"username": "tom", "color": "red"}
OK
^D
```

After sending a couple of records and waiting for flume to complete writing the
files, you should see them under `/tmp/data/default/users/`. We can also take a look at
the records using impala:

```
blue@work:~$ impala-shell
Starting Impala Shell in unsecure mode
Connected to localhost.localdomain:21000
Server version: impalad version 1.1.1 RELEASE (build 83d5868f005966883a918a819a449f636a5b3d5f)
Welcome to the Impala shell. Press TAB twice to see a list of available commands.

Copyright (c) 2012 Cloudera, Inc. All rights reserved.

(Shell build version: Impala Shell v1.1.1 (83d5868) built on Fri Aug 23 17:28:05 PDT 2013)
[localhost.localdomain:21000] > invalidate metadata;
Query: invalidate metadata
Query finished, fetching results ...

Returned 0 row(s) in 5.25s
[localhost.localdomain:21000] > show tables;
Query: show tables
Query finished, fetching results ...
+-------+
| name  |
+-------+
| users |
+-------+
Returned 1 row(s) in 0.11s
[localhost.localdomain:21000] > select * from users;
Query: select * from users
Query finished, fetching results ...
+----------+---------------+---------------+
| username | timestamp     | favoritecolor |
+----------+---------------+---------------+
| blue     | 1389403388593 | green         |
| tom      | 1389403395128 | red           |
+----------+---------------+---------------+
Returned 2 row(s) in 0.50s
[localhost.localdomain:21000] > select count(favoriteColor) from users group by favoriteColor;
Query: select count(favoriteColor) from users group by favoriteColor
Query finished, fetching results ...
+----------------------+
| count(favoritecolor) |
+----------------------+
| 1                    |
| 1                    |
+----------------------+
Returned 2 row(s) in 0.31s
[localhost.localdomain:21000] > select favoriteColor, count(favoriteColor) from users group by favoriteColor;
Query: select favoriteColor, count(favoriteColor) from users group by favoriteColor
Query finished, fetching results ...
+---------------+----------------------+
| favoritecolor | count(favoritecolor) |
+---------------+----------------------+
| green         | 1                    |
| red           | 1                    |
+---------------+----------------------+
Returned 2 row(s) in 0.31s
```

## Delete the dataset

We can now delete the dataset using the maven plugin:
```bash
mvn kite:delete-dataset \
  -Dkite.rootDirectory=/tmp/data \
  -Dkite.datasetName=users
```
