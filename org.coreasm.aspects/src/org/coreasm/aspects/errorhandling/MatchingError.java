package org.coreasm.aspects.errorhandling;

import org.coreasm.engine.interpreter.ASTNode;

public class MatchingError extends AspectException {

	/**
	 * final id
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the pattern, that is involved in the matching error
	 */
	private String pattern;

	/**
	 * creates an matching error for the given pattern and node,
	 * which is compared with to the pattern.
	 * 
	 * @param pattern
	 * @param node
	 * @param message
	 * @param cause
	 */
	public MatchingError(String pattern, ASTNode node, String message, Throwable cause) {
		super(node, "Wrong syntax of pattern "+pattern+"\n"+message, cause);
		this.pattern = pattern;
	}
	
	/**
	 * return the maybe faulty pattern
	 * 
	 * @return
	 */
	public String getPattern(){
		return pattern;
	}

}
