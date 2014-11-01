/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import java.util.ArrayList;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.plugins.aspects.AoASMPlugin;
import org.coreasm.plugins.aspects.errorhandling.AspectException;
import org.coreasm.plugins.aspects.utils.AspectTools;

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
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, BinOrASTNode.NODE_TYPE, null, scannerInfo);
	}
	
	@Override
	public Binding matches(ASTNode compareToNode) throws AspectException {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>) this.getAbstractChildNodes();
		//just one node which must be a BinAndASTNode according to the grammar;
		//return the result of the child node.
		if (children.size() == 1)
			if (this.getFirstChild() instanceof BinAndASTNode)
				return this.getFirstChild().matches(compareToNode);
			else if (this.getFirstChild().getGrammarRule().equals(AspectTools.RULESIGNATURE))
				//exchange with pointcut from namedpointcut
				return new Binding(compareToNode, this);
			else
				//error!!! \todo errorhandling
				return new Binding(compareToNode, this);
		//exactly two nodes: if one of those nodes returns 'true', this node returns 'true', too.
		else if (children.size() == 2 &&
				this.getFirstChild() instanceof BinAndASTNode &&
				this.getSecondChild() instanceof BinOrASTNode)
		{
			Binding firstChildBinding, secondChildBinding;
			firstChildBinding = this.getFirstChild().matches(compareToNode);
			secondChildBinding = this.getSecondChild().matches(compareToNode);

			//todo null check for resulting bindings of children
			if (firstChildBinding.exists())
				return new Binding(compareToNode, this, firstChildBinding.getBinding());
			else if (secondChildBinding.exists())
				return new Binding(compareToNode, this, secondChildBinding.getBinding());
			return new Binding(compareToNode, this);
		}
		else
			return new Binding(compareToNode, this);
	}
	
	@Override
	public String getCondition() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		if (children.size() == 1 && children.get(0) instanceof BinAndASTNode
				&& !((BinAndASTNode) children.get(0)).getCondition().isEmpty())
			return ((BinAndASTNode) children.get(0)).getCondition();
		else if (children.size()==2 && 
				children.get(0) instanceof BinAndASTNode && 
				children.get(1) instanceof BinOrASTNode){
			String leftChild = ((BinAndASTNode) children.get(0)).getCondition();
			String rightChild = ((BinOrASTNode) children.get(1)).getCondition();

			if (!leftChild.isEmpty() && !rightChild.isEmpty())
				return leftChild + " or " + rightChild;
			else
				return leftChild + rightChild;
		}
		return "";
	}

	@Override
	public String getCflowBindings() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>) this.getAbstractChildNodes();
		//just one node which must be a ExpressionASTNode according to the grammar;
		//return the result of the child node.
		if (children.size() == 1 && children.get(0) instanceof BinAndASTNode
				&& !((BinAndASTNode) children.get(0)).getCflowBindings().isEmpty())
			return ((BinAndASTNode) children.get(0)).getCflowBindings();
		//exactly two nodes: if one of those nodes returns 'true', this node returns 'true', too.
		else if (children.size() == 2 &&
				children.get(0) instanceof BinAndASTNode &&
				children.get(1) instanceof BinOrASTNode) {
			String leftChild = ((BinAndASTNode) children.get(0)).getCflowBindings();
			String rightChild = ((BinOrASTNode) children.get(1)).getCflowBindings();

			if (!leftChild.isEmpty() && !rightChild.isEmpty())
				return "OrBinding( "
						+ leftChild + ", " + rightChild
						+ " )";
			else
				return leftChild + rightChild;
		}
		return "";
	}

}
