/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author Marcel Dausend
 *
 */
public class NamedPointCutDefinitionASTNode extends ASTNode {

	private static final long serialVersionUID = 1L;

	public static final String NODE_TYPE = NamedPointCutDefinitionASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public NamedPointCutDefinitionASTNode(NamedPointCutDefinitionASTNode self){
		super(self);
	}

	public NamedPointCutDefinitionASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, Node.OTHER_NODE, NamedPointCutDefinitionASTNode.NODE_TYPE, null, scannerInfo);
	}

}
