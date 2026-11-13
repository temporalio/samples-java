package io.temporal.samples.googleadk.mcp;

import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StdioServerParameters;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import tools.jackson.databind.json.JsonMapper;

/** A local MCP echo server, launched by the worker without Node or another service. */
public final class EchoMcpServer {
  private EchoMcpServer() {}

  public static McpToolset toolset() {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String classpath =
        System.getProperty("googleadk.echo.classpath", System.getProperty("java.class.path"));
    return new McpToolset(
        StdioServerParameters.builder()
            .command(java)
            .args(List.of("-cp", classpath, EchoMcpServer.class.getName()))
            .build()
            .toServerParameters());
  }

  public static void main(String[] args) throws InterruptedException {
    JacksonMcpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());
    McpSchema.Tool echo =
        McpSchema.Tool.builder("echo")
            .description("Echo the supplied text.")
            .inputSchema(
                Map.of(
                    "type", "object",
                    "properties", Map.of("text", Map.of("type", "string")),
                    "required", List.of("text")))
            .build();
    McpSyncServer server =
        McpServer.sync(new StdioServerTransportProvider(mapper))
            .serverInfo("temporal-sample-echo", "1.0.0")
            .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
            .toolCall(
                echo,
                (exchange, request) ->
                    McpSchema.CallToolResult.builder()
                        .addTextContent((String) request.arguments().get("text"))
                        .isError(false)
                        .build())
            .build();
    Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));
    new CountDownLatch(1).await();
  }
}
