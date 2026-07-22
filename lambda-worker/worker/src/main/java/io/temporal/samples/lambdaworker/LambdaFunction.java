package io.temporal.samples.lambdaworker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.temporal.aws.lambda.LambdaWorker;
import io.temporal.aws.lambda.OtelLambdaWorkerConfigurationHelper;
import io.temporal.common.WorkerDeploymentVersion;

/** AWS Lambda entry point for the Temporal worker. */
public class LambdaFunction implements RequestHandler<Object, Void> {

  private static final RequestHandler<Object, Void> WORKER =
      LambdaWorker.define(
          new WorkerDeploymentVersion(
              LambdaWorkerSample.deploymentName(), LambdaWorkerSample.buildId()),
          builder -> {
            OtelLambdaWorkerConfigurationHelper.configure(builder);
            builder.setTaskQueue(LambdaWorkerSample.taskQueue());
            builder.registerWorkflowImplementationTypes(SampleWorkflowImpl.class);
            builder.registerActivitiesImplementations(new GreetingActivitiesImpl());
          });

  @Override
  public Void handleRequest(Object input, Context context) {
    return WORKER.handleRequest(input, context);
  }
}
