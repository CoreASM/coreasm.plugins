/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.AspectWeaver;
import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMWarning;
import org.coreasm.engine.absstorage.FunctionElement.FunctionClass;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.plugins.signature.FunctionNode;
import org.coreasm.engine.plugins.string.StringBackgroundElement;

/**
 * @author Marcel Dausend
 */
public abstract class PointCutASTNode extends ASTNode implements IPointCutASTNodeExpression{

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = PointCutASTNode.class.getSimpleName();

	private String callByAgent;

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public PointCutASTNode(PointCutASTNode self) {
		super(self);
	}
	
	/**
	 * 
	 * @param pluginName
	 * @param grammarClass
	 * @param grammarRule
	 * @param token
	 * @param scannerInfo
	 */
	PointCutASTNode(String pluginName, String grammarClass,
					String grammarRule, String token, ScannerInfo scannerInfo) {
		super(pluginName, grammarClass, grammarRule, token, scannerInfo);
	}
	/**
	 * This node references a special type of itself or is either a binary operation node with 'and' or 'or'
	 * 
	 *
     * @param compareToNode
     * @return
	 * @throws AspectException 
	 */
	@Override
	public abstract Binding matches(ASTNode compareToNode) throws AspectException;
	
	/**
	 * @return
	 */
	PointCutASTNode getFirstChild() {
		for (ASTNode node : getAbstractChildNodes()) {
			if (node instanceof PointCutASTNode)
				return (PointCutASTNode)node;
		}
		return null;
	}
	
	PointCutASTNode getSecondChild() {
		boolean skippedFirst = false;
		for (ASTNode node : getAbstractChildNodes()) {
			if (node instanceof PointCutASTNode && skippedFirst)
				return (PointCutASTNode)node;
			else if (node instanceof PointCutASTNode && skippedFirst)
				return (PointCutASTNode)node;
			else if (node instanceof PointCutASTNode)
				skippedFirst = true;
		}
		return null;
	}
	
	public LinkedList<PointCutParameterNode> getParameters() {
		LinkedList<PointCutParameterNode> parameters = new LinkedList<PointCutParameterNode>();
		collectParameters(this, parameters);
		return parameters;
	}
	
	private static void collectParameters(ASTNode node, LinkedList<PointCutParameterNode> parameters) {
		if (node instanceof PointCutParameterNode)
			parameters.add((PointCutParameterNode)node);
		for (ASTNode child : node.getAbstractChildNodes())
			collectParameters(child, parameters);
	}

	@Override
	public HashSet<String> getLocalIds() {
		HashSet<String> localIds = new HashSet<String>();
		for (PointCutParameterNode param : this.getParameters()) {
			if (param.getName() != null)
				localIds.add(param.getName());
		}
		return localIds;
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
		if (callByAgent != null)
			return "matches ( toString( self ) , \"" + callByAgent + "\" )";
		return "true";
	}
	
	protected void fetchCallByAgent(Node node) {
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
	}

	/**
	 * this method should lookup if the found rule returns any value and check
	 * if this corresponds to the this pointcut expression
	 * 
	 * @param node
	 *            current node
	 * @param compareToNodeToken
	 *            token is used to find matching rule declaration
	 * @return returns true if the rule definition of the rule which name
	 *         pointcut for return matches the rule's return value, if exists
	 */
	protected boolean checkRuleReturn(Node node, String compareToNodeToken) {
		while (node != null && !(node.getConcreteNodeType().equals("keyword") && node.getToken().startsWith("with")))
			node = node.getNextCSTNode();
		if (node == null)
			return true;
		String ruleDeclarationName;
		for (ASTNode astn : AspectWeaver.getInstance().getAstNodes().get("RuleDeclaration")) {
			ruleDeclarationName = astn.getFirst().getFirst().getToken();
			//check if the current rule declaration is the one with the name of the searching one
			if (Pattern.matches(compareToNodeToken, ruleDeclarationName))
			{
				//collect all nodes inside the rule definition which have the same token as the returnOrResult node
				LinkedList<Node> children = AspectTools.getNodesWithName(astn, node.getNextCSTNode().getToken());
				//return xor of returnRequired and children.isEmpty()
				return node.getToken().equals("with") ^ children.isEmpty();
			}
		}
		//no rule declaration found - should not occur, because its  has already been tested if compareToNodeToken exists inside the matches(..) method
		return false;
	}

	/**
	 * returns the type of the PointCutNnodeElement
	 * 
	 * @return
	 */
	public static String getNodeType(){
		return NODE_TYPE;
	}
	
	/**
	 * 
	 * @return
	 */
	public AdviceASTNode getAdvice() {
		ASTNode node = this;
		while (node != null && !(node instanceof AdviceASTNode))
			node = node.getParent();
		if (node != null)
			return (AdviceASTNode) node;
		throw new CoreASMError("Node is not a child of any AdviceASTNode!", this);
	}

	/**
	 * 
	 * @return
	 */
	public AspectASTNode getAspect() {
		ASTNode node = this;
		while (node != null && !(node instanceof AspectASTNode))
			node = node.getParent();
		if (node != null)
			return (AspectASTNode) node;
		throw new CoreASMError("Node is not a child of any AspectASTNode!", this);
	}

}
