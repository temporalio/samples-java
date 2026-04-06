package io.temporal.samples.springai.mcp;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow interface demonstrating MCP (Model Context Protocol) integration.
 *
 * <p>This workflow shows how to use tools from MCP servers within Temporal workflows. The AI model
 * can call MCP tools (like file system operations) as durable activities.
 */
@WorkflowInterface
public interface McpWorkflow {

  /**
   * Runs the workflow until ended.
   *
   * @return summary of the chat session
   */
  @WorkflowMethod
  String run();

  /**
   * Sends a message to the AI assistant with MCP tools available.
   *
   * @param message the user message
   */
  @SignalMethod
  void chat(String message);

  /**
   * Gets the last response from the AI.
   *
   * @return the last response
   */
  @QueryMethod
  String getLastResponse();

  /**
   * Lists the available MCP tools.
   *
   * @return list of available tools
   */
  @QueryMethod
  String listTools();

  /** Ends the chat session. */
  @SignalMethod
  void end();
}
