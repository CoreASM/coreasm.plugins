/**
 * AoASM CoreASM Plugin
 * 
 * @author Marcel Dausend
 */
package org.coreasm.aspects.errorhandling;

import java.util.Stack;

import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter.CallStackElement;
import org.coreasm.engine.parser.CharacterPosition;

public abstract class AspectException extends CoreASMError {

	/**
	 * private field that specifies this exception
	 */
	//@{
	private static final long serialVersionUID = 1L;
	//@}

	/**
	 * creates a new aspect exception for a given node, cause, and message
	 * 
	 * @param node
	 * @param message
	 */
	public AspectException(ASTNode node, String message ) {
		super(message, node);
	}
	
	/**
	 * creates a new aspect exception for runtime issues
	 * that refers to a node, text position, and the current call stack.
	 * 
	 * @param node
	 * @param message
	 * @param cause
	 * @param pos
	 * @param stack
	 */
	public AspectException(ASTNode node, String message, Throwable cause, CharacterPosition pos, Stack<CallStackElement> stack ){
		super(message, cause, pos, stack, node);
	}
}
