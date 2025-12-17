package io.temporal.samples.batch.iterator;

import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Random;
import org.slf4j.Logger;

/** Fake RecordProcessorWorkflow implementation. */
public class SingleListingMigrationWorkflowImpl implements SingleListingMigrationWorkflow {
  public static final Logger log = Workflow.getLogger(SingleListingMigrationWorkflowImpl.class);
  private final Random random = Workflow.newRandom();

  @Override
  public SingleResponse processRecord(SingleRecord r) {
    // Simulate some processing
    int result = random.nextInt(30) + r.getId();
    Workflow.sleep(Duration.ofSeconds(random.nextInt(5)));
    log.info("Processed {}, result={}", r, result);
    return new SingleResponse(r.getId(), result);
  }
}
