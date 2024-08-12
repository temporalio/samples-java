/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.messagepassing.introduction;

import io.temporal.client.*;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * A simple workflow that demonstrates message passing between the workflow and the workflow client.
 */
public class MessagePassingIntro {
  public enum Language {
    CHINESE,
    ENGLISH,
    FRENCH,
    SPANISH,
    PORTUGUESE,
  }
  // Define the task queue name
  static final String TASK_QUEUE = "MessagePassingIntro";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "MessagePassingIntroWorkflow";

  public static class GetLanguagesInput {
    private boolean includeUnsupported;

    public GetLanguagesInput() {
      this.includeUnsupported = false;
    }

    public GetLanguagesInput(boolean includeUnsupported) {
      this.includeUnsupported = includeUnsupported;
    }

    public boolean getIncludeUnsupported() {
      return includeUnsupported;
    }

    public void setIncludeUnsupported(boolean includeUnsupported) {
      this.includeUnsupported = includeUnsupported;
    }
  }

  public static class ApproveInput {
    private String name;

    public ApproveInput() {}

    public ApproveInput(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @WorkflowInterface
  public interface GreetingWorkflow {
    @WorkflowMethod
    String getGreetings();

    @QueryMethod
    List<Language> getLanguages(GetLanguagesInput input);

    @QueryMethod
    Language getLanguage();

    @UpdateMethod
    Language setLanguage(Language language);

    @UpdateValidatorMethod(updateName = "setLanguage")
    void setLanguageValidator(Language language);

    @SignalMethod
    void approve(ApproveInput input);
  }

  // Define the workflow implementation which implements the getGreetings workflow method.
  public static class GreetingWorkflowImpl implements GreetingWorkflow {
    private static final Logger log = Workflow.getLogger(GreetingWorkflowImpl.class);
    private boolean approvedForRelease = false;
    private String approverName = null;
    private Language language = Language.ENGLISH;
    private Map<Language, String> greetings =
        Map.of(
            Language.ENGLISH, "Hello, world",
            Language.CHINESE, "你好，世界");

    @Override
    public String getGreetings() {
      Workflow.await(() -> approvedForRelease);
      log.info("Approved for release by " + approverName);
      return greetings.get(language);
    }

    @Override
    public List<Language> getLanguages(GetLanguagesInput input) {
      if (input.includeUnsupported) {
        return Arrays.asList(Language.values());
      } else {
        return new ArrayList(greetings.keySet());
      }
    }

    @Override
    public Language getLanguage() {
      return language;
    }

    @Override
    public Language setLanguage(Language language) {
      Language previousLanguage = this.language;
      this.language = language;
      return previousLanguage;
    }

    @Override
    public void setLanguageValidator(Language language) {
      if (!greetings.containsKey(language)) {
        throw new IllegalArgumentException("Unsupported language: " + language);
      }
    }

    @Override
    public void approve(ApproveInput input) {
      approvedForRelease = true;
      approverName = input.name;
    }
  }

  /**
   * With the Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws Exception {

    // Get a Workflow service stub.
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    /*
     * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
     */
    WorkflowClient client = WorkflowClient.newInstance(service);

    /*
     * Define the workflow factory. It is used to create workflow workers for a specific task queue.
     */
    WorkerFactory factory = WorkerFactory.newInstance(client);

    /*
     * Define the workflow worker. Workflow workers listen to a defined task queue and process
     * workflows and activities.
     */
    Worker worker = factory.newWorker(TASK_QUEUE);

    /*
     * Register the workflow implementation with the worker.
     * Workflow implementations must be known to the worker at runtime in
     * order to dispatch workflow tasks.
     */
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    /*
     * Start all the workers registered for a specific task queue.
     * The started workers then start polling for workflows and activities.
     */
    factory.start();

    // Create the workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build();

    // Create the workflow client stub. It is used to start the workflow execution.
    GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously and call its getGreeting workflow method
    WorkflowClient.start(workflow::getGreetings);

    List<Language> languages = workflow.getLanguages(new GetLanguagesInput(false));
    System.out.println("Supported languages: " + languages);

    Language previousLanguage = workflow.setLanguage(Language.CHINESE);
    Language currentLanguage = workflow.getLanguage();
    System.out.println("Language changed: " + previousLanguage + "->" + currentLanguage);

    UpdateHandle<Language> handle =
        WorkflowStub.fromTyped(workflow)
            .startUpdate(
                "setLanguage", WorkflowUpdateStage.ACCEPTED, Language.class, Language.ENGLISH);
    previousLanguage = handle.getResultAsync().get();
    currentLanguage = workflow.getLanguage();
    System.out.println("Language changed: " + previousLanguage + "->" + currentLanguage);

    workflow.approve(new ApproveInput("John Doe"));

    System.out.println(workflow.getGreetings());
    System.exit(0);
  }
}
