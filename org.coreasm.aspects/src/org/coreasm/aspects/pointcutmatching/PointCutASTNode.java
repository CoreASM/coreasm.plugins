/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.coreasm.aspects.AspectWeaver;
import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author Marcel Dausend
 */
public abstract class PointCutASTNode extends ASTNode implements IPointCutASTNodeExpression {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = PointCutASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * 
	 * @param self
	 *            this object
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
	 * This node references a special type of itself or is either a binary
	 * operation node with 'and' or 'or'
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
				return (PointCutASTNode) node;
		}
		return null;
	}

	PointCutASTNode getSecondChild() {
		boolean skippedFirst = false;
		for (ASTNode node : getAbstractChildNodes()) {
			if (node instanceof PointCutASTNode && skippedFirst)
				return (PointCutASTNode) node;
			else if (node instanceof PointCutASTNode && skippedFirst)
				return (PointCutASTNode) node;
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
			parameters.add((PointCutParameterNode) node);
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
		return "";
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
	 * return the letExpression of the child(ren) or the empty String. This is
	 * used for the implementation of cflow
	 * 
	 * @return element of a CoreASM let expression
	 */
	public String getLetExpression() {
		String letExpression = "";
		List<ASTNode> children = this.getAbstractChildNodes();
		if (children.size() == 1 && children.get(0) instanceof PointCutASTNode)
			letExpression = ((PointCutASTNode) children.get(0)).getLetExpression();
		else if (children.size() == 2 && children.get(0) instanceof PointCutASTNode
				&& children.get(1) instanceof PointCutASTNode) {
			String leftChild = ((PointCutASTNode) children.get(0)).getLetExpression();
			String rightChild = ((PointCutASTNode) children.get(1)).getLetExpression();

			if (!leftChild.isEmpty() && !rightChild.isEmpty())
				letExpression = leftChild + ", " + rightChild;
			else
				letExpression = leftChild + rightChild;
		}
		return letExpression;
	}

	/**
	 * return the dynamic bindings extracted from the call stack according to
	 * the cflow expressions of the pointcut. The consistency of bindings has to
	 * be taken into account in case of BinAnd.
	 * 
	 * @return union of tuples of bindings
	 */
	public String getCflowBindings() {
		String cflowCondotion = "";
		List<ASTNode> children = this.getAbstractChildNodes();
		if (children.size() == 1 && children.get(0) instanceof PointCutASTNode)
			cflowCondotion = ((PointCutASTNode) children.get(0)).getCflowBindings();
		else if (children.size() == 2 && children.get(0) instanceof PointCutASTNode
				&& children.get(1) instanceof PointCutASTNode) {
			String leftChild = ((PointCutASTNode) children.get(0)).getCflowBindings();
			String rightChild = ((PointCutASTNode) children.get(1)).getCflowBindings();

			if (!leftChild.isEmpty() && !rightChild.isEmpty())
				cflowCondotion = leftChild + " + " + rightChild;
			else
				cflowCondotion = leftChild + rightChild;
		}
		return cflowCondotion;
	}

	/**
	 * returns the type of the PointCutNodeElement
	 * 
	 * @return
	 */
	public static String getNodeType() {
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
