# Kite Java Spark Demo

This module provides an example of processing event data using Apache Spark.

## Getting started

This example assumes that you're running a CDH5.1 or later cluster (such as the [Cloudera Quickstart VM][getvm]) that has Spark configured. This example requires the `spark-submit` command to execute the Spark job on the cluster. If you're using the Quickstart VM, run this example from the VM rather than the host computer.

[getvm]: http://www.cloudera.com/content/support/en/downloads/quickstart_vms.html

On the cluster, check out a copy of the code and navigate to the `/spark` directory using the following commands in a terminal window.

```
git clone https://github.com/kite-sdk/kite-examples.git
cd kite-examples
cd spark
```

## Building the Application

To build the project, enter the following command in a terminal window.

```
mvn install
```

## Creating and Populating the Events Dataset

In this example, you store raw events in a Hive-backed dataset so that you can  process the results using Hive. Use `CreateEvents`, provided with the demo, to both create and populate random event records. Execute the following command from a terminal window in the `kite-examples/spark` directory.

```
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.spark.CreateEvents"
```

You can browse the generated events using [Hue on the QuickstartVM](http://localhost:8888/metastore/table/default/events/read).

## Using Spark to Correlate Events 

In this example, you use Spark to correlate events generated from the same IP address within a five-minute window. Begin by configuring Spark to use the Kryo serialization library.

Register your Avro classes with the following Scala class to use Avro's specific binary serialization for both the `StandardEvent` and `CorrelatedEvents` classes.

### AvroKyroRegistrator.scala

```scala
class AvroKyroRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[StandardEvent], AvroSerializer.SpecificRecordBinarySerializer[StandardEvent])
    kryo.register(classOf[CorrelatedEvents], AvroSerializer.SpecificRecordBinarySerializer[CorrelatedEvents])
  }
}
```

### Highlights from CorrelateEventsTask.class

The following snippets show examples of code you use to configure and invoke Spark tasks.

Configure Kryo to automatically serialize Avro objects.

```java
// Create the Spark configuration and get a Java context
SparkConf sparkConf = new SparkConf()
    .setAppName("Correlate Events")
    // Configure the use of Kryo serialization including the Avro registrator
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryo.registrator", "org.kitesdk.examples.spark.AvroKyroRegistrator");
JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
``

To access Hive-backed datasets from remote Spark tasks,
register JARs in the Spark equivalent of the Hadoop DistributedCache:

```java
// Register classes needed for remote Spark tasks
addJarFromClass(sparkContext, getClass());
addJars(sparkContext, System.getenv("HIVE_HOME"), "lib");
sparkContext.addFile(System.getenv("HIVE_HOME")+"/conf/hive-site.xml");
```

Configure the MapReduce `DatasetKeyInputFormat` to enable the application to read from the _events_ dataset. Use Spark built-in support to generate an
RDD (Resilient Distributed Dataset) from the input format.

```java
Configuration conf = new Configuration();
DatasetKeyInputFormat.configure(conf).readFrom(eventsUri).withType(StandardEvent.class);

JavaPairRDD<StandardEvent, Void> events = sparkContext.newAPIHadoopRDD(conf,
    DatasetKeyInputFormat.class, StandardEvent.class, Void.class);
```

The application can now process events as needed. Using your RDD, configure `DatasetKeyOutputFormat` the same way and use `saveAsNewAPIHadoopFile` to store data in an output dataset.

```java
DatasetKeyOutputFormat.configure(conf).writeTo(correlatedEventsUri).withType(CorrelatedEvents.class);

matches.saveAsNewAPIHadoopFile("dummy", CorrelatedEvents.class, Void.class,
    DatasetKeyOutputFormat.class, conf);
```

In a terminal window, run the Spark job using the following command.

```
spark-submit --class org.kitesdk.examples.spark.CorrelateEvents --jars $(mvn dependency:build-classpath | grep -v '^\[' | sed -e 's/:/,/g') target/kite-spark-demo-*.jar
```

You can browse the correlated events using [Hue on the QuickstartVM](http://localhost:8888/metastore/table/default/correlated_events/read).

## Deleting the datasets

When you're done, or if you want to run the example again, delete the datasets using the Kite CLI `delete` command.

```
curl http://central.maven.org/maven2/org/kitesdk/kite-tools/0.17.0/kite-tools-0.17.0-binary.jar -o kite-dataset
chmod +x kite-dataset
./kite-dataset delete events
./kite-dataset delete correlated_events
```

## Troubleshooting

The following are known issues and their solutions.

### ClassNotFoundException

The first time you execute `spark-submit`, the process might not find `CorrelateEvents`.

```
java.lang.ClassNotFoundException: org.kitesdk.examples.spark.CorrelateEvents
```

Execute the command a second time to get past this exception.

### AccessControlException

On some VMs, you might receive the following exception.

```
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.security.AccessControlException): \
Permission denied: user=cloudera, access=EXECUTE, inode="/user/spark":spark:spark:drwxr-x---
```

In a terminal window, update permissions using the following commands.

```
$ sudo su - hdfs
$ hadoop fs -chmod -R 777 /user/spark
```
