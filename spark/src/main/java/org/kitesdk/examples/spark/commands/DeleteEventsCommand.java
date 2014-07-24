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
import java.io.IOException;
import java.util.List;
import org.kitesdk.data.Datasets;

@Parameters(commandDescription = "Delete the events dataset")
public class DeleteEventsCommand extends BaseEventsCommand {

  public DeleteEventsCommand(org.slf4j.Logger console) {
    super(console);
  }

  @Override
  public int run() throws IOException {
    Preconditions.checkState(Datasets.exists(createUri()),
        "events dataset doesn't exists");

    Datasets.delete(createUri());

    return 0;
  }

  @Override
  public List<String> getExamples() {
    return null;
  }

}
