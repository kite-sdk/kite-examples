/*
 * Copyright 2015 Cloudera, Inc.
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

package org.kitesdk.examples.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.event.StandardEvent;

public class GenerateEvents extends BaseEventsTool {

  protected Random random;
  protected long baseTimestamp;
  protected long counter;

  public GenerateEvents() {
    random = new Random();
    baseTimestamp = System.currentTimeMillis();
    counter = 0l;
  }

  @Override
  public int run(List<String> args) throws Exception {

    View<StandardEvent> events = Datasets.load(
        "dataset:hive:events", StandardEvent.class);
    DatasetWriter<StandardEvent> writer = events.newWriter();
    try {
      while (System.currentTimeMillis() - baseTimestamp < 36000) {
        writer.write(generateRandomEvent());
      }
    } finally {
      writer.close();
    }

    System.out.println("Generated " + counter + " events");

    return 0;
  }

  public StandardEvent generateRandomEvent() {
    return StandardEvent.newBuilder()
        .setEventInitiator(new Utf8("client_user"))
        .setEventName(randomEventName())
        .setUserId(randomUserId())
        .setSessionId(randomSessionId())
        .setIp(randomIp())
        .setTimestamp(randomTimestamp())
        .setEventDetails(randomEventDetails())
        .build();
  }

  public Utf8 randomEventName() {
    return new Utf8("event"+counter++);
  }

  public long randomUserId() {
    return random.nextInt(10);
  }

  public Utf8 randomSessionId() {
    return new Utf8(UUID.randomUUID().toString());
  }

  public Utf8 randomIp() {
    return new Utf8("192.168." + (random.nextInt(254) + 1) + "."
        + (random.nextInt(254) + 1));
  }

  public long randomTimestamp() {
    long delta = System.currentTimeMillis() - baseTimestamp;
    // Each millisecond elapsed will elapse 100 milliseconds
    // this is the equivalent of each second being 1.67 minutes
    delta = delta*100l;
    return baseTimestamp+delta;
  }

  public Map<Utf8, Object> randomEventDetails() {
    Map<Utf8, Object> details = new HashMap<Utf8, Object>();
    String type = random.nextInt(1500) < 1 ? "alert" : "click";
    details.put(new Utf8("type"), type);

    return details;
  }

  public static void main(String[] args) throws Exception {
    int rc = ToolRunner.run(new Configuration(), new GenerateEvents(), args);

    System.exit(rc);
  }

}
