

package io.temporal.samples.batch.iterator;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Random;
import org.slf4j.Logger;

/** Fake RecordProcessorWorkflow implementation. */
public class RecordProcessorWorkflowImpl implements RecordProcessorWorkflow {
  public static final Logger log = Workflow.getLogger(RecordProcessorWorkflowImpl.class);
  private final Random random = Workflow.newRandom();

  @Override
  public void processRecord(SingleRecord r) {
    // Simulate some processing
    Workflow.sleep(Duration.ofSeconds(random.nextInt(30)));
    log.info("Processed " + r);
  }
}
