/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.Iterator;

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
	private static int cflowCounter = 0;
	private final String CFLOWIDPREFIX = "_cflow_";
	int cflowId;

	/**
	 * this constructor is needed to support duplicate
	 * 
	 * @param self
	 *            this object
	 */
	public CFlowASTNode(CFlowASTNode self) {
		super(self);
		cflowCounter++;
		this.cflowId = cflowCounter;
	}

	/**
	 * @param scannerInfo
	 */
	public CFlowASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, CFlowASTNode.NODE_TYPE, null,
				scannerInfo);
		cflowCounter++;
		this.cflowId = cflowCounter;
	}

	@Override
	public Binding matches(ASTNode compareToNode) {
		FunctionRuleTermNode fnNode = (FunctionRuleTermNode) compareToNode;
		if (!AspectTools.isRuleName(fnNode.getName()))
			return new Binding(compareToNode, this);
		//if this is a child of any not node that returns no match
		ASTNode parent = this.getParent();
		while (parent != null && !(parent instanceof NotASTNode) && !"and".equals(parent.getToken()))
			parent = parent.getParent();
		if (parent instanceof NotASTNode)
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

	@Override
	public String getLetExpression() {
		String letExpression = "";
		letExpression = this.getCflowId() + " = callStackMatches( [";
		Iterator<PointCutParameterNode> it = this.getParameters().iterator();
		while (it.hasNext()) {
			PointCutParameterNode parameterNode = it.next();
			String name = parameterNode.getName();
			String pattern = parameterNode.getPattern();
			if (name == null)
				letExpression += "\"" + pattern + "\"";
			else
				letExpression += "[\"" + pattern + "\", \"" + name + "\"]";
			if (it.hasNext())
				letExpression += ", ";
		}

		letExpression += " ])";
		return letExpression;
	}

	private String getCflowId() {
		return CFLOWIDPREFIX + String.valueOf(cflowId);
	}

	@Override
	public String getCflowBindings() {
		//the let expression from <code>getLetExpression()</code> should have been used for initialization of that id.
		return this.getCflowId();

	}
}
