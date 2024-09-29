package io.temporal.samples.basic;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
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
            .setWorkflowIdReusePolicy(
                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING)
            .build();
    MyWorkflow workflow = client.newWorkflowStub(MyWorkflow.class, workflowOptions);

    WorkflowClient.start(workflow::run);

    int result = workflow.myUpdate();
    System.out.println(result);
    System.exit(0);
  }
}
