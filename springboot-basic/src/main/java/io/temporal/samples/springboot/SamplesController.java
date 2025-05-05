package io.temporal.samples.springboot;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.springboot.hello.HelloWorkflow;
import io.temporal.samples.springboot.hello.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SamplesController {

  @Autowired WorkflowClient client;

  @GetMapping("/hello")
  public String hello(Model model) {
    model.addAttribute("sample", "Say Hello");
    return "hello";
  }

  @PostMapping(
      value = "/hello",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.TEXT_HTML_VALUE})
  ResponseEntity<String> helloSample(@RequestBody Person person) {
    HelloWorkflow workflow =
        client.newWorkflowStub(
            HelloWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue("HelloSampleTaskQueue")
                .setWorkflowId("HelloSample")
                .build());

    // bypass thymeleaf, don't return template name just result
    return new ResponseEntity<>("\"" + workflow.sayHello(person) + "\"", HttpStatus.OK);
  }
}
