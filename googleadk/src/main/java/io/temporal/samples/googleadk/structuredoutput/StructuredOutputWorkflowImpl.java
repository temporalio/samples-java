package io.temporal.samples.googleadk.structuredoutput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.SchemaUtils;
import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import io.temporal.failure.ApplicationFailure;
import io.temporal.googleadk.TemporalAdk;
import io.temporal.googleadk.model.TemporalModel;
import io.temporal.samples.googleadk.AgentSupport;
import java.util.List;
import java.util.Map;

public class StructuredOutputWorkflowImpl implements StructuredOutputWorkflow {
  // @@@SNIPSTART java-google-adk-structuredoutput-schema
  private static final Schema PLAN_SCHEMA =
      Schema.builder()
          .type("OBJECT")
          .properties(
              Map.of(
                  "city", Schema.builder().type("STRING").build(),
                  "days", Schema.builder().type("INTEGER").minimum(1.0).maximum(7.0).build(),
                  "activities",
                      Schema.builder()
                          .type("ARRAY")
                          .items(Schema.builder().type("STRING").build())
                          .minItems(1L)
                          .build()))
          .required(List.of("city", "days", "activities"))
          .build();

  // @@@SNIPEND

  // @@@SNIPSTART java-google-adk-structuredoutput-workflow
  @Override
  public TravelPlan plan(String request) {
    LlmAgent agent =
        LlmAgent.builder()
            .name("structured_planner")
            .model(TemporalModel.create(AgentSupport.MODEL))
            .instruction(
                "Return a travel plan as JSON with city, days (1 through 7), and activities.")
            .outputSchema(PLAN_SCHEMA)
            .build();
    Runner runner = TemporalAdk.runnerBuilder().appName("structured_plan").agent(agent).build();
    String json =
        AgentSupport.lastText(
            TemporalAdk.run(
                runner,
                AgentSupport.USER,
                AgentSupport.SESSION,
                Content.fromParts(Part.fromText(request))));
    try {
      ObjectMapper mapper =
          new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
      Map<String, Object> output = mapper.readValue(json, new TypeReference<>() {});
      if (output == null) {
        throw new IllegalArgumentException("A plan must be a JSON object.");
      }
      SchemaUtils.validateMapOnSchema(output, PLAN_SCHEMA, false);
      return mapper.convertValue(output, TravelPlan.class);
    } catch (JsonProcessingException | IllegalArgumentException e) {
      throw ApplicationFailure.newNonRetryableFailure(
          "The model did not return a valid travel plan.", "InvalidTravelPlan");
    }
  }
  // @@@SNIPEND
}
