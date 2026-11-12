package io.temporal.samples.externalstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.gaul.s3proxy.S3Proxy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ExternalStorageTest {

  private static S3Proxy s3Mock;

  @BeforeClass
  public static void startS3Mock() throws Exception {
    s3Mock = S3Mock.start(0);
  }

  @AfterClass
  public static void stopS3Mock() throws Exception {
    s3Mock.stop();
  }

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(ProcessOrderBatchWorkflowImpl.class)
          .setActivityImplementations(new OrderActivitiesImpl())
          .setWorkflowClientOptions(
              StorageConfig.withExternalStorage(
                  WorkflowClientOptions.newBuilder().build(),
                  StorageConfig.mockS3Client(s3Mock.getPort()),
                  StorageConfig.MOCK_BUCKET))
          .build();

  @Test
  public void testOrderListIsOffloaded() {
    ProcessOrderBatchWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(
                ProcessOrderBatchWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

    WorkflowExecution execution = WorkflowClient.start(workflow::run, 200);
    BatchSummary summary = WorkflowStub.fromTyped(workflow).getResult(BatchSummary.class);
    assertEquals(200, summary.orderCount());
    assertEquals(5000.0, summary.totalWeightKg(), 0);

    // The sample only demonstrates anything if the order list is still above the threshold after
    // compression.
    Payload fetchOrdersResult =
        testWorkflowRule.getHistory(execution).getEventsList().stream()
            .filter(HistoryEvent::hasActivityTaskCompletedEventAttributes)
            .findFirst()
            .orElseThrow()
            .getActivityTaskCompletedEventAttributes()
            .getResult()
            .getPayloads(0);
    assertTrue(fetchOrdersResult.getExternalPayloadsCount() > 0);
  }
}
