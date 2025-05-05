

package io.temporal.samples.excludefrominterceptor.workflows;

import io.temporal.workflow.WorkflowInterface;

@WorkflowInterface
public interface MyWorkflowTwo extends MyWorkflow {}
