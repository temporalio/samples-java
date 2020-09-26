package io.temporal.samples.dsl;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.dsl.models.Workflow;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DslStarter {
  private static final String TASK_QUEUE = "dsl";

  public static void main(String[] args) throws IOException {
    // Path resourceDirectory = Paths.get("src", "main", "resources", "dsl", "workflow1.yaml");
    Path resourceDirectory = Paths.get("src", "main", "resources", "dsl", "workflow2.yaml");

    String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    ObjectMapper objectMapper =
        new ObjectMapper(new YAMLFactory()).registerModule(new ParameterNamesModule());
    objectMapper.enable(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES);
    objectMapper.findAndRegisterModules();

    Workflow workflow = objectMapper.readValue(new File(absolutePath), Workflow.class);

    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);
    final Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(SimpleDSLWorkflowImpl.class);
    worker.registerActivitiesImplementations(
        new SampleActivities.SampleActivitiesImpl1(),
        new SampleActivities.SampleActivitiesImpl2(),
        new SampleActivities.SampleActivitiesImpl3(),
        new SampleActivities.SampleActivitiesImpl4(),
        new SampleActivities.SampleActivitiesImpl5());
    factory.start();

    SimpleDSLWorkflow interpreter =
        client.newWorkflowStub(
            SimpleDSLWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    interpreter.execute(workflow);
  }
}
