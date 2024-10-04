package io.temporal.samples.safemessagepassing;

import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerActivitiesImpl implements ClusterManagerActivities {
  private static final Logger log = LoggerFactory.getLogger(ClusterManagerActivitiesImpl.class);

  @Override
  public void assignNodesToJob(AssignNodesToJobInput input) {
    for (String node : input.getNodes()) {
      log.info("Assigned node " + node + " to job " + input.getJobName());
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void unassignNodesForJob(UnassignNodesForJobInput input) {
    for (String node : input.getNodes()) {
      log.info("Unassigned node " + node + " from job " + input.getJobName());
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<String> findBadNodes(FindBadNodesInput input) {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    Set<String> badNodes =
        input.getNodesToCheck().stream()
            .filter(n -> Integer.parseInt(n) % 5 == 0)
            .collect(Collectors.toSet());
    if (!badNodes.isEmpty()) {
      log.info("Found bad nodes: " + badNodes);
    } else {
      log.info("No bad nodes found");
    }
    return badNodes;
  }
}
