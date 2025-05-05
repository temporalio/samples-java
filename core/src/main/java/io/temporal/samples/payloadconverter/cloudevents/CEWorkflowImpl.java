package io.temporal.samples.payloadconverter.cloudevents;

import io.cloudevents.CloudEvent;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;

public class CEWorkflowImpl implements CEWorkflow {

  private List<CloudEvent> eventList = new ArrayList<>();

  @Override
  public void exec(CloudEvent cloudEvent) {

    eventList.add(cloudEvent);

    Workflow.await(() -> eventList.size() == 10);
  }

  @Override
  public void addEvent(CloudEvent cloudEvent) {
    eventList.add(cloudEvent);
  }

  @Override
  public CloudEvent getLastEvent() {
    return eventList.get(eventList.size() - 1);
  }
}
