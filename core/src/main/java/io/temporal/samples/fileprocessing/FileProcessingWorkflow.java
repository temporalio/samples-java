package io.temporal.samples.fileprocessing;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.net.URL;

/** Contract for file processing workflow. */
@WorkflowInterface
public interface FileProcessingWorkflow {
  @WorkflowMethod
  void processFile(URL source, URL destination);
}
