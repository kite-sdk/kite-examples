package com.cloudera.cdk.examples.demo;

import com.cloudera.cdk.examples.demo.event.Session;
import com.cloudera.data.DatasetRepository;
import com.cloudera.data.event.StandardEvent;
import com.cloudera.data.filesystem.CrunchDatasets;
import com.cloudera.data.filesystem.FileSystemDatasetRepository;
import com.cloudera.data.hcatalog.HCatalogDatasetRepository;
import java.io.Serializable;
import java.net.URI;
import org.apache.crunch.CombineFn;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.Pair;
import org.apache.crunch.Target;
import org.apache.crunch.types.avro.Avros;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;

public class CreateSessions extends CrunchTool implements Serializable {

  @Override
  public int run(String[] args) throws Exception {

    // Construct a local filesystem dataset repository rooted at /tmp/data
    DatasetRepository fsRepo = new FileSystemDatasetRepository.Builder()
        .rootDirectory(new URI("/tmp/data")).get();

    // Construct an HCatalog dataset repository using external Hive tables
    DatasetRepository hcatRepo = new HCatalogDatasetRepository(
        FileSystem.get(new Configuration()), new Path("/tmp/data"));

    // Turn debug on while in development.
    getPipeline().enableDebug();
    getPipeline().getConfiguration().set("crunch.log.job.progress", "true");

    PCollection<StandardEvent> events = read(
        CrunchDatasets.asSource(fsRepo.get("events"), StandardEvent.class));

    PCollection<Session> sessions = events
      .parallelDo(new DoFn<StandardEvent, Session>() {
        @Override
        public void process(StandardEvent event, Emitter<Session> emitter) {
          emitter.emit(Session.newBuilder()
              .setUserId(event.getUserId())
              .setSessionId(event.getSessionId())
              .setIp(event.getIp())
              .setStartTimestamp(event.getTimestamp())
              .setDuration(0)
              .setSessionEventCount(1)
              .build());
        }
      }, Avros.specifics(Session.class))
      .by(new MapFn<Session, Pair<Long, String>>() {
        @Override
        public Pair<Long, String> map(Session session) {
          return Pair.of(session.getUserId(), session.getSessionId());
        }
      }, Avros.pairs(Avros.longs(), Avros.strings()))
      .groupByKey()
      .combineValues(new CombineFn<Pair<Long, String>, Session>() {
        @Override
        public void process(Pair<Pair<Long, String>, Iterable<Session>> pairIterable,
            Emitter<Pair<Pair<Long, String>, Session>> emitter) {
          String ip = null;
          long startTimestamp = Long.MAX_VALUE;
          long endTimestamp = Long.MIN_VALUE;
          int sessionEventCount = 0;
          for (Session s : pairIterable.second()) {
            ip = s.getIp();
            startTimestamp = Math.min(startTimestamp, s.getStartTimestamp());
            endTimestamp = Math.max(endTimestamp, s.getStartTimestamp() + s.getDuration());
            sessionEventCount += s.getSessionEventCount();
          }
          emitter.emit(Pair.of(pairIterable.first(), Session.newBuilder()
              .setUserId(pairIterable.first().first())
              .setSessionId(pairIterable.first().second())
              .setIp(ip)
              .setStartTimestamp(startTimestamp)
              .setDuration(endTimestamp - startTimestamp)
              .setSessionEventCount(sessionEventCount)
              .build()));
        }
      })
      .parallelDo(new DoFn<Pair<Pair<Long, String>, Session>, Session>() {
        @Override
        public void process(Pair<Pair<Long, String>, Session> pairSession,
            Emitter<Session> emitter) {
          emitter.emit(pairSession.second());
        }
      }, Avros.specifics(Session.class));

    getPipeline().write(sessions, CrunchDatasets.asTarget(hcatRepo.get("sessions")),
        Target.WriteMode.APPEND);

    return run().succeeded() ? 0 : 1;
  }

  public static void main(String... args) throws Exception {
    int rc = ToolRunner.run(new CreateSessions(), args);
    System.exit(rc);
  }

}
