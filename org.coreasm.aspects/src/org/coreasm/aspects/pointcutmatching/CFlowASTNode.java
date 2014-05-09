/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
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
	 * 
	 * @param self
	 *            this object
	 */
	public CFlowASTNode(CFlowASTNode self) {
		super(self);
	}

	/**
	 * @param scannerInfo
	 */
	public CFlowASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, CFlowASTNode.NODE_TYPE, null,
				scannerInfo);
	}

	@Override
	public Binding matches(ASTNode compareToNode) {
		FunctionRuleTermNode fnNode = (FunctionRuleTermNode) compareToNode;
		if (!AspectTools.isRuleName(fnNode.getName()))
			return new Binding(compareToNode, this);
		Binding resultingBinding = null;
		Node node;
		// \todo add bindings
		//step through all children of the call pointcut call ( regEx4name by regEx4agentOrUnivers with||without return||result )
		for (node = this.getFirstCSTNode(); node != null; node = node.getNextCSTNode()) {
			if (node instanceof PointCutParameterNode) {
				//check if the name/regEx of the pointcut matches the compareToNode
				PointCutParameterNode parameterNode = (PointCutParameterNode) node;
				//compare the token of the given node with this node's token by using regular expressions
				//if a string is given instead of an id node, the regular expression has to be generated
				String name = parameterNode.getName();
				if (name == null) {
					if (resultingBinding == null)
						resultingBinding = new Binding(compareToNode, this, new HashMap<String, ASTNode>());
				}
				else {
					if (resultingBinding == null)
						resultingBinding = new Binding(compareToNode, this);
					resultingBinding.addBinding(name, null);
				}
			}
		}

		fetchCallByAgent(node);
		return resultingBinding;
	}
}
