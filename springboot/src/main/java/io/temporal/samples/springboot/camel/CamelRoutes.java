package io.temporal.samples.springboot.camel;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CamelRoutes extends RouteBuilder {

  @Autowired private WorkflowClient workflowClient;
  @Autowired OrderRepository repository;

  @Override
  public void configure() {
    from("direct:getOrders")
        .routeId("direct-getOrders")
        .tracing()
        .process(
            exchange -> {
              OrderWorkflow workflow =
                  workflowClient.newWorkflowStub(
                      OrderWorkflow.class,
                      WorkflowOptions.newBuilder()
                          .setWorkflowId("CamelSampleWorkflow")
                          .setTaskQueue("CamelSampleTaskQueue")
                          .build());
              exchange.getIn().setBody(workflow.start());
            })
        .end();

    from("direct:findAllOrders")
        .routeId("direct-findAllOrders")
        .process(
            exchange -> {
              exchange.getIn().setBody(repository.findAll());
            })
        .end();
  }
}
