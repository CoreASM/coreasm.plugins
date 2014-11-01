/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.plugins.aspects.AoASMPlugin;

/**
 * @author Marcel Dausend
 *
 */
public class CFlowBelowASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = CFlowBelowASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public CFlowBelowASTNode(CFlowBelowASTNode self){
		super(self);
	}
	
	/**
	 * @param scannerInfo
	 */
	public CFlowBelowASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, CFlowBelowASTNode.NODE_TYPE, null, scannerInfo);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Binding matches(ASTNode compareToNode) {
		return new Binding(compareToNode, this);
	}
}
