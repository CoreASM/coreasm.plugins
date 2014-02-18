/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author Marcel Dausend
 *
 */
public class BinAndASTNode extends PointCutASTNode {

	private static final String NODE_TYPE = BinAndASTNode.class.getSimpleName();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public BinAndASTNode(BinAndASTNode self){
		super(self);
	}
	/**
	 * @param scannerInfo
	 */
	public BinAndASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, BinAndASTNode.NODE_TYPE, null, scannerInfo);
	}
	
	@Override
	public PointCutMatchingResult matches(ASTNode compareToNode) throws Exception {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		//just one node which must be a ExpressionASTNode according to the grammar;
		//return the result of the child node.
		if (children.size()==1 && this.getFirstChild() instanceof ExpressionASTNode)
			return this.getFirstChild().matches(compareToNode);
		//exactly two nodes: if one of those nodes returns 'true', this node returns 'true', too.
		else if (children.size()==2 && 
				this.getFirstChild() instanceof ExpressionASTNode && 
				this.getSecondChild() instanceof BinAndASTNode)
			{
				PointCutMatchingResult firstChildResult, secondChildResult;
				firstChildResult = this.getFirstChild().matches(compareToNode);
				secondChildResult = this.getSecondChild().matches(compareToNode);
				boolean result = (firstChildResult.getBoolean() && secondChildResult.getBoolean());
				LinkedList<ArgsASTNode> listOfArgs = new LinkedList<ArgsASTNode>();
				listOfArgs.addAll(firstChildResult.getArgsASTNodes());
				listOfArgs.addAll(secondChildResult.getArgsASTNodes());
				return new PointCutMatchingResult(result, listOfArgs);
			}
		else
			return new PointCutMatchingResult(false, new LinkedList<ArgsASTNode>());
	}

	@Override
	public String generateExpressionString() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		//just one node which must be a ExpressionASTNode according to the grammar;
		//return the result of the child node.
		if (children.size()==1 && children.get(0) instanceof ExpressionASTNode)
			return ((ExpressionASTNode)children.get(0)).generateExpressionString();
		//exactly two nodes: if one of those nodes returns 'true', this node returns 'true', too.
		else if (children.size()==2 && 
				children.get(0) instanceof ExpressionASTNode && 
				children.get(1) instanceof BinAndASTNode)
			return ((ExpressionASTNode)children.get(0)).generateExpressionString()+" and "+
					((BinAndASTNode)children.get(1)).generateExpressionString();
		else throw new CoreASMError("generation of espression failed", this);
	}
}
