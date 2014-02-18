package org.coreasm.aspects.errorhandling;

import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.interpreter.ASTNode;

public class MatchingError extends AspectException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String pattern;

	public MatchingError(String pattern, ASTNode node, String message, Throwable cause) {
		super(node, "Wrong syntax of pattern "+pattern+"\n"+message, cause);
		this.pattern = pattern;
	}
	
	public void generateCapiError(ControlAPI capi){
		super.generateCapiError(capi);
	}
	
	/**
	 * return the faulty pattern
	 * @return
	 */
	public String getPattern(){
		return pattern;
	}

}
