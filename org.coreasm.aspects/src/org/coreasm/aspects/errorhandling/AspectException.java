/**
 * AoASM CoreASM Plugin
 * 
 * @author Marcel Dausend
 */
package org.coreasm.aspects.errorhandling;

import java.util.Stack;

import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter.CallStackElement;
import org.coreasm.engine.parser.CharacterPosition;

public abstract class AspectException extends Exception {

	/**
	 * private field that specifies this exception
	 */
	//@{
	private static final long serialVersionUID = 1L;
	private ASTNode astNode;
	private CharacterPosition position;
	private Stack<CallStackElement> stack;
	//@}

	/**
	 * creates a new aspect exception for a given node, cause, and message
	 * 
	 * @param node
	 * @param message
	 * @param cause
	 */
	public AspectException(ASTNode node, String message, Throwable cause ) {
		super(message, cause);
		this.astNode = node;
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
		super(message, cause);
		this.astNode = node;
		this.position = pos;
		this.stack = stack;
	}

	/**
	 * @return the ASTNode
	 */
	public ASTNode getASTNode() {
		return astNode;
	}

	/**
	 * @return the position
	 */
	public CharacterPosition getPosition() {
		return position;
	}

	/**
	 * @return the stack
	 */
	public Stack<CallStackElement> getStack() {
		return stack;
	}

	/**
	 * creates a new generic error for the given capi
	 * 
	 * @param capi
	 */
	public void generateCapiError(ControlAPI capi){
		capi.error(new CoreASMError(this.getMessage(), this.getCause(), this.getPosition(), this.getStack(), this.getASTNode()));
	}
	
}
