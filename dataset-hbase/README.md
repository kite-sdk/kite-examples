# CDK HBase Example

This example shows basic usage of the CDK Data API for working with a dataset stored in
 HBase.

## Pre-requisites

Make sure ports 2181 (ZooKeeper) and 60000, 60020 (HBase Master and Region Server) are
enabled for port forwarding, as described on the
[CDK Examples page](https://github.com/cloudera/cdk-examples).

## Running

Build the code with:

```bash
mvn compile
```

Then create the dataset with:

```bash
mvn cdk:create-dataset
```

Write some data to it:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.WriteUserDataset"
```

Once we have created a dataset and written some data to it, the next thing to do is to
read it back. We can do this with the `ReadUserDataset` program.

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.cdk.examples.data.ReadUserDataset"
```

You can also have a look at the raw data in HBase by typing `hbase shell` in a terminal
(e.g. on the QuickStart VM) followed by:

```bash
list # show the tables
scan 'users' # dump all rows and columns
```

Finally, delete the dataset:

```bash
mvn cdk:delete-dataset
```
