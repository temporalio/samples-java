package io.temporal.samples.lambdaworker;

/** Shared configuration for the Lambda Worker sample. */
public final class LambdaWorkerSample {

  public static final String TASK_QUEUE_ENV = "TEMPORAL_TASK_QUEUE";
  public static final String DEPLOYMENT_NAME_ENV = "TEMPORAL_LAMBDA_DEPLOYMENT_NAME";
  public static final String BUILD_ID_ENV = "TEMPORAL_LAMBDA_BUILD_ID";

  public static final String DEFAULT_TASK_QUEUE = "serverless-task-queue-java";
  public static final String DEFAULT_DEPLOYMENT_NAME = "my-app";
  public static final String DEFAULT_BUILD_ID = "build-1";

  public static String taskQueue() {
    return envOrDefault(TASK_QUEUE_ENV, DEFAULT_TASK_QUEUE);
  }

  public static String deploymentName() {
    return envOrDefault(DEPLOYMENT_NAME_ENV, DEFAULT_DEPLOYMENT_NAME);
  }

  public static String buildId() {
    return envOrDefault(BUILD_ID_ENV, DEFAULT_BUILD_ID);
  }

  private static String envOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private LambdaWorkerSample() {}
}
