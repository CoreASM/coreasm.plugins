package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author	Marcel Dausend
 * @date	2013.02.11
 * @version	0.1 (alpha)
 * 
 * @brief	The class {@link org.coreasm.aspects.pointcutmatching.AspectASTNode} is a super type for all nodes used in the aspect-oriented CoreASM-Plugin.
 */
public class AspectASTNode extends ASTNode {

	private static final long serialVersionUID = 1L;
	/** NODE_TYPE - shortcut for class.getSimpleName() */
	private static final String NODE_TYPE = AspectASTNode.class.getSimpleName();

	public AspectASTNode(AspectASTNode self){
		super(self);
	}

	/**
	 * get the name of the aspect
	 * 
	 * @return token of first ASTNode
	 */
	public String getName() {
		String name = "";
		if (this.getFirst().getToken() != null)
			name = this.getFirst().getToken();
		return name;
	}

	/** {@inheritDoc}
	 *
	 * @param scannerInfo    necessary for super constructor
	 */
	public AspectASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, ASTNode.DECLARATION_CLASS, AspectASTNode.NODE_TYPE, null, scannerInfo);
	}

}
