/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import java.util.LinkedList;
import java.util.List;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.plugins.aspects.AoASMPlugin;

/**
 * @author marcel
 *
 */
public class NamedPointCutASTNode extends ASTNode {

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
		return this.getFirst().getFirst().getToken();
	}

	public List<FunctionRuleTermNode> getPointCutParameters(){
		List<FunctionRuleTermNode> params = new LinkedList<FunctionRuleTermNode>();
		for (ASTNode child = this.getFirst().getNext(); child != null; child = child.getNext()) {
			if (child instanceof FunctionRuleTermNode)
				params.add((FunctionRuleTermNode)child);
		}
		return params;
	}

}
