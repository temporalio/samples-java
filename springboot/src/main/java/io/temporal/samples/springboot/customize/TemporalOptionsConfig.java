package io.temporal.samples.springboot.customize;

import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import io.temporal.spring.boot.WorkerOptionsCustomizer;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkerOptions;
import io.temporal.worker.WorkflowImplementationOptions;
import javax.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalOptionsConfig {

  // Worker specific options customization
  @Bean
  public WorkerOptionsCustomizer customWorkerOptions() {
    return new WorkerOptionsCustomizer() {
      @Nonnull
      @Override
      public WorkerOptions.Builder customize(
          @Nonnull WorkerOptions.Builder optionsBuilder,
          @Nonnull String workerName,
          @Nonnull String taskQueue) {

        // For CustomizeTaskQueue (also name of worker) we set worker
        // to only handle workflow tasks and local activities
        if (taskQueue.equals("CustomizeTaskQueue")) {
          optionsBuilder.setLocalActivityWorkerOnly(true);
        }
        return optionsBuilder;
      }
    };
  }

  // WorkflowServiceStubsOptions customization
  @Bean
  public TemporalOptionsCustomizer<WorkflowServiceStubsOptions.Builder>
      customServiceStubsOptions() {
    return new TemporalOptionsCustomizer<WorkflowServiceStubsOptions.Builder>() {
      @Nonnull
      @Override
      public WorkflowServiceStubsOptions.Builder customize(
          @Nonnull WorkflowServiceStubsOptions.Builder optionsBuilder) {
        // set options on optionsBuilder as needed
        // ...
        return optionsBuilder;
      }
    };
  }

  // WorkflowClientOption customization
  @Bean
  public TemporalOptionsCustomizer<WorkflowClientOptions.Builder> customClientOptions() {
    return new TemporalOptionsCustomizer<WorkflowClientOptions.Builder>() {
      @Nonnull
      @Override
      public WorkflowClientOptions.Builder customize(
          @Nonnull WorkflowClientOptions.Builder optionsBuilder) {
        // set options on optionsBuilder as needed
        // ...
        return optionsBuilder;
      }
    };
  }

  // WorkerFactoryOptions customization
  @Bean
  public TemporalOptionsCustomizer<WorkerFactoryOptions.Builder> customWorkerFactoryOptions() {
    return new TemporalOptionsCustomizer<WorkerFactoryOptions.Builder>() {
      @Nonnull
      @Override
      public WorkerFactoryOptions.Builder customize(
          @Nonnull WorkerFactoryOptions.Builder optionsBuilder) {
        // set options on optionsBuilder as needed
        // ...
        return optionsBuilder;
      }
    };
  }

  // WorkflowImplementationOptions customization
  @Bean
  public TemporalOptionsCustomizer<WorkflowImplementationOptions.Builder>
      customWorkflowImplementationOptions() {
    return new TemporalOptionsCustomizer<>() {
      @Nonnull
      @Override
      public WorkflowImplementationOptions.Builder customize(
          @Nonnull WorkflowImplementationOptions.Builder optionsBuilder) {
        // set options on optionsBuilder such as per-activity options
        return optionsBuilder;
      }
    };
  }
}
