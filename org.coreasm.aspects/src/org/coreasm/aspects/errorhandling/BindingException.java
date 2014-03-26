package org.coreasm.aspects.errorhandling;

import org.coreasm.engine.interpreter.ASTNode;

public class BindingException extends AspectException {

	/** serialization id */
	private static final long serialVersionUID = 1L;
	private ASTNode parameterNode = null;

	public BindingException(String string, ASTNode node) {
		super(node, string);
		parameterNode = node;
	}

	public ASTNode getParameterNode() {
		return parameterNode;
	}

}
