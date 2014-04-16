/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashSet;

import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.engine.interpreter.ASTNode;

/**
 * @author Marcel Dausend
 *
 */
public interface IPointCutASTNodeExpression {
	
	/**
	 * returns the condition for the pointcut node which must be checked before
	 * executing the corresponding advice at runtime
	 * 
	 * @return guard condition
	 */
	public String getCondition();

	/**
	 * The tokens of local ids which are used to implement dynamically assigned
	 * bindings, e.g. for cflow
	 * 
	 * @return set of ids for local definitions
	 */
	public HashSet<String> getLocalIds();

	Binding matches(ASTNode compareToNode) throws AspectException;
	
}
