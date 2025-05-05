package io.temporal.samples.payloadconverter;

import static org.junit.Assert.*;

import com.codingrodent.jackson.crypto.CryptoModule;
import com.codingrodent.jackson.crypto.EncryptionService;
import com.codingrodent.jackson.crypto.PasswordCryptoContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.samples.payloadconverter.crypto.CryptoWorkflow;
import io.temporal.samples.payloadconverter.crypto.CryptoWorkflowImpl;
import io.temporal.samples.payloadconverter.crypto.MyCustomer;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

public class CryptoPayloadConverterTest {
  private static final String encryptDecryptPassword = "encryptDecryptPassword";

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowClientOptions(
              WorkflowClientOptions.newBuilder()
                  .setDataConverter(
                      DefaultDataConverter.newDefaultInstance()
                          .withPayloadConverterOverrides(getCryptoJacksonJsonPayloadConverter()))
                  .build())
          .setWorkflowTypes(CryptoWorkflowImpl.class)
          .build();

  @Test
  public void testEncryptedWorkflowData() {
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build();
    CryptoWorkflow workflow =
        testWorkflowRule.getWorkflowClient().newWorkflowStub(CryptoWorkflow.class, workflowOptions);

    MyCustomer customer = workflow.exec(new MyCustomer("John", 22));
    assertNotNull(customer);
    assertTrue(customer.isApproved());
  }

  private JacksonJsonPayloadConverter getCryptoJacksonJsonPayloadConverter() {
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
}
