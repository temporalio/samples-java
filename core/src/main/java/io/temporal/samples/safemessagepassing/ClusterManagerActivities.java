package io.temporal.samples.safemessagepassing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.List;
import java.util.Set;

@ActivityInterface
public interface ClusterManagerActivities {

  class AssignNodesToJobInput {
    private final List<String> nodes;
    private final String jobName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AssignNodesToJobInput(
        @JsonProperty("nodes_to_assign") Set<String> nodesToAssign,
        @JsonProperty("job_name") String jobName) {
      this.nodes = List.copyOf(nodesToAssign);
      this.jobName = jobName;
    }

    @JsonProperty("nodes_to_assign")
    public List<String> getNodes() {
      return nodes;
    }

    @JsonProperty("job_name")
    public String getJobName() {
      return jobName;
    }
  }

  @ActivityMethod
  void assignNodesToJob(AssignNodesToJobInput input);

  class UnassignNodesForJobInput {
    private final List<String> nodes;
    private final String jobName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public UnassignNodesForJobInput(
        @JsonProperty("nodes") Set<String> nodes, @JsonProperty("job_name") String jobName) {
      this.nodes = List.copyOf(nodes);
      this.jobName = jobName;
    }

    @JsonProperty("nodes")
    public List<String> getNodes() {
      return nodes;
    }

    @JsonProperty("job_name")
    public String getJobName() {
      return jobName;
    }
  }

  @ActivityMethod
  void unassignNodesForJob(UnassignNodesForJobInput input);

  class FindBadNodesInput {
    private final Set<String> nodesToCheck;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public FindBadNodesInput(@JsonProperty("assigned_nodes") Set<String> assignedNodes) {
      this.nodesToCheck = assignedNodes;
    }

    @JsonProperty("assigned_nodes")
    public Set<String> getNodesToCheck() {
      return nodesToCheck;
    }
  }

  @ActivityMethod
  Set<String> findBadNodes(FindBadNodesInput input);

  @ActivityMethod
  void shutdown();
}
