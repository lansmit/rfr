package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ProbeTargetCondition implements ExecutionCondition {

  public static final String RUN_PROBES = "rfr.probes.run";

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return context.getConfigurationParameter(RUN_PROBES, Boolean::parseBoolean).orElse(false)
        ? ConditionEvaluationResult.enabled("started by its outer test through EngineTestKit")
        : ConditionEvaluationResult.disabled("probe runs only from its outer test through EngineTestKit");
  }
}
