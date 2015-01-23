/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jparsec.Parser;
import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMWarning;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.MacroCallRuleNode;
import org.coreasm.engine.kernel.RuleOrFuncElementNode;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugins.caserule.CaseRuleNode;
import org.coreasm.engine.plugins.number.NumberPlugin;
import org.coreasm.plugins.aspects.AoASMPlugin;
import org.coreasm.plugins.aspects.errorhandling.AspectException;
import org.coreasm.plugins.aspects.errorhandling.BindingException;
import org.coreasm.plugins.aspects.utils.AspectTools;

/**
 * @author Marcel Dausend
 *
 */
public class AdviceASTNode extends ASTNode {
	
	private static final long serialVersionUID = 2L;
	public static final String NODE_TYPE = AdviceASTNode.class.getSimpleName();

	private static int numberOfAdviceNodes=0;
	private final String adviceId;
	private String realName;/*TODO String realName maybe useful for debugging*/
	
	private HashMap<ASTNode, Binding> bindings = new HashMap<ASTNode, Binding>();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public AdviceASTNode(AdviceASTNode self){
		super(self);
		adviceId=self.adviceId;
		realName=self.realName;
	}
	
	public AdviceASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, ASTNode.DECLARATION_CLASS, AdviceASTNode.NODE_TYPE, null, scannerInfo);
		adviceId = "adv".concat(Integer.toString(numberOfAdviceNodes));
		numberOfAdviceNodes++;
	}

	public static void resetAdviceCounter(){
		numberOfAdviceNodes=0;
	}
	
	/**
	 * Override the advice name in the identifier with the generic unique value
	 * adviceName!
	 */
	@Override
	public void addChild(String name, Node node) {
		super.addChild(name, node);

		//real name cannot be set, when e.g. using cloneTree, because children do not exist at this time
		if (node instanceof ASTNode && realName == null) {
			this.realName = this.getName();
			//this.getFirst().getFirst().setToken(adviceId);
		}
	}

	/**
	 * returns the boolean value of the pointcut expression of this advice
	 * and stores successful bindings in the bindings HashMap of the advice
	 * 
	 * @param candidate
	 *            given pointcut of the advice
	 * @return result of the matching
	 * @throws AspectException
	 */
	public Binding matches(ASTNode candidate) throws AspectException{
		//pointcut cannot be null (which is assured by parsing)
		Binding binding = getPointCut().matches(candidate);
		/*	stores the binding in the bindings HashMap
			where the key is the node that successfully matches this advice */
		if (binding.exists()) {
			if ("around".equals(getLocator())) {
				FunctionRuleTermNode fnNode = (FunctionRuleTermNode)candidate;
				ASTNode astn = candidate.getFirst();
				RuleOrFuncElementNode ruleOrFuncElemNode = new RuleOrFuncElementNode(astn.getScannerInfo());
				AspectTools.addChild(ruleOrFuncElemNode, new Node(null, "@", astn.getScannerInfo(), Node.OPERATOR_NODE));
				AspectTools.addChild(ruleOrFuncElemNode, "alpha", astn.cloneTree());
				if (!binding.addBinding("proceed", ruleOrFuncElemNode))
					throw new CoreASMError("Name proceed already bound to a different construct during pointcut matching between "+candidate.unparseTree()+" and "+this.unparseTree(), this);
				int numProceedParameters = 0;
				for (ASTNode arg : fnNode.getArguments()) {
					if (Kernel.GR_ID.equals(arg.getGrammarRule())) {
						FunctionRuleTermNode functionRuleTermNode = new FunctionRuleTermNode(arg.getScannerInfo());
						AspectTools.addChild(functionRuleTermNode, "alpha", arg.cloneTree());
						arg = functionRuleTermNode;
					}
					if (!binding.addBinding("p" + (numProceedParameters + 1), arg))
						throw new CoreASMError("Name p" + (numProceedParameters + 1) + " already bound to a different construct during pointcut matching between "+candidate.unparseTree()+" and "+this.unparseTree(), this);
					numProceedParameters++;
				}
				ensureProceedParameters(numProceedParameters);
				substituteProceeds(numProceedParameters, this.getRuleBlock());
			}
			bindings.put(candidate, binding);
		}
		return binding;
	}
	
	private void substituteProceeds(int numProceedParameters, ASTNode astNode) {
		if (astNode instanceof CaseRuleNode ){
			CaseRuleNode caseNode = ((CaseRuleNode)astNode);
			if ("pn".equals(caseNode.getCaseTerm().getFirst().getToken())){
				Map<ASTNode, ASTNode> caseValues = caseNode.getCaseMap();
				Node expression = null;
				for (ASTNode caseValue : caseValues.keySet()) {
					if (Integer.parseInt(caseValue.getToken()) == numProceedParameters)
						return;
					expression = caseValue;
				}
				Node newCaseValue = expression.cloneTree();
				newCaseValue.setToken(Integer.toString(numProceedParameters));
				caseNode.addChildAfter(expression.getNextCSTNode().getNextCSTNode(), "beta", newCaseValue);
				Node operator = expression.getNextCSTNode().cloneTree();
				caseNode.addChildAfter(newCaseValue, DEFAULT_NAME, operator);
				MacroCallRuleNode proceedCall = (MacroCallRuleNode)expression.getNextCSTNode().getNextCSTNode().cloneTree();
				caseNode.addChildAfter(operator, "gamma", proceedCall);

				// Add missing parameters
				FunctionRuleTermNode fn = (FunctionRuleTermNode)proceedCall.getFirst();
				ASTNode lastASTNode = fn.getFirst();
				for (int i = 0; i < numProceedParameters; i++) {
					if (lastASTNode.getNext() == null) {
						ASTNode param = (ASTNode)lastASTNode.cloneTree();
						if (param.getFirst() == null) {
							FunctionRuleTermNode tmp = new FunctionRuleTermNode(param.getScannerInfo());
							tmp.addChild(param);
							param = tmp;
						}
						param.getFirst().setToken("p" + (i + 1));
						fn.addChildAfter(lastASTNode, "lambda", param);
						fn.addChildAfter(lastASTNode, Node.DEFAULT_NAME, new Node(null, ",", param.getScannerInfo(), Node.OPERATOR_NODE));
						lastASTNode = param;
					}
					else
						lastASTNode = lastASTNode.getNext();
				}

				// Remove unnecessary parameter
				while (lastASTNode.getNext() != null)
					lastASTNode.getNextCSTNode().removeFromTree();
				if ("proceed".equals(lastASTNode.getToken()))
					lastASTNode.getNextCSTNode().removeFromTree();
			}
		}
		else if (astNode instanceof MacroCallRuleNode) {
			FunctionRuleTermNode proceed = (FunctionRuleTermNode) astNode.getFirst();
			if ("proceed".equals(proceed.getName()) && proceed.getNumberOfChildren() == 1) {
				String params = "";
				for (int i = 1; i <= numProceedParameters; i++) {
					params += "p" + i;
					if (i < numProceedParameters)
						params += ", ";
				}
				String caseRule =
						"case pn of\n"
								+ numProceedParameters + ": proceed(" + params + ")\n"
								+ "endcase";
				ControlAPI capi = AspectTools.getCapi();
				Parser<Node> caseParser = ((ParserPlugin) capi
						.getPlugin("CaseRulePlugin")).getParsers().get(
						"Rule").parser;// using
				ParserTools parserTools = ParserTools.getInstance(capi);
				Parser<Node> parser = caseParser.from(
						parserTools.getTokenizer(), parserTools.getIgnored());
				ASTNode caseConstruct = (ASTNode) parser.parse(caseRule);
				astNode.replaceWith(caseConstruct);
			}
		}
		else {
			for (ASTNode child : astNode.getAbstractChildNodes())
				substituteProceeds(numProceedParameters, child);
		}
	}

	private void ensureProceedParameters(int requiredProceedParameters) {
		ASTNode signature = getSignature();
		ASTNode lastASTNode = signature.getFirst();
		ASTNode lastProceedParameter = null;
		Pattern pattern = Pattern.compile("p[0-9]+");
		boolean hasProceed = false;
		for (ASTNode param = signature.getFirst().getNext(); param != null; param = param.getNext()) {
			Matcher matcher = pattern.matcher(param.getToken());
			if (matcher.find())
				lastProceedParameter = param;
			if ("proceed".equals(param.getToken()))
				hasProceed = true;
			lastASTNode = param;
		}
		if (!hasProceed) {
			boolean hasParams = signature.getFirst().getNext() != null;
			ASTNode idNode = (ASTNode)signature.getFirst().duplicate();
			idNode.setToken("proceed");
			if (!hasParams)
				signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, new Node(null, ")", idNode.getScannerInfo(), Node.OPERATOR_NODE));
			signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, idNode);
			if (hasParams)
				signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, new Node(null, ",", idNode.getScannerInfo(), Node.OPERATOR_NODE));
			else
				signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, new Node(null, "(", idNode.getScannerInfo(), Node.OPERATOR_NODE));
			lastASTNode = idNode;
			idNode = (ASTNode)signature.getFirst().duplicate();
			idNode.setToken("pn");
			signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, idNode);
			signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, new Node(null, ",", idNode.getScannerInfo(), Node.OPERATOR_NODE));
			lastASTNode = idNode;
		}
		int numProceedParameters = 0;
		if (lastProceedParameter != null)
			numProceedParameters = Integer.parseInt(lastProceedParameter.getToken().substring("p".length()));
		for (int i = numProceedParameters; i < requiredProceedParameters; i++) {
			ASTNode idNode = (ASTNode)signature.getFirst().duplicate();
			idNode.setToken("p" + (i + 1));
			signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, idNode);
			signature.addChildAfter(lastASTNode, Node.DEFAULT_NAME, new Node(null, ",", idNode.getScannerInfo(), Node.OPERATOR_NODE));
			lastASTNode = idNode;
		}
	}
	
	public ASTNode getSignature() {
		return getFirst();
	}
	
	/**
	 * returns the root of the pointcut expression used or defined by this advice
	 * @return 
	 */
	public PointCutASTNode getPointCut(){
		for (ASTNode child : this.getAbstractChildNodes()) {
			if (child instanceof PointCutASTNode)
				return (PointCutASTNode)child;
        }
		//\todo exception no pointcut defined as direct child of AdviceASTNode
		return null;
	}

	public String getLocator() {
		return this.getChildNodes().get(2).getToken();
	}

	public ASTNode getRuleBlock() {
		for (ASTNode child : this.getAbstractChildNodes()) {
			if (child.getGrammarClass().equals(ASTNode.RULE_CLASS))
				return child;
		}
		return null;
	}
	
	public List<ASTNode> getParameters() {
		List<ASTNode> parameters = new LinkedList<ASTNode>();
		for (ASTNode param = getFirst().getFirst().getNext(); param != null; param = param.getNext())
			parameters.add(param);
		return parameters;
	}

	/**
	 * remove bindings created by dynamic poincut constructs from all bindings
	 */
	public List<ASTNode> removeDynamicBindingParameters() {
		List<ASTNode> dynamicBindingparams = new ArrayList<ASTNode>();
		for (Binding binding : bindings.values()) {
			//remove parameters from advice which have a null binding. These bindings are used for dynamic constructs like cflow.
			List<ASTNode> parameter = getParameters();
			ListIterator<ASTNode> it = parameter.listIterator(parameter.size());
			while (it.hasPrevious())
			{
				ASTNode param = it.previous();
				if (binding.hasBindingPartner(param.getToken()) && binding.getBindingPartner(param.getToken()) == null)
				{
					dynamicBindingparams.add(param);
					Node predecessor = param.removeFromTree();
					Node successor = predecessor.getNextCSTNode();
					if ("(".equals(predecessor.getToken()) && ")".equals(successor.getToken())) {
						predecessor.removeFromTree();
						successor.removeFromTree();
					}
					else if (",".equals(predecessor.getToken()))
						predecessor.removeFromTree();
				}
			}
		}
		return dynamicBindingparams;
	}

	/**
	 * transform the given advice ASTNode into a rule declaration ASTNode
	 * 
	 * @param advice
	 *            given for transformation into a rule declaration
	 * @return rule declaration ASTNode
	 * 
	 */
	public ASTNode makeRuleDeclaration() {

		//create components for the rule declaration node
		ASTNode ruleDeclaration;
		//see method RuleDeclarationParseMap in class ParserTools
		ruleDeclaration = new ASTNode(
				null,
				ASTNode.DECLARATION_CLASS,
				Kernel.GR_RULEDECLARATION,
				null,
				this.getScannerInfo()
				);
		Node ruleKeyword = new Node(
				null,
				"rule",
				this.getScannerInfo(),
				Node.KEYWORD_NODE
				);
		//the signature of the rule declaration has to be the one from the advice
		ASTNode ruleSignature = this.getFirst();
		Node equal = new Node(
				"Kernel",
				"=",
				//get ScannerInfo from locator node
				this.getChildNodes().get(2).getScannerInfo(),
				Node.OPERATOR_NODE
				);
		//the body of the rule declaration is the body of the advice
		ASTNode body = this.getAbstractChildNodes().get(this.getAbstractChildNodes().size() - 1);

		//compose the components of the rule declaration node
		AspectTools.addChild(ruleDeclaration, ruleKeyword);
		AspectTools.addChild(ruleDeclaration, ruleSignature);
		AspectTools.addChild(ruleDeclaration, equal);
		AspectTools.addChild(ruleDeclaration, body);

		return ruleDeclaration;
	}

	/**
	 * returns the clone of this object taking into account the given binding.
	 * If the provided binding is null, the unchanged cloneTree of this object is returned.
	 * 
	 * @param binding a binding String->ASTNode used to replace the advices' parameters to enable the proper generation of MacroCallRules for the current weaving candidate 
	 * @return cloneTree with parameters bound to the parameters of a MacroCallRule
	 */
	public AdviceASTNode cloneWithBinding(Binding binding) {
		//if the given binding is null, the this object is returned
		if (!binding.exists())
			return (AdviceASTNode)this.cloneTree();
		else
		//clone this object taking into account the given binding
		{
			Pattern pattern = Pattern.compile("p[0-9]+");
			List<String> paramNames = new LinkedList<String>();
			for (ASTNode param : getParameters()) {
				if (!binding.hasBindingPartner(param.getToken())) {
					if ("around".equals(getLocator())) {
						if (pattern.matcher(param.getToken()).find())
							binding.addBinding(param.getToken(), new ASTNode(Kernel.PLUGIN_NAME, ASTNode.EXPRESSION_CLASS, "KernelTerms", "undef", param.getScannerInfo(), Node.KEYWORD_NODE));
						else if ("pn".equals(param.getToken())) {
							int numProceedParameters = 0;
							while (binding.hasBindingPartner("p" + (numProceedParameters + 1)))
								numProceedParameters++;
							binding.addBinding(param.getToken(), new ASTNode(NumberPlugin.PLUGIN_NAME, ASTNode.EXPRESSION_CLASS, "KernelTerms", "" + numProceedParameters, param.getScannerInfo(), Node.KEYWORD_NODE));
						}
						else
							throw new BindingException("The advice " + getRealName() + " requires a binding for the parameter " + param.getToken() + "!", param);
					}
					else
						throw new BindingException("The advice " + getRealName() + " requires a binding for the parameter " + param.getToken() + "!", param);
				}
				paramNames.add(param.getToken());
			}
			for (String bindingKey : binding.getBinding().keySet()) {
				if (!paramNames.contains(bindingKey))
					AspectTools.getCapi().warning(new CoreASMWarning(AoASMPlugin.PLUGIN_NAME, "The pointcut parameter " + bindingKey + " in aspect " + getRealName() + " is unbound!", this.getFirst().getFirst()));
			}
			AdviceASTNode cloneWithBinding;
			cloneWithBinding = (AdviceASTNode)this.cloneTree();
			
			//change parameters within this method by using side-effects
			cloneWithBinding(cloneWithBinding.getFirst(), binding);
			
			return cloneWithBinding;
		}
	}

	/**
	 * this method initially called by cloneWithBinding(Binding binding) and 
	 * recursively modifies nodes for which a binding exists.
	 * 
	 * @param node
	 * @param binding
	 */
	private void cloneWithBinding(ASTNode node, Binding binding) {

		if (binding.hasBindingPartner(node.getToken()) && //a binding for the current token exists
				node.getParent().getGrammarRule().equals("RuleSignature") && //node is part of the advice signature
				node.getParent().getFirst() != node)	//but node is not the name of the signature
		{
			//children are named "beta" according to ..kernel.RuleDeclarationParseMap.java
			ASTNode bindingPartner = binding.getBindingPartner(node.getToken());
			if (bindingPartner != null)
				node.replaceWith(bindingPartner);
			else
				node.removeFromTree();
		}

		for (ASTNode child : node.getAbstractChildNodes())
			cloneWithBinding(child, binding);

	}

	/**
	 * returns the binding between this advice and the given node
	 * if such a binding exists, otherwise null is returned
	 * @param candidate
	 * @return
	 */
	public Binding getBinding(ASTNode candidate){
		return this.bindings.get(candidate);
	}

	/**
	 * returns the name of the advice
	 * 
	 * @return name of the advice
	 */
	public String getName() {
		return this.getFirst().getFirst().getToken();
	}

	/**
	 * 
	 * @return
	 */
	public String getRealName() {
		return realName;
	}
	
}
