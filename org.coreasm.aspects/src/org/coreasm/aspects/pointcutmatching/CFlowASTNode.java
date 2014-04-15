/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.PatternSyntaxException;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.AspectWeaver;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMWarning;
import org.coreasm.engine.absstorage.FunctionElement.FunctionClass;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.plugins.signature.FunctionNode;
import org.coreasm.engine.plugins.string.StringBackgroundElement;

/**
 * @author Marcel Dausend
 * 
 */
public class CFlowASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = CFlowASTNode.class.getSimpleName();
	private String callByAgent = "";

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
		Iterator<ASTNode> argIterator = fnNode.getArguments().iterator();
		String pointCutToken = null;
		Binding resultingBinding = null;
		Node node;
		ASTNode astn = fnNode.getFirst();
		// \todo add bindings
		//step through all children of the call pointcut call ( regEx4name by regEx4agentOrUnivers with||without return||result )
		for (node = this.getFirstCSTNode(); node != null && astn != null; node = node.getNextCSTNode()) {
			if (node instanceof PointCutParameterNode) {
				//check if the name/regEx of the pointcut matches the compareToNode
				PointCutParameterNode parameterNode = (PointCutParameterNode) node;
				//get pointcut's token
				pointCutToken = parameterNode.getPattern();

				//compare the token of the given node with this node's token by using regular expressions
				//if a string is given instead of an id node, the regular expression has to be generated
				try {
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
				catch (PatternSyntaxException e) {
					//if the pointcut token is no regular expression throw an exception towards the weaver
					throw new MatchingError(pointCutToken, this, e.getMessage());
				}
				astn = (argIterator.hasNext() ? argIterator.next() : null);
			}
		}

		// find next ASTNode
		Node parameterNode = node;
		while (parameterNode != null && !(parameterNode instanceof ASTNode))
			parameterNode = parameterNode.getNextCSTNode();

		while (node != null) {
			if (node.getConcreteNodeType().equals("keyword") && node.getToken().equals("by")) {
				ASTNode agentPattern = (ASTNode) node.getNextCSTNode();
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
				}
			}
			node = node.getNextCSTNode();
		}
		return resultingBinding;
	}

	@Override
	public String generateExpressionString() {
		String condition = "";
		ArrayList<ASTNode> children = (ArrayList<ASTNode>) this.getAbstractChildNodes();
		if (children.size() >= 1 && children.get(0) instanceof PointCutParameterNode)
		{
			//			AspectWeaver.getInstance().getControlAPI()
			//					.warning(AoASMPlugin.PLUGIN_NAME, "Generation of expression not implemented yed for cflow",
			//							this, AspectWeaver.getInstance().getControlAPI().getInterpreter());
			condition = "matchingRuleCallsInsideCallstack(" + AspectTools.getRuleSignatureAsCoreASMList(this)
					+ ") != []";//"( "+((PointCutASTNode)children.get(0)).generateExpressionString()+" )";
			//if the context of the call has been restricted to an specific agent
			if (!callByAgent.isEmpty())
			{
				condition += " and matches ( toString( self ) , \"" + callByAgent + "\" )";
			}
		}
		else
			throw new CoreASMError("generation of expression failed for cflow", this);
		return condition;
	}
}
