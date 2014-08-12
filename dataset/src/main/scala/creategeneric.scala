/**
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.kitesdk.data.{DatasetDescriptor, DatasetWriter, Datasets, View}
import java.io.FileInputStream
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import scala.compat.Platform
import scala.util.Random

// Read an Avro schema from the user.avsc file on the classpath
val schema = new Parser().parse(new FileInputStream("src/main/resources/user.avsc"))

// Create a dataset of users with the Avro schema
val descriptor = new DatasetDescriptor.Builder().schema(schema).build()
val users = Datasets.create("dataset:hdfs:/tmp/data/users", descriptor).asInstanceOf[View[GenericRecord]]

// Get a writer for the dataset and write some users to it
val writer = users.newWriter().asInstanceOf[DatasetWriter[GenericRecord]]
val colors = Array("green", "blue", "pink", "brown", "yellow")
val rand = new Random()
for (i <- 0 to 100) {
  val builder = new GenericRecordBuilder(schema)
  val record = builder.set("username", "user-" + i)
    .set("creationDate", Platform.currentTime)
    .set("favoriteColor", colors(rand.nextInt(colors.length))).build()
  writer.write(record)
}
writer.close()
