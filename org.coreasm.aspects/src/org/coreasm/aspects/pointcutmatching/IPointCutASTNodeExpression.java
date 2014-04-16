/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.LinkedList;

import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.engine.interpreter.ASTNode;

/**
 * @author Marcel Dausend
 *
 */
public interface IPointCutASTNodeExpression {
	
	//all expressions of a pointcut object are collected inside this hashmap
	static final HashMap <String, LinkedList<ASTNode>> expressions = new HashMap<String, LinkedList<ASTNode>>();
			
	/**
	 * adds the expression to the expressions hashmap if not already included
	 * implemented in superclass PointCutASTNode
	 * 
	 * @param candidate
	 * @param expression
	 */
	public String getCondition();

	public void addExpression(ASTNode candidate, String expression);
	
	/**
	 * should return the hashmap of expressions and their source nodes
	 * implemented in superclass PointCutASTNode
	 * 
	 * @return
	 */
	public HashMap <String, LinkedList<ASTNode>> getExpressions();
	
	/**
	 * The tokens of local ids which are used to implement dynamically assigned
	 * bindings, e.g. for cflow
	 * 
	 * @return set of ids for local definitions
	 */

	Binding matches(ASTNode compareToNode) throws AspectException;
	
	
}
