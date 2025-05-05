

package io.temporal.samples.batch.heartbeatingactivity;

import static io.temporal.samples.batch.heartbeatingactivity.RecordLoaderImpl.RECORD_COUNT;
import static org.junit.Assert.assertTrue;

import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class HeartbeatingActivityBatchWorkflowTest {
  private static boolean[] processedRecords = new boolean[RECORD_COUNT];

  public static class TestRecordProcessorImpl implements RecordProcessor {

    @Override
    public void processRecord(SingleRecord r) {
      processedRecords[r.getId()] = true;
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HeartbeatingActivityBatchWorkflowImpl.class)
          .setActivityImplementations(
              new RecordProcessorActivityImpl(
                  new RecordLoaderImpl(), new TestRecordProcessorImpl()))
          .build();

  @Test
  public void testBatchWorkflow() {
    HeartbeatingActivityBatchWorkflow workflow =
        testWorkflowRule.newWorkflowStub(HeartbeatingActivityBatchWorkflow.class);
    workflow.processBatch();

    for (int i = 0; i < processedRecords.length; i++) {
      assertTrue(processedRecords[i]);
    }
  }
}
