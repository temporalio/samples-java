package io.temporal.samples.googleadk;

import com.google.adk.models.Gemini;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.googleadk.GoogleAdkPlugin;
import io.temporal.samples.googleadk.basic.BasicWorkflowImpl;
import io.temporal.samples.googleadk.chat.ChatWorkflowImpl;
import io.temporal.samples.googleadk.humanintheloop.ApprovalWorkflowImpl;
import io.temporal.samples.googleadk.humanintheloop.OrderActivitiesImpl;
import io.temporal.samples.googleadk.mcp.EchoMcpServer;
import io.temporal.samples.googleadk.mcp.McpWorkflowImpl;
import io.temporal.samples.googleadk.multiagent.MultiAgentWorkflowImpl;
import io.temporal.samples.googleadk.tools.ToolsWorkflowImpl;
import io.temporal.samples.googleadk.tools.WeatherActivitiesImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/** One worker hosts all six samples; real model credentials are used only here. */
public final class SampleWorker {
  private SampleWorker() {}

  public static void main(String[] args) {
    // @@@SNIPSTART java-google-adk-worker
    // Client reads GOOGLE_API_KEY or the Vertex AI environment variables on the worker.
    // One HTTP attempt disables native retries so Temporal Activities own retries.
    Client modelClient =
        Client.builder()
            .httpOptions(
                HttpOptions.builder()
                    .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                    .build())
            .build();
    GoogleAdkPlugin plugin =
        GoogleAdkPlugin.newBuilder()
            .model(
                AgentSupport.MODEL,
                () -> Gemini.builder().modelName(AgentSupport.MODEL).apiClient(modelClient).build())
            .mcpToolset("echo", EchoMcpServer::toolset)
            .build();
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setPlugins(plugin).build());
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(AgentSupport.TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(
        BasicWorkflowImpl.class,
        ToolsWorkflowImpl.class,
        MultiAgentWorkflowImpl.class,
        McpWorkflowImpl.class,
        ApprovalWorkflowImpl.class,
        ChatWorkflowImpl.class);
    worker.registerActivitiesImplementations(
        new WeatherActivitiesImpl(), new OrderActivitiesImpl());
    factory.start();
    // @@@SNIPEND
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  factory.shutdown();
                  factory.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
                  modelClient.close();
                  service.shutdown();
                }));
  }
}
