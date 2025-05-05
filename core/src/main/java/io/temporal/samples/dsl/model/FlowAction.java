

package io.temporal.samples.dsl.model;

public class FlowAction {
  private String action;
  private String compensateBy;
  private int retries;
  private int startToCloseSec;
  private int next;

  public FlowAction() {}

  public FlowAction(
      String action, String compensateBy, int retries, int startToCloseSec, int next) {
    this.action = action;
    this.compensateBy = compensateBy;
    this.retries = retries;
    this.startToCloseSec = startToCloseSec;
    this.next = next;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getCompensateBy() {
    return compensateBy;
  }

  public void setCompensateBy(String compensateBy) {
    this.compensateBy = compensateBy;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  public int getStartToCloseSec() {
    return startToCloseSec;
  }

  public void setStartToCloseSec(int startToCloseSec) {
    this.startToCloseSec = startToCloseSec;
  }

  public int getNext() {
    return next;
  }

  public void setNext(int next) {
    this.next = next;
  }
}
