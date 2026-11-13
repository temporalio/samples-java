## Resilient fan-out: partial failure tolerance in a heterogeneous DAG

A user-onboarding Workflow starts three Child Workflows in parallel:

- `CreateAccountWorkflow` -- **critical**. Onboarding cannot succeed without it.
- `ProvisionStorageWorkflow` -- **best-effort**. Can be redone later if it fails.
- `SendWelcomeEmailWorkflow` -- **best-effort**. Not worth failing onboarding over.

This is different from a homogeneous fan-out, where every child does the same kind of work and
the parent waits for all of them with `Promise.allOf(...).get()` -- fine when the run really is
all-or-nothing, but wrong here: that call fails fast on whichever promise fails *first*, even if
it's a best-effort branch, and it never tears down the children still running.

This sample shows three things missing from the existing Child Workflow docs/samples:

1. **Await each branch individually**, not with `Promise.allOf(...).get()`, so a best-effort
   failure doesn't take down a healthy critical branch (or vice versa).
2. **Explicitly cancel the still-running best-effort children** (via a `CancellationScope`)
   when the critical branch fails, rather than relying on the default `ParentClosePolicy`
   (`TERMINATE`). This isn't just tidiness. `TERMINATE` kills a child outright the moment the
   parent closes -- it never delivers a `CanceledFailure`, so the child's own code gets no chance
   to react. That matters most when the child has already dispatched real work to a downstream
   system before the parent fails: that work doesn't know or care that its Workflow wrapper just
   died, so it keeps running on its own, and when it eventually finishes it has no workflow left
   to report back to (`WorkflowNotFoundException`). An explicit `.cancel()` at least gives the
   child a `CanceledFailure` it can act on before that happens.
3. **Reconcile the failure with independent, isolated side effects.** `Saga` with
   `setParallelCompensation(true)` is normally framed as "undo what I already did," but the same
   isolation is exactly what you want for a failure-notification step that hits several unrelated
   systems, where one call failing must not block the others.

Run the sample against each outcome:

```bash
# Happy path -- account created, storage provisioned, email sent.
./gradlew -q execute -PmainClass=io.temporal.samples.resilientfanout.Starter --args="user-1"

# Best-effort branch fails -- onboarding still succeeds, but degraded.
./gradlew -q execute -PmainClass=io.temporal.samples.resilientfanout.Starter --args="user-degraded"

# Critical branch fails -- onboarding fails, best-effort siblings are cancelled, failure is reconciled.
./gradlew -q execute -PmainClass=io.temporal.samples.resilientfanout.Starter --args="user-critical-failure"
```

Sample unit testing: [OnboardingWorkflowTest](https://github.com/temporalio/samples-java/blob/main/core/src/test/java/io/temporal/samples/resilientfanout/OnboardingWorkflowTest.java)
