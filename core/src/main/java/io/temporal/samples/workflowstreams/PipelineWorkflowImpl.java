package io.temporal.samples.workflowstreams;

import io.temporal.samples.workflowstreams.Shared.PipelineInput;
import io.temporal.samples.workflowstreams.Shared.StageEvent;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import io.temporal.workflowstreams.WorkflowStream;
import io.temporal.workflowstreams.WorkflowTopicHandle;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class PipelineWorkflowImpl implements PipelineWorkflow {

  private final WorkflowTopicHandle status;

  @WorkflowInit
  public PipelineWorkflowImpl(PipelineInput input) {
    WorkflowStream stream = WorkflowStream.newInstance(input.streamState);
    status = stream.topic(Shared.TOPIC_STATUS);
  }

  @Override
  public String runPipeline(PipelineInput input) {
    List<String> stages =
        Arrays.asList(
            "validating",
            "loading data",
            "transforming",
            "writing output",
            "verifying",
            "complete");
    for (String stage : stages) {
      status.publish(new StageEvent(stage));
      if (!stage.equals("complete")) {
        Workflow.sleep(Duration.ofSeconds(2));
      }
    }

    // The "complete" stage above is the in-band terminator subscribers break on. Hold the
    // run open briefly so the final poll delivers it.
    Workflow.sleep(OrderWorkflowImpl.DRAIN_DELAY);
    return "pipeline " + input.pipelineId + " done";
  }
}
