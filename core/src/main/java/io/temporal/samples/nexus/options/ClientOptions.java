package io.temporal.samples.nexus.options;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.net.ssl.SSLException;
import org.apache.commons.cli.*;

public class ClientOptions {

  public static WorkflowClient getWorkflowClient(String[] args) {
    return getWorkflowClient(args, WorkflowClientOptions.newBuilder());
  }

  public static WorkflowClient getWorkflowClient(
      String[] args, WorkflowClientOptions.Builder clientOptions) {
    Options options = new Options();
    Option targetHostOption = new Option("target-host", true, "Host:port for the Temporal service");
    targetHostOption.setRequired(false);
    options.addOption(targetHostOption);

    Option namespaceOption = new Option("namespace", true, "Namespace to connect to");
    namespaceOption.setRequired(false);
    options.addOption(namespaceOption);

    Option serverRootCaOption =
        new Option("server-root-ca-cert", true, "Optional path to root server CA cert");
    serverRootCaOption.setRequired(false);
    options.addOption(serverRootCaOption);

    Option clientCertOption =
        new Option(
            "client-cert", true, "Optional path to client cert, mutually exclusive with API key");
    clientCertOption.setRequired(false);
    options.addOption(clientCertOption);

    Option clientKeyOption =
        new Option(
            "client-key", true, "Optional path to client key, mutually exclusive with API key");
    clientKeyOption.setRequired(false);
    options.addOption(clientKeyOption);

    Option apiKeyOption =
        new Option("api-key", true, "Optional API key, mutually exclusive with cert/key");
    apiKeyOption.setRequired(false);
    options.addOption(apiKeyOption);

    Option serverNameOption =
        new Option(
            "server-name", true, "Server name to use for verifying the server's certificate");
    serverNameOption.setRequired(false);
    options.addOption(serverNameOption);

    Option insercureSkipVerifyOption =
        new Option(
            "insecure-skip-verify",
            false,
            "Skip verification of the server's certificate and host name");
    insercureSkipVerifyOption.setRequired(false);
    options.addOption(insercureSkipVerifyOption);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);

      System.exit(1);
    }

    String targetHost = cmd.getOptionValue("target-host", "localhost:7233");
    String namespace = cmd.getOptionValue("namespace", "default");
    String serverRootCaCert = cmd.getOptionValue("server-root-ca-cert", "");
    String clientCert = cmd.getOptionValue("client-cert", "");
    String clientKey = cmd.getOptionValue("client-key", "");
    String serverName = cmd.getOptionValue("server-name", "");
    boolean insecureSkipVerify = cmd.hasOption("insecure-skip-verify");
    String apiKey = cmd.getOptionValue("api-key", "");

    // API key and client cert/key are mutually exclusive
    if (!apiKey.isEmpty() && (!clientCert.isEmpty() || !clientKey.isEmpty())) {
      throw new IllegalArgumentException("API key and client cert/key are mutually exclusive");
    }
    WorkflowServiceStubsOptions.Builder serviceStubOptionsBuilder =
        WorkflowServiceStubsOptions.newBuilder().setTarget(targetHost);
    // Configure TLS if client cert and key are provided
    if (!clientCert.isEmpty() || !clientKey.isEmpty()) {
      if (clientCert.isEmpty() || clientKey.isEmpty()) {
        throw new IllegalArgumentException("Both client-cert and client-key must be provided");
      }
      try {
        SslContextBuilder sslContext =
            SslContextBuilder.forClient()
                .keyManager(new FileInputStream(clientCert), new FileInputStream(clientKey));
        if (serverRootCaCert != null && !serverRootCaCert.isEmpty()) {
          sslContext.trustManager(new FileInputStream(serverRootCaCert));
        }
        if (insecureSkipVerify) {
          sslContext.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }
        serviceStubOptionsBuilder.setSslContext(GrpcSslContexts.configure(sslContext).build());
      } catch (SSLException e) {
        throw new RuntimeException(e);
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
      if (serverName != null && !serverName.isEmpty()) {
        serviceStubOptionsBuilder.setChannelInitializer(c -> c.overrideAuthority(serverName));
      }
    }
    // Configure API key if provided
    if (!apiKey.isEmpty()) {
      serviceStubOptionsBuilder.setEnableHttps(true);
      serviceStubOptionsBuilder.addApiKey(() -> apiKey);
    }

    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(serviceStubOptionsBuilder.build());
    return WorkflowClient.newInstance(service, clientOptions.setNamespace(namespace).build());
  }
}
