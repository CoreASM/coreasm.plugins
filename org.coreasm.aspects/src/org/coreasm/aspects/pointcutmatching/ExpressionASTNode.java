/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

import java.util.ArrayList;

/**
 * @author Marcel Dausend
 * 
 */
public class ExpressionASTNode extends PointCutASTNode {
	
	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = ExpressionASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public ExpressionASTNode(ExpressionASTNode self){
		super(self);
	}
	
	/**
	 * @param scannerInfo
	 */
	public ExpressionASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, ExpressionASTNode.NODE_TYPE, null, scannerInfo);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Binding matches(ASTNode compareToNode) throws Exception {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		//just one node which must be a BinAndASTNode according to the grammar;
		//return the result of the child node.
		if (children.size()==1 && children.get(0) instanceof PointCutASTNode)
			return ((PointCutASTNode)children.get(0)).matches(compareToNode);
		else return new Binding(compareToNode, this);
	}

	@Override
	public String generateExpressionString() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		if (this.getChildNodes().size()>1 && children.get(0) instanceof PointCutASTNode)
			return "("+((PointCutASTNode)children.get(0)).generateExpressionString()+")";
		else if (this.getChildNodes().size()==1 && children.get(0) instanceof PointCutASTNode)
			return ((PointCutASTNode)children.get(0)).generateExpressionString();
		else throw new CoreASMError("generation of expression failed", this);
	}

}
