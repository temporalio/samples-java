

package io.temporal.samples.payloadconverter.crypto;

import com.codingrodent.jackson.crypto.CryptoModule;
import com.codingrodent.jackson.crypto.EncryptionService;
import com.codingrodent.jackson.crypto.PasswordCryptoContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class Starter {
  private static final String TASK_QUEUE = "CryptoConverterQueue";
  private static final String encryptDecryptPassword = "encryptDecryptPassword";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    // Set crypto data converter in client options
    WorkflowClient client =
        WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setDataConverter(
                    DefaultDataConverter.newDefaultInstance()
                        .withPayloadConverterOverrides(getCryptoJacksonJsonPayloadConverter()))
                .build());

    // Create worker and start Worker factory
    createWorker(client);

    // Create typed workflow stub
    CryptoWorkflow workflow =
        client.newWorkflowStub(
            CryptoWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("cryptoWorkflow")
                .setTaskQueue(TASK_QUEUE)
                .build());

    // Start workflow exec and wait for results (sync)
    MyCustomer customer = workflow.exec(new MyCustomer("John", 22));

    System.out.println("Approved: " + customer.isApproved());

    System.exit(0);
  }

  private static JacksonJsonPayloadConverter getCryptoJacksonJsonPayloadConverter() {
    ObjectMapper objectMapper = new ObjectMapper();

    // Create the Crypto Context (password based)
    PasswordCryptoContext cryptoContext =
        new PasswordCryptoContext(
            encryptDecryptPassword, // decrypt password
            encryptDecryptPassword, // encrypt password
            PasswordCryptoContext.CIPHER_NAME, // cipher name
            PasswordCryptoContext.KEY_NAME); // key generator names
    EncryptionService encryptionService = new EncryptionService(objectMapper, cryptoContext);
    objectMapper.registerModule(new CryptoModule().addEncryptionService(encryptionService));

    return new JacksonJsonPayloadConverter(objectMapper);
  }

  private static void createWorker(WorkflowClient client) {
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(CryptoWorkflowImpl.class);
    factory.start();
  }
}
