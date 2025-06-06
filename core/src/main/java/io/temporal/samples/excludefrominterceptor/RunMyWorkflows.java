package io.temporal.samples.excludefrominterceptor;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.excludefrominterceptor.activities.ForInterceptorActivitiesImpl;
import io.temporal.samples.excludefrominterceptor.activities.MyActivitiesImpl;
import io.temporal.samples.excludefrominterceptor.interceptor.MyWorkerInterceptor;
import io.temporal.samples.excludefrominterceptor.workflows.*;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class RunMyWorkflows {
  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactoryOptions wfo =
        WorkerFactoryOptions.newBuilder()
            // exclude MyWorkflowTwo from interceptor
            .setWorkerInterceptors(
                new MyWorkerInterceptor(
                    // exclude MyWorkflowTwo from workflow interceptors
                    Arrays.asList(MyWorkflowTwo.class.getSimpleName()),
                    // exclude ActivityTwo and the "ForInterceptor" activities from activity
                    // interceptor
                    // note with SpringBoot starter you could use bean names here, we use strings to
                    // not have
                    // to reflect on the activity impl class in sample
                    Arrays.asList(
                        "ActivityTwo", "ForInterceptorActivityOne", "ForInterceptorActivityTwo")))
            .validateAndBuildWithDefaults();

    WorkerFactory factory = WorkerFactory.newInstance(client, wfo);
    Worker worker = factory.newWorker("exclude-from-interceptor-queue");
    worker.registerWorkflowImplementationTypes(MyWorkflowOneImpl.class, MyWorkflowTwoImpl.class);
    worker.registerActivitiesImplementations(
        new MyActivitiesImpl(), new ForInterceptorActivitiesImpl());

    factory.start();

    MyWorkflow myWorkflow =
        client.newWorkflowStub(
            MyWorkflowOne.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("MyWorkflowOne")
                .setTaskQueue("exclude-from-interceptor-queue")
                .build());

    MyWorkflowTwo myWorkflowTwo =
        client.newWorkflowStub(
            MyWorkflowTwo.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("MyWorkflowTwo")
                .setTaskQueue("exclude-from-interceptor-queue")
                .build());

    WorkflowClient.start(myWorkflow::execute, "my workflow input");
    WorkflowClient.start(myWorkflowTwo::execute, "my workflow two input");

    // wait for both execs to complete
    try {
      CompletableFuture.allOf(
              WorkflowStub.fromTyped(myWorkflow).getResultAsync(String.class),
              WorkflowStub.fromTyped(myWorkflowTwo).getResultAsync(String.class))
          .get();
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }

    System.exit(0);
  }
}
