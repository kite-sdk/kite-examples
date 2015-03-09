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
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.event.StandardEvent;

public class GenerateEvents extends Configured implements Tool {
  protected Random random;
  protected long baseTimestamp;
  protected long counter;

  public GenerateEvents() {
    random = new Random();
  }
  @Override
  public int run(String[] args) throws Exception {
    long counter = 0l;
    baseTimestamp = System.currentTimeMillis();  

    View<StandardEvent> events = Datasets.load(
        (args.length==1 ? args[0] : "dataset:hive:events"), StandardEvent.class);

    DatasetWriter<StandardEvent> writer = events.newWriter();
    try {
      Utf8 sessionId = new Utf8("sessionId");
      long userId = 0;
      Utf8 ip = new Utf8("ip");
      int randomEventCount = 0;

      while (System.currentTimeMillis() - baseTimestamp < 36000) {
          sessionId = randomSessionId();
          userId = randomUserId();
          ip = randomIp();
          randomEventCount = random.nextInt(25);
          for (int i=0; i < randomEventCount; i++) {
            writer.write(generateRandomEvent(sessionId, userId, ip, counter++));
          }
      }
    } finally {
      writer.close();
    }

    System.out.println("Generated " + counter + " events");

    return 0;
  }

  public StandardEvent generateRandomEvent(Utf8 sessionId, long userId, Utf8 ip, long counter) {
    return StandardEvent.newBuilder()
        .setEventInitiator(new Utf8("client_user"))
        .setEventName(randomEventName(counter))
        .setUserId(userId)
        .setSessionId(sessionId)
        .setIp(ip)
        .setTimestamp(randomTimestamp())
        .build();
  }

  public Utf8 randomEventName(long counter) {
    return new Utf8("event"+counter);
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
    delta = delta*1000l+random.nextInt(5000);
    return baseTimestamp+delta;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new Configuration(), new GenerateEvents(), args);

    System.exit(rc);
  }

}
