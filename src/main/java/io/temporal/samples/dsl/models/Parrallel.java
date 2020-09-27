package io.temporal.samples.dsl.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Parrallel {
  public Statement branches[];

  @JsonCreator
  public Parrallel(@JsonProperty("branches") Statement branches[]) {
    this.branches = branches;
  }

  public void execute(Map<String, String> bindings) {
    // You can use the context passed in to activity as a way to cancel the activity like standard
    // GO way.
    // Cancelling a parent context will cancel all the derived contexts as well.

    // In the parallel block, we want to execute all of them in parallel and wait for all of them.
    // if one activity fails then we want to cancel all the rest of them as well.
    List<Promise<Void>> results = new ArrayList<>(bindings.size());
    CancellationScope scope =
        Workflow.newCancellationScope(
            () -> {
              for (Statement statement : branches) {
                results.add(Async.function(statement::execute, bindings));
              }
            });
    // As code inside the scope is non blocking the run doesn't block.
    scope.run();

    try {
      // If one activity fails then all the rest will fail
      Promise.allOf(results).get();
    } catch (RuntimeException ex) {
      // Cancel uncompleted activities
      scope.cancel();
      System.out.println("One of the Activities failed.  Canceling the rest." + ex.getMessage());
      throw ex;
    }
  }
}
