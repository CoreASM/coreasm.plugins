/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.ArrayList;
import java.util.HashMap;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

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
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, CFlowASTNode.NODE_TYPE, null, scannerInfo);
	}

	@Override
	public Binding matches(ASTNode compareToNode) {
		//this concept is always true before execution, because its a runtime concept
		return new Binding(compareToNode, this, new HashMap<String, ASTNode>());
	}

	@Override
	public String generateExpressionString() {
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();
		if (children.size() >= 1 && children.get(0) instanceof PointCutParameterNode)
		{
			//			AspectWeaver.getInstance().getControlAPI()
			//					.warning(AoASMPlugin.PLUGIN_NAME, "Generation of expression not implemented yed for cflow",
			//							this, AspectWeaver.getInstance().getControlAPI().getInterpreter());
			return "matchingRuleCallsInsideCallstack(" + AspectTools.getRuleSignatureAsCoreASMList(this) + ") != []";//"( "+((PointCutASTNode)children.get(0)).generateExpressionString()+" )";
		}
		else throw new CoreASMError("generation of expression failed for cflow", this);
	}
}
