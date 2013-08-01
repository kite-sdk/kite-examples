# Cloudera Development Kit - Examples Module

The Examples Module is a collection of examples for the CDK.

## Example - Products Dataset

This example shows basic usage of the CDK Data API for performing streaming writes
to (and reads from) a dataset.

From the examples module, build with:

```bash
mvn compile
```

Then create the dataset with:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateProductDatasetPojo"
```

You can look at the files that were created in
[`/tmp/data/products`](http://localhost:8888/filebrowser/#/tmp/data/products).

__Note__: The above assumes that you are running against a single-node localhost HDFS installation.
If this is not the case, then you can change `fs.default.name` in
`src/main/resources/core-site.xml`, e.g. to `file:///` to use the local filesystem.
Alternatively, you can pass in extra arguments to the command, as follows:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateProductDatasetPojo" \
  -Dexec.args="-fs file:///"
```

For the rest of the examples we will assume a single-node localhost HDFS installation.

Once we have created a dataset and written some data to it, the next thing to do is to
read it back. We can do this with the `ReadProductDatasetPojo` program.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadProductDatasetPojo"
```

Finally, drop the dataset:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DropProductDataset"
```

### Generic records vs. POJOs

The previous examples used POJOs, since they are the most familiar data transfer
objects for most Java programmers. Avro supports generic records too,
which are more efficient, since they don't require reflection,
and also don't require either the reader or writer to have the POJO class available.

__Note__: It's currently not possible to write POJOs and then read them back as generic
objects since the return type will be the POJO class. So for the generic examples below
we model a dataset of users rather than products.

Run the following to use the generic writer and reader:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DropUserDataset"
```

### Partitioning

The API supports partitioning, so that records are written to different partition files
according to the value of particular partition fields.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateUserDatasetGenericPartitioned"
```

You can see how partitioning affects the data layout by looking at the subdirectories
created in [`/tmp/data/users`](http://localhost:8888/filebrowser/#/tmp/data/users).

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGenericOnePartition"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DropUserDataset"
```

### Parquet Columnar Format

Parquet is a new columnar format for data. Columnar formats provide performance
advantages over row-oriented formats like Avro data files (which is the default in CDK),
when the number of columns is large (typically dozens) and the typical queries that you perform
over the data only retrieve a small number of the columns.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateUserDatasetGenericParquet"
```

You can see the parquet file extension for files in
[`/tmp/data/users`](http://localhost:8888/filebrowser/#/tmp/data/users).

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DropUserDataset"
```

### HCatalog

So far all metadata has been stored in the _.metadata_ directory on the filesystem.
It's possible to store metadata in HCatalog so that other HCatalog-aware applications
like Hive can make use of it.

Run the following to create the dataset:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.CreateHCatalogUserDatasetGeneric"
```

Note: This example assumes a local (not embedded) metastore running on the local machine. You can
change the default to use a different metastore by editing _src/main/resources/hive-site.xml_.

Now inspect the dataset storage area in
[`/user/hive/warehouse/users`](http://localhost:8888/filebrowser/#/user/hive/warehouse/users).

Notice that there is no metadata stored there, since the metadata is stored in
[Hive/HCatalog's metastore](http://localhost:8888/metastore/tables/).

You can use SQL to query the data directly using the
[Hive UI (Beeswax)](http://localhost:8888/beeswax/) in Hue. For example:

```
select * from users
```

Alternatively, you can use the Java API to read the data:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadHCatalogUserDatasetGeneric"
```

Dropping the dataset deletes the metadata from the metastore and the data from the
filesystem:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DropHCatalogUserDataset"
```

## Scala

Run the equivalent example with:

```bash
scala -cp "$(mvn dependency:build-classpath | grep -v '^\[')" src/main/scala/createpojo.scala
```

Or for the generic example:

```bash
scala -cp "$(mvn dependency:build-classpath | grep -v '^\[')" src/main/scala/creategeneric.scala
```

The Java examples can be used to read (and drop) the dataset written from Scala:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDatasetGeneric"
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.DropUserDataset"
```
