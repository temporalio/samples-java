package io.temporal.samples.common;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.IOException;
import java.util.Optional;

/**
 * Queries a workflow execution using the Temporal query API. Temporal redirects a query to any
 * currently running workflow worker for the workflow type of the requested workflow execution.
 *
 * @author fateev
 */
public class QueryWorkflowExecution {

  public static void main(String[] args) {
    if (args.length < 2 || args.length > 3) {
      System.err.println(
          "Usage: java "
              + QueryWorkflowExecution.class.getName()
              + " <queryType> <workflowId> [<runId>]");
      System.exit(1);
    }
    String queryType = args[0];
    String workflowId = args[1];
    String runId = args.length == 3 ? args[2] : "";

    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    // Load configuration from environment and files
    ClientConfigProfile profile;
    try {
      profile = ClientConfigProfile.load();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load client configuration", e);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(profile.toWorkflowServiceStubsOptions());

    WorkflowClient client = WorkflowClient.newInstance(service, profile.toWorkflowClientOptions());

    WorkflowExecution workflowExecution =
        WorkflowExecution.newBuilder().setWorkflowId(workflowId).setRunId(runId).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub(workflowExecution, Optional.empty());

    String result = workflow.query(queryType, String.class);

    System.out.println("Query result for " + workflowExecution + ":");
    System.out.println(result);
  }
}
