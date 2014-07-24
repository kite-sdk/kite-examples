/*
 * Copyright 2014 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.examples.spark.commands;

import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.event.StandardEvent;
import org.slf4j.Logger;

@Parameters(commandDescription = "Create the events dataset")
public class CreateEventsCommand extends BaseEventsCommand {

  protected Random random;
  protected long baseTimestamp;
  protected long counter;

  public CreateEventsCommand(Logger console) {
    super(console);
    random = new Random();
    baseTimestamp = System.currentTimeMillis();
    counter = 0l;
  }

  @Override
  public int run() throws IOException {
    Preconditions.checkState(!Datasets.exists(createUri()),
        "events dataset already exists");

    DatasetDescriptor.Builder descriptorBuilder = new DatasetDescriptor.Builder();

    descriptorBuilder.format("avro");
    descriptorBuilder.schema(StandardEvent.class);

    View<StandardEvent> events = Datasets.create(createUri(), descriptorBuilder.build(), StandardEvent.class);
    DatasetWriter<StandardEvent> writer = events.newWriter();
    try {
      while (System.currentTimeMillis() - baseTimestamp < 36000) {
        writer.write(generateRandomEvent());
      }
    } finally {
      writer.close();
    }

    console.info("Generated " + counter + " events");

    return 0;
  }

  public StandardEvent generateRandomEvent() {
    return StandardEvent.newBuilder()
        .setEventInitiator("client_user")
        .setEventName(randomEventName())
        .setUserId(randomUserId())
        .setSessionId(randomSessionId())
        .setIp(randomIp())
        .setTimestamp(randomTimestamp())
        .setEventDetails(randomEventDetails())
        .build();
  }

  public String randomEventName() {
    return "event"+counter++;
  }

  public long randomUserId() {
    return random.nextInt(10);
  }

  public String randomSessionId() {
    return UUID.randomUUID().toString();
  }

  public String randomIp() {
    return "192.168." + (random.nextInt(254) + 1) + "."
        + (random.nextInt(254) + 1);
  }

  public long randomTimestamp() {
    long delta = System.currentTimeMillis() - baseTimestamp;
    // Each millisecond elapsed will elapse 100 milliseconds
    // this is the euivalent of each second being 1.67 minutes
    delta = delta*100l;
    return baseTimestamp+delta;
  }

  public Map<String, Object> randomEventDetails() {
    Map<String, Object> details = new HashMap<String, Object>();
    String type = random.nextInt(1500) < 1 ? "alert" : "click";
    details.put("type", type);

    return details;
  }

  @Override
  public List<String> getExamples() {
    return Lists.newArrayList(
        "# Create the events dataset in HDFS:",
        "--use-hdfs",
        "# Create the events dataset in Hive:",
        "--use-hive"
    );
  }

}
