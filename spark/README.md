# Kite Java Spark Demo

This module provides an example of processing event data using Apache Spark.

## Getting started

This example assumes that you're running a CDH5.1 or later cluster (such as the
[Cloudera Quickstart VM][getvm]) that has Spark configured.

[getvm]: http://www.cloudera.com/content/support/en/downloads/quickstart_vms.html

On the cluster, check out a copy of the code:

```bash
git clone https://github.com/kite-sdk/kite-examples.git
cd kite-examples
cd spark
```

## Building

To build the project, type

```bash
mvn install
```

## Running

### Create and populate the events dataset

First we need to create and populate the `events` dataset.

We store the raw events in a Hive-backed dataset so you can also process the data
using Impala or Hive. We'll use a tool provided with the demo to both create and
populate the random events:

```bash
mvn exec:java -Dexec.mainClass="org.kitesdk.examples.spark.CreateEvents"
```

You can browse the generated events using [Hue on the QuickstartVM](http://localhost:8888/metastore/table/default/events/read).

### Use Spark to correlate events

Now we want to use Spark to correlate events from the same IP address within a
five minute window. Before we implement our algorithm, we need to configure Spark.
In particular, we need to set up Spark to use the Kryo serialization library and
configure Kryo to automatically serialize our Avro objects.

```java
// Create our Spark configuration and get a Java context
SparkConf sparkConf = new SparkConf()
    .setAppName("Correlate Events")
    // Configure the use of Kryo serialization including our Avro registrator
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryo.registrator", "org.kitesdk.examples.spark.AvroKyroRegistrator");
JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
```

We can register our Avro classes with a small bit of Scala code:

```scala
class AvroKyroRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[StandardEvent], AvroSerializer.SpecificRecordBinarySerializer[StandardEvent])
    kryo.register(classOf[CorrelatedEvents], AvroSerializer.SpecificRecordBinarySerializer[CorrelatedEvents])
  }
}
```

This will register the use of Avro's specific binary serialization for bot the
`StandardEvent` and `CorrelatedEvents` classes.

In order to access our Hive-backed datasets from remote Spark tasks, we need to
register some JARs in Spark's equivalent of the Hadoop DistributedCache:

```java
// Register some classes that will be needed in remote Spark tasks
addJarFromClass(sparkContext, getClass());
addJars(sparkContext, System.getenv("HIVE_HOME"), "lib");
sparkContext.addFile(System.getenv("HIVE_HOME")+"/conf/hive-site.xml");
```

Now we're ready to read from the events dataset by configuring the MapReduce
`DatasetKeyInputFormat` and then using Spark's built-in support to generate an
RDD form an `InputFormat`.

```java
Configuration conf = new Configuration();
DatasetKeyInputFormat.configure(conf).readFrom(eventsUri).withType(StandardEvent.class);

JavaPairRDD<StandardEvent, Void> events = sparkContext.newAPIHadoopRDD(conf,
    DatasetKeyInputFormat.class, StandardEvent.class, Void.class);
```

We can now process the events as needed. Once we have our finall RDD, we can
configure `DatasetKeyOutputFormat` in the same way and use the
`saveAsNewAPIHadoopFile` method to persist the data to our output dataset.

```java
DatasetKeyOutputFormat.configure(conf).writeTo(correlatedEventsUri).withType(CorrelatedEvents.class);

matches.saveAsNewAPIHadoopFile("dummy", CorrelatedEvents.class, Void.class,
    DatasetKeyOutputFormat.class, conf);
```

You can run the example Spark job by executing the following:

```bash
spark-submit --class org.kitesdk.examples.spark.CorrelateEvents --jars $(mvn dependency:build-classpath | grep -v '^\[' | sed -e 's/:/,/g') target/kite-spark-demo-*.jar
```

You can browse the correlated events using [Hue on the QuickstartVM](http://localhost:8888/metastore/table/default/correlated_events/read).

