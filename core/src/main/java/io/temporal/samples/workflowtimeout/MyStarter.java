package io.temporal.samples.workflowtimeout;

import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class MyStarter {

  static final String WORKFLOW_ID = "wid";

  public static void main(String[] args) throws Exception {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(MyWorker.TASK_QUEUE)
            .setWorkflowId(WORKFLOW_ID)
            .setWorkflowIdConflictPolicy(
                WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_TERMINATE_EXISTING)
            .setWorkflowExecutionTimeout(java.time.Duration.ofSeconds(2))
            .build();
    MyWorkflow workflow = client.newWorkflowStub(MyWorkflow.class, workflowOptions);

    WorkflowClient.start(workflow::run);

    int upResult = workflow.myUpdate();
    System.out.println("upResult: " + upResult);

    int wfResult = workflow.run();
    System.out.println("wfResult: " + wfResult);

    System.exit(0);
  }
}
