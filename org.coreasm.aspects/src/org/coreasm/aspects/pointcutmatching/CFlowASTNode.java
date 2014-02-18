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
public class CFlowASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = CFlowASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public CFlowASTNode(CFlowASTNode self){
		super(self);
	}
	
	/**
	 * @param scannerInfo
	 */
	public CFlowASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, CFlowASTNode.NODE_TYPE, null, scannerInfo);
	}

	@Override
	public PointCutMatchingResult matches(ASTNode compareToNode) {
		//this concept is always true before execution, because its a runtime concept
		return new PointCutMatchingResult(true, new LinkedList<ArgsASTNode>());
	}

	@Override
	public String generateExpressionString() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		if (children.size()==1 && children.get(0) instanceof PointCutASTNode)
		{
			return "( "+((PointCutASTNode)children.get(0)).generateExpressionString()+" )";
		}
		else throw new CoreASMError("generation of expression failed for cflow", this);
	}
}
