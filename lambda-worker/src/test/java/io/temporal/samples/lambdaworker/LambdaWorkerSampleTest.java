package io.temporal.samples.lambdaworker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.temporal.aws.lambda.LambdaWorker;
import io.temporal.aws.lambda.LambdaWorkerOptions;
import io.temporal.common.WorkerDeploymentVersion;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Tests Lambda worker registration without AWS. */
public class LambdaWorkerSampleTest {

  @Test
  public void configureSetsTaskQueueAndRegistrations() throws Exception {
    LambdaWorkerOptions.Builder builder = LambdaWorkerOptions.newBuilderFromEnvironment(baseEnv());

    LambdaWorkerSample.configure(builder);
    LambdaWorkerOptions options = builder.build();

    assertEquals(LambdaWorkerSample.DEFAULT_TASK_QUEUE, options.getTaskQueue());
    assertEquals(2, registrations(options).size());

    RequestHandler<Object, Void> handler =
        LambdaWorker.newHandler(
            new WorkerDeploymentVersion(
                LambdaWorkerSample.DEFAULT_DEPLOYMENT_NAME, LambdaWorkerSample.DEFAULT_BUILD_ID),
            options);
    assertNotNull(handler);
  }

  @Test
  public void configureUsesDefaultTaskQueueWhenNoProcessEnvironmentOverride() throws Exception {
    Map<String, String> env = baseEnv();
    env.put(LambdaWorkerOptions.TEMPORAL_TASK_QUEUE, "from-env");
    LambdaWorkerOptions.Builder builder = LambdaWorkerOptions.newBuilderFromEnvironment(env);

    LambdaWorkerSample.configure(builder);
    LambdaWorkerOptions options = builder.build();

    assertEquals(LambdaWorkerSample.DEFAULT_TASK_QUEUE, options.getTaskQueue());
    assertEquals(2, registrations(options).size());
  }

  private static Map<String, String> baseEnv() {
    Map<String, String> env = new HashMap<>();
    env.put(LambdaWorkerOptions.TEMPORAL_CONFIG_FILE, "/nonexistent/temporal.toml");
    return env;
  }

  private static List<?> registrations(LambdaWorkerOptions options) throws Exception {
    Field registrations = LambdaWorkerOptions.class.getDeclaredField("registrations");
    registrations.setAccessible(true);
    Object value = registrations.get(options);
    assertTrue(value instanceof List);
    return (List<?>) value;
  }
}
