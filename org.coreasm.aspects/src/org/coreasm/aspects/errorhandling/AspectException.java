/**
 * 
 */
package org.coreasm.aspects.errorhandling;

import java.util.Stack;

import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter.CallStackElement;
import org.coreasm.engine.parser.CharacterPosition;

/**
 * @author marcel
 *
 */
public abstract class AspectException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ASTNode astNode;
	private CharacterPosition position;
	private Stack<CallStackElement> stack;

	/**
	 * 
	 */
	public AspectException(ASTNode node, String message, Throwable cause ) {
		super(message, cause);
		this.astNode = node;
	}
	
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

	public void generateCapiError(ControlAPI capi){
		capi.error(new CoreASMError(this.getMessage(), this.getCause(), this.getPosition(), this.getStack(), this.getASTNode()));
	}
	
}
