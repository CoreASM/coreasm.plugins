/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.AspectTools;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author marcel
 *
 */
public class NamedPointCutASTNode extends PointCutASTNode {

	public static final String NODE_TYPE = NamedPointCutASTNode.class.getSimpleName();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public NamedPointCutASTNode(NamedPointCutASTNode self){
		super(self);
	}

	public NamedPointCutASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, Node.OTHER_NODE, NamedPointCutASTNode.NODE_TYPE, null, scannerInfo);
	}

	/**
	 * get the name of the pointcut
	 * @return token as name of the pointut
	 */
	public String getName(){
		return this.getFirstASTNode().getFirstASTNode().getToken();
	}
	
	@Override
	public Binding matches(ASTNode compareToNode) throws Exception {
		throw new MatchingError("no pattern for matching", this, "NamedPointcut should not occcur during matching",null);
	}
	
	@Override
	public String generateExpressionString() {
		return AspectTools.node2String(this);		
	}

}
