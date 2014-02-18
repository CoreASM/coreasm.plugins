/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author Marcel Dausend
 *
 */
public class NotASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = NotASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public NotASTNode(NotASTNode self){
		super(self);
	}
	
	/**
	 * @param scannerInfo
	 */
	public NotASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, NotASTNode.NODE_TYPE, null, scannerInfo);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * returns the inverted boolean value of its subexpression node
	 * @throws Exception 
	 */
	@Override
	public PointCutMatchingResult matches(ASTNode compareToNode) throws Exception {
		PointCutMatchingResult result = this.getFirstChild().matches(compareToNode);
		//TODO not for args nodes - what is an inverted return value of args list?!
		//\attention argslist not yed inverted!!!
		return new PointCutMatchingResult(! result.getBoolean(), result.getArgsASTNodes());
	}

	@Override
	public String generateExpressionString() {
		return "not ( "+ this.getFirstChild().generateExpressionString() +" )";
	}
}
