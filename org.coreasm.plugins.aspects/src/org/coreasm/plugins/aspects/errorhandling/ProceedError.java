package org.coreasm.plugins.aspects.errorhandling;

import java.util.Stack;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter.CallStackElement;
import org.coreasm.engine.parser.CharacterPosition;

public class ProceedError extends AspectException {

	public ProceedError(ASTNode node, String message, Throwable cause,
			CharacterPosition pos, Stack<CallStackElement> stack) {
		super(node, message, cause, pos, stack);
		// TODO Auto-generated constructor stub
	}

}
