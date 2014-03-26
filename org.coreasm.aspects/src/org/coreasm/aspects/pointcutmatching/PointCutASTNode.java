/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.LinkedList;

import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author Marcel Dausend
 */
public abstract class PointCutASTNode extends ASTNode implements IPointCutASTNodeExpression{

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = PointCutASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public PointCutASTNode(PointCutASTNode self) {
		super(self);
	}
	
	/**
	 * 
	 * @param pluginName
	 * @param grammarClass
	 * @param grammarRule
	 * @param token
	 * @param scannerInfo
	 */
	PointCutASTNode(String pluginName, String grammarClass,
					String grammarRule, String token, ScannerInfo scannerInfo) {
		super(pluginName, grammarClass, grammarRule, token, scannerInfo);
	}
	/**
	 * This node references a special type of itself or is either a binary operation node with 'and' or 'or'
	 * 
	 *
     * @param compareToNode
     * @return
	 * @throws AspectException 
	 */
	public abstract Binding matches(ASTNode compareToNode) throws AspectException;
	
	/**
	 * @return
	 */
	PointCutASTNode getFirstChild() {
		for (ASTNode node : getAbstractChildNodes()) {
			if (node instanceof PointCutASTNode)
				return (PointCutASTNode)node;
		}
		return null;
	}
	
	PointCutASTNode getSecondChild() {
		boolean skippedFirst = false;
		for (ASTNode node : getAbstractChildNodes()) {
			if (node instanceof PointCutASTNode && skippedFirst)
				return (PointCutASTNode)node;
			else if (node instanceof PointCutASTNode && skippedFirst)
				return (PointCutASTNode)node;
			else if (node instanceof PointCutASTNode)
				skippedFirst = true;
		}
		return null;
	}
	
	public LinkedList<PointCutParameterNode> getParameters() {
		LinkedList<PointCutParameterNode> parameters = new LinkedList<PointCutParameterNode>();
		collectParameters(this, parameters);
		return parameters;
	}
	
	private static void collectParameters(ASTNode node, LinkedList<PointCutParameterNode> parameters) {
		if (node instanceof PointCutParameterNode)
			parameters.add((PointCutParameterNode)node);
		for (ASTNode child : node.getAbstractChildNodes())
			collectParameters(child, parameters);
	}

	/**
	 *  returns null, because this node holds no expression
	 *   
	 * @return a this method should return a string which can be used to weave runtime conditions for aspect matching
	 */
	public abstract String generateExpressionString();
	
	/**
	 * returns the type of the PointCutNnodeElement
	 * 
	 * @return
	 */
	public static String getNodeType(){
		return NODE_TYPE;
	}
	
	/**
	 * should contain the string representation of the node kind, e.g. "PointCutASTNode" 
	 */
	
	/**
	 * hashmap containing string representation of expressions and their source PointCutASTNodes
	 */
	private static HashMap <String, LinkedList<ASTNode>> expressions;
	
	/**
	 * adds the expression to the expressions static hashmap of PointCutASTNode if not already included
	 * 
	 * @param expression
	 * @param candidate
	 */
	public void addExpression(ASTNode candidate, String expression){
		if (expression!=null && this!=null){
			LinkedList<ASTNode> nodesWithSameExpression=new LinkedList<ASTNode>();
			
			if (PointCutASTNode.expressions.containsKey(expression))
				nodesWithSameExpression=PointCutASTNode.expressions.get(expression);
			if (!nodesWithSameExpression.contains(this))
				nodesWithSameExpression.add(this);
			if(!nodesWithSameExpression.isEmpty())
			PointCutASTNode.expressions.put(expression, nodesWithSameExpression);
		}
	}
	
	/**
	 * returns the hashmap of expressions and their source nodes
	 * 
	 * @return
	 */
	public HashMap <String, LinkedList<ASTNode>> getExpressions(){
		return PointCutASTNode.expressions;
	}

}
