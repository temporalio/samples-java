

package io.temporal.samples.batch.slidingwindow;

import static org.junit.Assert.assertTrue;

import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import org.junit.Rule;
import org.junit.Test;

public class SlidingWindowBatchWorkflowTest {

  private static final int RECORD_COUNT = 15;
  private static boolean[] processedRecords = new boolean[RECORD_COUNT];

  public static class TestRecordProcessorWorkflowImpl implements RecordProcessorWorkflow {

    @Override
    public void processRecord(SingleRecord r) {
      processedRecords[r.getId()] = true;
      WorkflowInfo info = Workflow.getInfo();
      String parentId = info.getParentWorkflowId().get();
      SlidingWindowBatchWorkflow parent =
          Workflow.newExternalWorkflowStub(SlidingWindowBatchWorkflow.class, parentId);
      Workflow.sleep(500);
      // Notify parent about record processing completion
      // Needs to retry due to a continue-as-new atomicity
      // bug in the testservice:
      // https://github.com/temporalio/sdk-java/issues/1538
      while (true) {
        try {
          parent.reportCompletion(r.getId());
          break;
        } catch (Exception e) {
          continue;
        }
      }
    }
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(
              SlidingWindowBatchWorkflowImpl.class, TestRecordProcessorWorkflowImpl.class)
          .setActivityImplementations(new RecordLoaderImpl())
          .build();

  @Test
  public void testSlidingWindowBatchWorkflow() {
    SlidingWindowBatchWorkflow workflow =
        testWorkflowRule.newWorkflowStub(SlidingWindowBatchWorkflow.class);

    ProcessBatchInput input = new ProcessBatchInput();
    input.setPageSize(3);
    input.setSlidingWindowSize(7);
    input.setOffset(0);
    input.setMaximumOffset(RECORD_COUNT);
    workflow.processBatch(input);
    for (int i = 0; i < RECORD_COUNT; i++) {
      assertTrue(processedRecords[i]);
    }
  }
}
