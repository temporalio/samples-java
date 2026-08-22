package io.temporal.samples.nexuswalkthrough.handler;

import io.temporal.samples.nexuswalkthrough.generatedservice.NotifyRequesterInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.RequestApprovalOutput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionInput;
import io.temporal.samples.nexuswalkthrough.generatedservice.SubmitDecisionOutput;

/**
 * Converts a decision between the generated types that carry one.
 *
 * <p>The contract declares the same {@code APPROVED | DENIED} value set on four different
 * Operations. The generator emits a distinct nested value class for each - {@code
 * RequestApprovalOutput.Decision}, {@code SubmitDecisionInput.Decision}, {@code
 * SubmitDecisionOutput.Recorded}, {@code NotifyRequesterInput.Decision} - because an enum declared
 * inline on a property is scoped to the model that contains it.
 *
 * <p>A single shared enum type would be preferable, and the contract cannot express one today: a
 * named enum under {@code $defs} is rejected by the generator. Until that is supported, the values
 * are carried across type boundaries by their wire string, which is identical in all four.
 *
 * <p>Keeping the conversions here rather than at each call site means the Operation implementations
 * read as though the shared type existed.
 */
final class Decisions {

  private Decisions() {}

  static RequestApprovalOutput.Decision toRequestApprovalOutput(SubmitDecisionInput.Decision d) {
    return require(RequestApprovalOutput.Decision.fromString(d.getValue()), d.getValue());
  }

  static SubmitDecisionOutput.Recorded toSubmitDecisionOutput(SubmitDecisionInput.Decision d) {
    return require(SubmitDecisionOutput.Recorded.fromString(d.getValue()), d.getValue());
  }

  static NotifyRequesterInput.Decision toNotifyRequesterInput(RequestApprovalOutput.Decision d) {
    return require(NotifyRequesterInput.Decision.fromString(d.getValue()), d.getValue());
  }

  /**
   * The generated {@code fromString} returns null for a value the target type does not know. The
   * four value sets are identical today, so this cannot happen - but if the contract ever adds a
   * value to one Operation and not another, failing here names the problem instead of quietly
   * producing an output with a null decision.
   */
  private static <T> T require(T converted, String value) {
    if (converted == null) {
      throw new IllegalStateException("no matching decision constant for wire value " + value);
    }
    return converted;
  }
}
