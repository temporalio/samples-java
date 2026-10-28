package io.temporal.samples.googleadk;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.googleadk.GoogleAdkPlugin;
import io.temporal.samples.googleadk.chat.ChatInput;
import io.temporal.samples.googleadk.chat.ChatWorkflow;
import io.temporal.samples.googleadk.humanintheloop.ApprovalWorkflow;
import io.temporal.samples.googleadk.mcp.McpWorkflow;
import io.temporal.samples.googleadk.multiagent.MultiAgentWorkflow;
import io.temporal.samples.googleadk.tools.ToolsWorkflow;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Arrays;
import java.util.UUID;

/** Starts samples or sends messages to their existing Workflow Executions. */
public final class SampleStarter {
  private SampleStarter() {}

  public static void main(String[] args) {
    if (args.length == 0) {
      args = new String[] {"chat"};
    }
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    try {
      // Register the plugin on external clients too, so Queries/Signals use the same converter.
      WorkflowClient client =
          WorkflowClient.newInstance(
              service,
              WorkflowClientOptions.newBuilder()
                  .setPlugins(GoogleAdkPlugin.newBuilder().build())
                  .build());
      String id = "google-adk-" + UUID.randomUUID();
      WorkflowOptions options =
          WorkflowOptions.newBuilder()
              .setTaskQueue(AgentSupport.TASK_QUEUE)
              .setWorkflowId(id)
              .build();
      switch (args[0]) {
        case "tools":
          System.out.println(
              client
                  .newWorkflowStub(ToolsWorkflow.class, options)
                  .ask("What's the weather in Chicago?"));
          break;
        case "multiagent":
          client
              .newWorkflowStub(MultiAgentWorkflow.class, options)
              .plan("Chicago")
              .forEach(System.out::println);
          break;
        case "mcp":
          System.out.println(
              client.newWorkflowStub(McpWorkflow.class, options).echo("Hello from Temporal"));
          break;
        case "approval":
          ApprovalWorkflow approval = client.newWorkflowStub(ApprovalWorkflow.class, options);
          WorkflowClient.start(approval::fulfill, "Ship order demo-order, after confirmation.");
          System.out.println("Workflow ID: " + id);
          break;
        case "chat":
          // @@@SNIPSTART java-google-adk-starter
          ChatWorkflow chat = client.newWorkflowStub(ChatWorkflow.class, options);
          WorkflowClient.start(chat::chat, ChatInput.empty());
          System.out.println("Workflow ID: " + id);
          // @@@SNIPEND
          break;
        case "pending":
          requireArgs(args, 2);
          client
              .newWorkflowStub(ApprovalWorkflow.class, args[1])
              .pendingConfirmations()
              .forEach(System.out::println);
          break;
        case "decide":
          requireArgs(args, 3);
          if (!args[2].equals("true") && !args[2].equals("false")) {
            throw new IllegalArgumentException("Decision must be true or false.");
          }
          ApprovalWorkflow pending = client.newWorkflowStub(ApprovalWorkflow.class, args[1]);
          pending.decide(Boolean.parseBoolean(args[2]));
          System.out.println("Decision sent.");
          break;
        case "result":
          requireArgs(args, 2);
          System.out.println(client.newUntypedWorkflowStub(args[1]).getResult(String.class));
          break;
        case "message":
          requireArgs(args, 3);
          client
              .newWorkflowStub(ChatWorkflow.class, args[1])
              .message(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
          break;
        case "reply":
          requireArgs(args, 2);
          System.out.println(client.newWorkflowStub(ChatWorkflow.class, args[1]).lastReply());
          break;
        case "finish":
          requireArgs(args, 2);
          ChatWorkflow conversation = client.newWorkflowStub(ChatWorkflow.class, args[1]);
          conversation.finish();
          System.out.println(
              io.temporal.client.WorkflowStub.fromTyped(conversation).getResult(String.class));
          break;
        default:
          throw new IllegalArgumentException("Unknown command: " + args[0]);
      }
    } finally {
      service.shutdown();
    }
  }

  private static void requireArgs(String[] args, int count) {
    if (args.length < count) {
      throw new IllegalArgumentException("Missing arguments; see googleadk/README.md.");
    }
  }
}
