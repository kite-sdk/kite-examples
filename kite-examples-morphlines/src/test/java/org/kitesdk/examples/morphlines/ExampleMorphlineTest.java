/*
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
package org.kitesdk.examples.morphlines;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.kitesdk.morphline.api.AbstractMorphlineTest;
import org.kitesdk.morphline.api.Record;
import org.kitesdk.morphline.base.Fields;
import org.kitesdk.morphline.base.Notifications;

import com.google.common.io.Files;

public class ExampleMorphlineTest extends AbstractMorphlineTest {

  @Test
  public void testAddValues() throws Exception {
    morphline = createMorphline("test-morphlines/addValues");    
    Record record = new Record();
    record.put("first_name", "Nadja");
    Record expected = new Record();
    expected.put("first_name", "Nadja");
    expected.put("source_type", "text/log");
    expected.put("source_type", "text/log2");
    expected.put("source_host", "123");
    expected.put("name", "Nadja");
    expected.put("names", "@{first_name}");
    expected.put("pids", 456);
    expected.put("pids", "hello");
    processAndVerifySuccess(record, expected);
  }

  @Test
  public void testGrokSyslogNgCisco() throws Exception {
    morphline = createMorphline("test-morphlines/grokSyslogNgCisco");
    Record record = new Record();
    String msg = "<179>Jun 10 04:42:51 www.foo.com Jun 10 2013 04:42:51 : %myproduct-3-mysubfacility-251010: " +
        "Health probe failed for server 1.2.3.4 on port 8083, connection refused by server";
    record.put(Fields.MESSAGE, msg);
    assertTrue(morphline.process(record));
    Record expected = new Record();
    expected.put(Fields.MESSAGE, msg);
    expected.put("cisco_message_code", "%myproduct-3-mysubfacility-251010");
    expected.put("cisco_product", "myproduct");
    expected.put("cisco_level", "3");
    expected.put("cisco_subfacility", "mysubfacility");
    expected.put("cisco_message_id", "251010");
    expected.put("syslog_message", "%myproduct-3-mysubfacility-251010: Health probe failed for server 1.2.3.4 " +
        "on port 8083, connection refused by server");
    assertEquals(expected, collector.getFirstRecord());
    assertNotSame(record, collector.getFirstRecord());      
  }
  
  public void testGrokSyslogNgCiscoWithoutSubFacility() throws Exception {
    morphline = createMorphline("test-morphlines/grokSyslogNgCisco");
    Record record = new Record();
    String msg = "<179>Jun 10 04:42:51 www.foo.com Jun 10 2013 04:42:51 : %myproduct-3-mysubfacility-251010: " +
        "Health probe failed for server 1.2.3.4 on port 8083, connection refused by server";
    record.put(Fields.MESSAGE, msg);
    assertTrue(morphline.process(record));
    Record expected = new Record();
    expected.put(Fields.MESSAGE, msg);
    expected.put("cisco_message_code", "%myproduct-3-251010");
    expected.put("cisco_product", "myproduct");
    expected.put("cisco_level", "3");
//    expected.put("cisco_subfacility", "mysubfacility");
    expected.put("cisco_message_id", "251010");
    expected.put("syslog_message", "%myproduct-3-mysubfacility-251010: Health probe failed for server 1.2.3.4 " +
        "on port 8083, connection refused by server");
    assertEquals(expected, collector.getFirstRecord());
    assertNotSame(record, collector.getFirstRecord());      
  }
  
  @Test
  public void testGrokEmail() throws Exception {
    morphline = createMorphline("test-morphlines/grokEmail");
    Record record = new Record();
    byte[] bytes = Files.toByteArray(new File(RESOURCES_DIR + "/test-documents/email.txt"));
    record.put(Fields.ATTACHMENT_BODY, bytes);
    assertTrue(morphline.process(record));
    Record expected = new Record();
    String msg = new String(bytes, "UTF-8"); //.replaceAll("(\r)?\n", "\n");
    expected.put(Fields.MESSAGE, msg);
    expected.put("message_id", "12345.6789.JavaMail.foo@bar");
    expected.put("date", "Wed, 6 Feb 2012 06:06:05 -0800");
    expected.put("from", "foo@bar.com");
    expected.put("to", "baz@bazoo.com");
    expected.put("subject", "WEDNESDAY WEATHER HEADLINES");
    expected.put("from_names", "Foo Bar <foo@bar.com>@xxx");
    expected.put("to_names", "'Weather News Distribution' <wfoo@bar.com>");    
    expected.put("text", 
        "Average 1 to 3- degrees above normal: Mid-Atlantic, Southern Plains.." +
        "\nAverage 4 to 6-degrees above normal: Ohio Valley, Rockies, Central Plains");
    assertEquals(expected, collector.getFirstRecord());
    assertNotSame(record, collector.getFirstRecord());      
  }
  
  @Test
  public void testExtractJsonPathsFlattened() throws Exception {
    morphline = createMorphline("test-morphlines/extractJsonPathsFlattened");    
    File file = new File(RESOURCES_DIR + "/test-documents/arrays.json");
    InputStream in = new BufferedInputStream(new FileInputStream(file));
    Record record = new Record();
    record.put(Fields.ATTACHMENT_BODY, in);
    
    startSession();
    assertEquals(1, collector.getNumStartEvents());
    assertTrue(morphline.process(record));    
    
    assertEquals(1, collector.getRecords().size());
    List expected = Arrays.asList(1, 2, 3, 4, 5, 10, 20, 100, 200);
    assertEquals(1, collector.getRecords().size());
    assertEquals(expected, collector.getFirstRecord().get("/price"));
    assertEquals(expected, collector.getFirstRecord().get("/price/[]"));
    assertEquals(Arrays.asList(), collector.getFirstRecord().get("/unknownField"));

    in.close();
  }

  @Test
  public void testSimpleCSV() throws Exception {
    morphline = createMorphline("test-morphlines/simpleCSV");

    Notifications.notifyBeginTransaction(morphline);

    InputStream in = new FileInputStream(new File(RESOURCES_DIR + "/test-documents/simpleCSV.txt"));
    Record record = new Record();
    record.put(Fields.ATTACHMENT_BODY, in);
    record.put(Fields.ATTACHMENT_MIME_TYPE, "text/plain");

    // Actually process the input file.
    assertTrue(morphline.process(record));

    assertEquals(collector.getRecords().size(), 2);
    Record rec = collector.getRecords().get(0);

    // Since id and timestamp vary with run, just see if they have anything in them
    assertTrue(rec.get("id").toString().length() > 5);
    assertTrue(rec.get("timestamp").toString().length() > 5);
    assertEquals(rec.get("text").toString(), "[text for body]");

    // Now look at second record
    rec = collector.getRecords().get(1);

    assertTrue(rec.get("id").toString().length() > 5);
    assertTrue(rec.get("timestamp").toString().length() > 5);
    assertEquals(rec.get("text").toString(), "[second record]");

    in.close();
    Notifications.notifyCommitTransaction(morphline);
  }
  
  @Test
  public void testMyLowerCase() throws Exception {
    morphline = createMorphline("test-morphlines/myToLowerCase");    
    Record record = new Record();
    record.put("message", "Hello");
    Record expected = new Record();
    expected.put("message", "olleh");
    processAndVerifySuccess(record, expected);
  }
  
  @Test
  @Ignore
  /** Crude quick n' dirty benchmark */
  // Before running this disable debug logging 
  // via log4j.logger.org.kitesdk.morphline=INFO in log4j.properties
  public void benchmarkJson() throws Exception {
    String morphlineConfigFile = "test-morphlines/readJson";
    long durationSecs = 20;
    //File file = new File(RESOURCES_DIR + "/test-documents/stream.json");
    File file = new File(RESOURCES_DIR + "/test-documents/sample-statuses-20120906-141433.json");
    System.out.println("Now benchmarking " + morphlineConfigFile + " ...");
    morphline = createMorphline(morphlineConfigFile);    
    byte[] bytes = Files.toByteArray(file);
    long start = System.currentTimeMillis();
    long duration = durationSecs * 1000;
    int iters = 0; 
    while (System.currentTimeMillis() < start + duration) {
      Record record = new Record();
      record.put(Fields.ATTACHMENT_BODY, bytes);      
      collector.reset();
      assertTrue(morphline.process(record));    
      iters++;
    }
    float secs = (System.currentTimeMillis() - start) / 1000.0f;
    System.out.println("Results: iters=" + iters + ", took[secs]=" + secs + ", iters/secs=" + (iters/secs));
  }  

  private void processAndVerifySuccess(Record input, Record expected) {
    processAndVerifySuccess(input, expected, true);
  }

  private void processAndVerifySuccess(Record input, Record expected, boolean isSame) {
    collector.reset();
    startSession();
    assertEquals(1, collector.getNumStartEvents());
    assertTrue(morphline.process(input));
    assertEquals(expected, collector.getFirstRecord());
    if (isSame) {
      assertSame(input, collector.getFirstRecord());    
    } else {
      assertNotSame(input, collector.getFirstRecord());    
    }
  }

}
