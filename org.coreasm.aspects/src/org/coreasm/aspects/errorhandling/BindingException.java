package org.coreasm.aspects.errorhandling;

import org.coreasm.engine.interpreter.FunctionRuleTermNode;

public class BindingException extends AspectException {

	/** serialization id */
	private static final long serialVersionUID = 1L;
	private FunctionRuleTermNode pointCutParameter = null;

	public BindingException(String string, FunctionRuleTermNode functionRuleTermNode, Exception cause) {
		super(functionRuleTermNode, string, cause);
		pointCutParameter = functionRuleTermNode;
	}

	public FunctionRuleTermNode getParameterNode() {
		return pointCutParameter;
	}

}
