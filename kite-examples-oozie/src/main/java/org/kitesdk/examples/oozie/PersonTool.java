package org.kitesdk.examples.oozie;

import java.net.URI;
import java.util.StringTokenizer;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PipelineResult;
import org.apache.crunch.io.From;
import org.apache.crunch.types.avro.Avros;
import org.apache.crunch.util.CrunchTool;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.kitesdk.data.crunch.CrunchDatasets;

public class PersonTool extends CrunchTool {

  @Override
  public int run(String[] args) throws Exception {
    final Path inputPath = new Path(args[0]);
    final URI outputDatasetUri = new URI(args[1]);

    final PCollection<String> rawPersons = read(From.textFile(inputPath));

    final PCollection<Person> processedPersons = doSomeProcessing(rawPersons);

    write(processedPersons, CrunchDatasets.asTarget(outputDatasetUri));

    final PipelineResult result = run();
    if(result == null) {
        throw new RuntimeException("Result of the run was null!");
    } else if (!result.succeeded()) {
        throw new RuntimeException("Pipeline run failed!");
    }
    return 0;
  }

  private static class PersonProcessing extends MapFn<String, Person> {
    private static final long serialVersionUID = -4763189327890300396L;

    @Override
    public Person map(final String input) {
      final StringTokenizer tokenizer = new StringTokenizer(input, ",");
      final String id = tokenizer.nextToken();
      final String firstName = tokenizer.nextToken();
      final String lastName = tokenizer.nextToken();
      final int age = Integer.parseInt(tokenizer.nextToken());
      Person person = Person.newBuilder().setId(id).setFirstName(firstName)
          .setLastName(lastName).setAge(age).build();
      return person;
    }
  }

  private PCollection<Person> doSomeProcessing(final PCollection<String> rawPersons) {
    PersonProcessing mapFn = new PersonProcessing();
    return rawPersons.parallelDo(mapFn, Avros.records(Person.class));
  }

  public static void main(final String[] args) throws Exception {
      ToolRunner.run(new PersonTool(), args);
  }

}
