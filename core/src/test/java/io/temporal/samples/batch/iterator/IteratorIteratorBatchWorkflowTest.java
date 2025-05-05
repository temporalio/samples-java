

package io.temporal.samples.batch.iterator;

import static io.temporal.samples.batch.iterator.RecordLoaderImpl.PAGE_COUNT;
import static org.junit.Assert.assertTrue;

import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.Workflow;
import org.junit.Rule;
import org.junit.Test;

public class IteratorIteratorBatchWorkflowTest {

  private static final int PAGE_SIZE = 10;

  /** The sample RecordLoaderImpl always returns the fixed number pages. */
  private static boolean[] processedRecords = new boolean[PAGE_SIZE * PAGE_COUNT];

  public static class TestRecordProcessorWorkflowImpl implements RecordProcessorWorkflow {

    @Override
    public void processRecord(SingleRecord r) {
      Workflow.sleep(5000);
      processedRecords[r.getId()] = true;
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(IteratorBatchWorkflowImpl.class, TestRecordProcessorWorkflowImpl.class)
          .setActivityImplementations(new RecordLoaderImpl())
          .build();

  @Test
  public void testBatchWorkflow() {
    IteratorBatchWorkflow workflow = testWorkflowRule.newWorkflowStub(IteratorBatchWorkflow.class);
    workflow.processBatch(PAGE_SIZE, 0);

    for (int i = 0; i < processedRecords.length; i++) {
      assertTrue(processedRecords[i]);
    }
  }
}
