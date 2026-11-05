package io.temporal.samples.moneybatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pending requests and deduplication state carried to the next batch execution. */
public class BatchState {
  public List<Withdrawal> pending = new ArrayList<>();
  public Set<String> references = new HashSet<>();

  public BatchState() {}

  public BatchState(Collection<Withdrawal> pending, Set<String> references) {
    this.pending = new ArrayList<>(pending);
    this.references = new HashSet<>(references);
  }

  public static class Withdrawal {
    public String fromAccountId;
    public String referenceId;
    public int amountCents;

    public Withdrawal() {}

    public Withdrawal(String fromAccountId, String referenceId, int amountCents) {
      this.fromAccountId = fromAccountId;
      this.referenceId = referenceId;
      this.amountCents = amountCents;
    }
  }
}
