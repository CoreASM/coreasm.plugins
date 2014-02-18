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
public class BinOrASTNode extends PointCutASTNode {
	
	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = BinOrASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public BinOrASTNode(BinOrASTNode self){
		super(self);
	}
	
	/**
	 * @param scannerInfo
	 */
	public BinOrASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, BinOrASTNode.NODE_TYPE, null, scannerInfo);
	}
	
	@Override
	public PointCutMatchingResult matches(ASTNode compareToNode) throws Exception {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		//just one node which must be a BinAndASTNode according to the grammar;
		//return the result of the child node.
		if (children.size()==1 && this.getFirstChild() instanceof BinAndASTNode)
			return this.getFirstChild().matches(compareToNode);
		//exactly two nodes: if one of those nodes returns 'true', this node returns 'true', too.
		else if (children.size()==2 && 
				this.getFirstChild() instanceof BinAndASTNode && 
				this.getSecondChild() instanceof BinOrASTNode)
			{
				PointCutMatchingResult firstChildResult, secondChildResult;
				firstChildResult = this.getFirstChild().matches(compareToNode);
				secondChildResult = this.getSecondChild().matches(compareToNode);
				boolean result = (firstChildResult.getBoolean() || secondChildResult.getBoolean());
				LinkedList<ArgsASTNode> listOfArgs = new LinkedList<ArgsASTNode>();
				if (firstChildResult.getBoolean()) listOfArgs.addAll(firstChildResult.getArgsASTNodes());
				if (secondChildResult.getBoolean()) listOfArgs.addAll(secondChildResult.getArgsASTNodes());
				return new PointCutMatchingResult(result, listOfArgs);
		}
		else
			return new PointCutMatchingResult(false, new LinkedList<ArgsASTNode>());
	}
	
	@Override
	public String generateExpressionString() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		if (children.size()==1 && children.get(0) instanceof BinAndASTNode)
			return ((BinAndASTNode)children.get(0)).generateExpressionString();
		else if (children.size()==2 && 
				children.get(0) instanceof BinAndASTNode && 
				children.get(1) instanceof BinOrASTNode)
		return ((BinAndASTNode)children.get(0)).generateExpressionString()+" or "+
				((BinOrASTNode)children.get(1)).generateExpressionString();
		else throw new CoreASMError("generation of espression failed", this);
	}

}
