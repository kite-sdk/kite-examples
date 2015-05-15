package org.kitesdk.examples.oozie;

import java.net.URI;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.types.avro.Avros;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.View;
import org.kitesdk.data.crunch.CrunchDatasets;

public class PersonSummaryTool extends CrunchTool {

  @Override
  public int run(String[] args) throws Exception {
    final URI inputDatasetUri = new URI(args[0]);
    final URI outputDatasetUri = new URI(args[1]);

    View<PersonOutcomes> inputDataset = Datasets.load(inputDatasetUri, PersonOutcomes.class);

    final PCollection<PersonOutcomes> personOutcomes = read(CrunchDatasets.asSource(inputDataset));

    final PCollection<PersonSummary> personSummaries = doSomeProcessing(personOutcomes);

    write(personSummaries, CrunchDatasets.asTarget(outputDatasetUri));

    final PipelineResult result = run();
    if(result == null) {
        throw new RuntimeException("Result of the run was null!");
    } else if (!result.succeeded()) {
        throw new RuntimeException("Pipeline run failed!");
    }
    return 0;
  }

  private PCollection<PersonSummary> doSomeProcessing(final PCollection<PersonOutcomes> personOutcomes) {
    // a real example would have some more interesting logic here
    return personOutcomes.parallelDo(
        new MapFn<PersonOutcomes, PersonSummary>() {

          @Override
          public PersonSummary map(final PersonOutcomes input) {
            return PersonSummary.newBuilder().setPersonId(input.getPersonId())
                .build();
          }
        }, Avros.records(PersonSummary.class));
  }


  public static void main(final String[] args) throws Exception {
      ToolRunner.run(new PersonSummaryTool(), args);
  }

}
