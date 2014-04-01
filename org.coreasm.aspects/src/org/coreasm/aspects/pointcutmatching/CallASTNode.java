package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.AspectWeaver;
import org.coreasm.aspects.errorhandling.AspectException;
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
import org.coreasm.engine.kernel.MacroCallRuleNode;
import org.coreasm.engine.kernel.RuleOrFuncElementNode;
import org.coreasm.engine.plugins.signature.FunctionNode;
import org.coreasm.engine.plugins.string.StringBackgroundElement;

/**
 * @author Marcel Dausend
 *
 */
public class CallASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = CallASTNode.class.getSimpleName();

	private String callByAgent ="";

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public CallASTNode(CallASTNode self){
		super(self);
	}

	public CallASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, Node.OTHER_NODE, CallASTNode.NODE_TYPE, null, scannerInfo);
	}

	/**
	 * return false the compareToNode does not match either the name(-expression) of the call pointcut or the return condition
	 * the return condition mean, that the rule has a return <var> in or a result node in its rule definition body.
	 *
	 * @param compareToNode joint point to compare against
	 * @throws AspectException
	 */
	/** \TODO init rule is a problem!!! */
	@Override
	public Binding matches(ASTNode compareToNode) throws AspectException {
        if ( !(compareToNode.getParent() instanceof MacroCallRuleNode) )
			return new Binding(compareToNode.getParent(), this);
		compareToNode = compareToNode.getParent();

        //name of the macro call rule node
        FunctionRuleTermNode fnNode = (FunctionRuleTermNode)compareToNode.getFirst();
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
				PointCutParameterNode parameterNode = (PointCutParameterNode)node;
				//get pointcut's token
				pointCutToken = parameterNode.getPattern();

				//compare the token of the given node with this node's token by using regular expressions
				//if a string is given instead of an id node, the regular expression has to be generated
				try {
					//check if the pointcut token is a regular expression
					if ( Pattern.compile(pointCutToken) != null ){
						if (!Pattern.matches(pointCutToken, (astn instanceof FunctionRuleTermNode ? astn.getFirst().getToken() : astn.getToken())))
							return new Binding(compareToNode, this);
						
						String name = parameterNode.getName();
						if (name == null) {
							if (resultingBinding == null)
								resultingBinding = new Binding(compareToNode, this, new HashMap<String, ASTNode>());
						}
						else {
							if (resultingBinding == null)
								resultingBinding = new Binding(compareToNode, this);
							if (astn == fnNode.getFirst()) {	// IdNode for the rule name
								RuleOrFuncElementNode ruleOrFuncElemNode = new RuleOrFuncElementNode(astn.getScannerInfo());
								AspectTools.addChild(ruleOrFuncElemNode, "alpha", astn.cloneTree());
								if ( ! resultingBinding.addBinding(name, ruleOrFuncElemNode))
									throw new CoreASMError("Name "+name+ " already bound to a different construct during pointcut matching between "+compareToNode.unparseTree()+" and "+this.getFirst().getToken(), this); 
							}
							else {
								if (Kernel.GR_ID.equals(astn.getGrammarRule())) {
									FunctionRuleTermNode functionRuleTermNode = new FunctionRuleTermNode(astn.getScannerInfo());
									AspectTools.addChild(functionRuleTermNode, "alpha", astn.cloneTree());
									astn = functionRuleTermNode;
								}
								if ( ! resultingBinding.addBinding(name, astn))
									throw new CoreASMError("Name "+name+ " already bound to a different construct during pointcut matching between "+compareToNode.unparseTree()+" and "+this.getFirst().getToken(), this); 
							}
						}
					}
				}
				catch (PatternSyntaxException e){
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
		
		if (astn != null || parameterNode instanceof PointCutParameterNode)
			return new Binding(compareToNode, this);
		
		while ( node != null ) {
			if (node.getConcreteNodeType().equals("keyword") && node.getToken().startsWith("with")) {
				if ( checkRuleReturn(fnNode.getName(), node.getToken().equals("with"), node.getNextCSTNode() )){
					if (resultingBinding == null)
						resultingBinding = new Binding(compareToNode, this, new HashMap<String, ASTNode>());
				}else
					return new Binding(compareToNode, this);
			}
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

	/**
	 * this method should lookup if the found rule returns any value and check if this corresponds to the this pointcut expression
	 *
	 * @param compareToNodeToken token is used to find matching rule declaration
	 * @param returnOrResultRequired states if a return value is required or has to be absent
	 * @param returnOrResult if a node with the same token is found in the rule declaration a return statement has been found in rule pointCutToken
	 * @return returns true if the rule definition of the rule which name pointcut for return matches the rule's return value, if exists
	 */
	boolean checkRuleReturn(String compareToNodeToken, boolean returnOrResultRequired, Node returnOrResult){
		String ruleDeclarationName;
		for(ASTNode astn : AspectWeaver.getInstance().getAstNodes().get("RuleDeclaration")){
			ruleDeclarationName = astn.getFirst().getFirst().getToken();
			//check if the current rule declaration is the one with the name of the searching one
			if ( Pattern.matches(compareToNodeToken,  ruleDeclarationName) )
			{
				//collect all nodes inside the rule definition which have the same token as the returnOrResult node
				LinkedList<Node> children = AspectTools.getNodesWithName(astn, returnOrResult.getToken());
					//return xor of returnRequired and children.isEmpty()
					return returnOrResultRequired ^ children.isEmpty();
			}
		}
		//no rule declaration found - should not occur, because its  has already been tested if compareToNodeToken exists inside the matches(..) method
		return false;
	}


	@Override
	public String generateExpressionString() {
		//runtime condition for this call node
		String condition = "";

		//check if this call(..) is part of a cflow condition, e.g. cflow ( call (..) )
		boolean withinCflow=false;
		ASTNode node = this;
		/** TODO question: is it correct, that a expression will be generated if the call is not directly contained within a cflow? */
		while(!(node.getParent() instanceof AdviceASTNode)){
			if (node instanceof CFlowASTNode || node instanceof CFlowBelowASTNode || node instanceof CFlowTopASTNode)
				withinCflow = true;
			node=node.getParent();
		}
		//if the call is surrounded by a cflow construct, generate runtime check
		if (withinCflow) {
			condition += AspectWeaver.MATCHING_RULE_INSIDE_CALLSTACK+"("+AspectTools.getRuleSignatureAsCoreASMList(this)+")!={}";
		}

		//if the context of the call has been restricted to an specific agent
		if ( !callByAgent.isEmpty())
		{
			if ( !condition.isEmpty())
				condition += " and ";
			condition += "matches ( toString( self ) , \"" + callByAgent + "\" )";
		}
		if (condition.isEmpty())
			return "true";
		else return condition;
	}
}