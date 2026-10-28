package io.temporal.samples.googleadk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.Gemini;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.googleadk.GoogleAdkPlugin;
import io.temporal.samples.googleadk.chat.ChatWorkflowImpl;
import io.temporal.samples.googleadk.humanintheloop.ApprovalWorkflowImpl;
import io.temporal.samples.googleadk.humanintheloop.OrderActivitiesImpl;
import io.temporal.samples.googleadk.longrunning.LongRunningWorkflowImpl;
import io.temporal.samples.googleadk.longrunning.ReportActivitiesImpl;
import io.temporal.samples.googleadk.mcp.EchoMcpServer;
import io.temporal.samples.googleadk.mcp.McpWorkflowImpl;
import io.temporal.samples.googleadk.multiagent.MultiAgentWorkflowImpl;
import io.temporal.samples.googleadk.structuredoutput.StructuredOutputWorkflowImpl;
import io.temporal.samples.googleadk.tools.ToolsWorkflowImpl;
import io.temporal.samples.googleadk.tools.WeatherActivitiesImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.util.function.Supplier;

public final class SampleWorker {
  private SampleWorker() {}

  public static void main(String[] args) {
    String mode = System.getenv().getOrDefault("ADK_MODEL_MODE", "live");
    if (!mode.equals("offline") && !mode.equals("live")) {
      throw new IllegalArgumentException("ADK_MODEL_MODE must be offline or live.");
    }
    // @@@SNIPSTART java-google-adk-worker
    Client modelClient =
        mode.equals("live")
            ? Client.builder()
                .httpOptions(
                    HttpOptions.builder()
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                        .build())
                .build()
            : null;
    GoogleAdkPlugin plugin =
        plugin(
            modelClient == null
                ? OfflineModel::new
                : () ->
                    Gemini.builder().modelName(AgentSupport.MODEL).apiClient(modelClient).build());
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setPlugins(plugin).build());
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(AgentSupport.TASK_QUEUE);
    register(worker);
    factory.start();
    // @@@SNIPEND
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  factory.shutdown();
                  factory.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
                  if (modelClient != null) {
                    modelClient.close();
                  }
                  service.shutdown();
                }));
    System.out.println("Google ADK Worker: " + mode + " model, queue " + AgentSupport.TASK_QUEUE);
  }

  // @@@SNIPSTART java-google-adk-worker-plugin
  public static GoogleAdkPlugin plugin(Supplier<BaseLlm> models) {
    return GoogleAdkPlugin.newBuilder()
        .model(AgentSupport.MODEL, models)
        .mcpToolset("echo", EchoMcpServer::toolset)
        .build();
  }

  // @@@SNIPEND

  public static void register(Worker worker) {
    worker.registerWorkflowImplementationTypes(
        ToolsWorkflowImpl.class,
        MultiAgentWorkflowImpl.class,
        McpWorkflowImpl.class,
        ApprovalWorkflowImpl.class,
        ChatWorkflowImpl.class,
        StructuredOutputWorkflowImpl.class,
        LongRunningWorkflowImpl.class);
    worker.registerActivitiesImplementations(
        new WeatherActivitiesImpl(), new OrderActivitiesImpl(), new ReportActivitiesImpl());
  }
}
