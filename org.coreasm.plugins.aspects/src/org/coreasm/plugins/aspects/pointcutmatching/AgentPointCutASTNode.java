/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import java.util.HashMap;

import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMWarning;
import org.coreasm.engine.absstorage.FunctionElement.FunctionClass;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.plugins.signature.FunctionNode;
import org.coreasm.engine.plugins.string.StringBackgroundElement;
import org.coreasm.plugins.aspects.AoASMPlugin;
import org.coreasm.plugins.aspects.AspectWeaver;
import org.coreasm.plugins.aspects.errorhandling.AspectException;

/**
 * @author marcel
 * 
 */
public class AgentPointCutASTNode extends PointCutASTNode {

	private static final String NODE_TYPE = AgentPointCutASTNode.class.getSimpleName();
	private String callByAgent;

	/**
	 * this constructor is needed to support duplicate
	 * 
	 * @param self
	 *            this object
	 */
	public AgentPointCutASTNode(AgentPointCutASTNode self) {
		super(self);
	}

	/**
	 * @param scannerInfo
	 */
	public AgentPointCutASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, AgentPointCutASTNode.NODE_TYPE,
				null,
				scannerInfo);
	}

	@Override
	public Binding matches(ASTNode compareToNode) throws AspectException {
		fetchCallByAgent(this.getFirst());
		//if this is a child of any not node that returns no match
		ASTNode parent = this.getParent();
		while (parent != null && !(parent instanceof NotASTNode) && !"and".equals(parent.getToken()))
			parent = parent.getParent();
		if (parent instanceof NotASTNode)
			return new Binding(compareToNode, this);
		return new Binding(compareToNode, this, new HashMap<String, ASTNode>());
	}

	private void fetchCallByAgent(ASTNode agentPattern) {
		if (agentPattern.getGrammarRule().equals("StringTerm")) {
			callByAgent = agentPattern.getToken();
		}
		else //must be id node, so get the initial value from the definition of the static value 
		{
			ASTNode astNode;
			if (!agentPattern.getGrammarRule().equals(Kernel.GR_ID))
				throw new CoreASMError("node must be an id node", agentPattern);
			else
				astNode = agentPattern;
			// ascend up to aspect node
			while (!(astNode instanceof AspectASTNode))
				astNode = astNode.getParent();
			// iterate over signatures to find the initial string value of the
			// used id
			astNode = astNode.getFirst();//first child of aspect ast node
			do {
				if (astNode.getGrammarRule().equals("Signature") && astNode.getFirst() instanceof FunctionNode) {
					FunctionNode fn = (FunctionNode) astNode.getFirst();
					if (fn.getName().equals(agentPattern.getToken())) {
						// error: initial value of the variable is not a string
						// term
						if (!(fn.getRange().equals(StringBackgroundElement.STRING_BACKGROUND_NAME)
								&& fn.getInitNode() != null && fn.getInitNode()
								.getGrammarRule().equals("StringTerm")))
							throw new CoreASMError("Value of function " + fn.getName()
									+ " is not a string but is used as pointcut pattern.", fn);
						// warning: function is not static what is against the
						// intention of the expected (final) static string
						// declaration
						if (fn.getFunctionClass() != FunctionClass.fcStatic) {
							CoreASMWarning warn = new CoreASMWarning(AoASMPlugin.PLUGIN_NAME, "Function "
									+ fn.getName() + " is not static but used as pointcut pattern.", fn);
							AspectWeaver.getInstance().getControlAPI().warning(warn);
						}
						callByAgent = fn.getInitNode().getToken();
					}
				}
			} while ((astNode = astNode.getNext()) != null);
			if (callByAgent == null)
				throw new CoreASMError(agentPattern.getToken() + " is not declared.",
						agentPattern);
		}
	}

	/**
	 * this method should return a string which can be used to weave runtime
	 * conditions for aspect matching
	 * 
	 * @return a string which can be used to weave runtime conditions for aspect
	 *         matching
	 */
	@Override
	public String getCondition() {
		return "matches ( toString( self ) , \"" + callByAgent + "\" )";
	}

}
