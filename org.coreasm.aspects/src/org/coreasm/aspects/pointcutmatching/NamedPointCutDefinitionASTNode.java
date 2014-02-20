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
	
	/**
	 * get the name of the pointcut
	 * @return token as name of the pointut
	 */
	public String getName(){
		return this.getFirstASTNode().getToken();
	}
	
	/**
	 * according to the general CoreASM definition, 
	 * a definition of a named pointcut has to have a uniq name.
	 * Under that premise, a NamedPointCutASTNode belongs is defined by exactly one
	 * NamedPointCutDefinitionASTNode with the same name.
	 * @param nptc
	 * @return true, if the usage and the defintion of the named pointcut have the same name
	 */
	public boolean isDefinitionOf(NamedPointCutASTNode nptc){
		return this.getName().equals(nptc.getName());
	}
	
	/**
	 * returns the pointcut defined as direct child of this node
	 * @return BinorASTNode as the root of a maybe complex pointcut expression
	 */
	public BinOrASTNode getPointCut(){
		for(Node child : this.getChildNodes())
			if (child instanceof BinOrASTNode)
				return (BinOrASTNode)child;
		//\todo exception no pointcut (BinOrASTNode) defined by this NamedPointCutASTNode
		return null;
	}

}
