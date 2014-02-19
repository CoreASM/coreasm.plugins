package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.aspects.AspectTools;
import org.coreasm.aspects.AspectWeaver;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.MacroCallRuleNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
		super(AopASMPlugin.PLUGIN_NAME, Node.OTHER_NODE, CallASTNode.NODE_TYPE, null, scannerInfo);
	}

	/**
	 * return false the compareToNode does not match either the name(-expression) of the call pointcut or the return condition
	 * the return condition mean, that the rule has a return <var> in or a result node in its rule definition body.
	 *
	 * @param compareToNode joint point to compare against
	 * @throws Exception
	 */
	/** \TODO init rule is a problem!!! */
	@Override
	public Binding matches(ASTNode compareToNode) throws Exception {
        if ( !(compareToNode instanceof MacroCallRuleNode) )
            return new Binding(compareToNode, this);
        //name of the macro call rule node
		String compareToNodeToken = compareToNode.getFirstASTNode().getFirstASTNode().getToken();

        String pointCutToken = null;
		Binding resultingBinding = new Binding(compareToNode, this);
        // \todo add bindings
		//step through all children of the call pointcut call ( regEx4name by regEx4agentOrUnivers with||without return||result )
		for(Node astn : this.getChildNodes()){
			switch (this.getChildNodes().indexOf(astn)) {
			//check if the name/regEx of the pointcut matches the compareToNode
			case 2: //index of the pointcut name expression

				//get pointcut's token
				pointCutToken = astn.getToken();

				//compare the token of the given node with this node's token by using regular expressions
				//if a string is given instead of an id node, the regular expression has to be generated
				try {
					//check if the pointcut token is a regular expression
					if ( Pattern.compile(pointCutToken) != null ){
						if (!Pattern.matches(pointCutToken, compareToNodeToken))
							return new Binding(compareToNode, this);
					}
				}
				catch (PatternSyntaxException e){
					//if the pointcut token is no regular expression throw an exception towards the weaver
					throw new MatchingError(pointCutToken, this, e.getMessage(), e);
				}
				break;
			//agent expression relevant in method \link generateExpressionString to extend the runtime condition
			case 4:
				callByAgent = astn.getToken();
				break;
			//if tokens match, check if compareToNode fulfills the return-condition
			case 5: //last check => return result
				//if the matching was successful, the compareToNode rule definition has to be investigated for return statements depending on the given condition
				if(astn.getToken().equals(AopASMPlugin.KW_WITHOUT)) //if the token of astn is "without" it has to be checked that no return or result exists in the ruledefinitoin
					if ( ! checkRuleReturn(compareToNodeToken, false, astn.getNextCSTNode()))
                    return new Binding(compareToNode, this);
				else
					return resultingBinding;
				}
		}
		return resultingBinding;
	}

	/**
	 * this method should lookup if the found rule returns any value and check if this corresponds to the this pointcut expression
	 *
	 * @param compareToNodeToken token is used to find matching rule declaration
	 * @param returnRequired states if a return value is required or has to be absent
	 * @param returnOrResult if a node with the same token is found in the rule declaration a return statement has been found in rule pointCutToken
	 * @return returns true if the rule definition of the rule which name pointcut for return matches the rule's return value, if exists
	 */
	boolean checkRuleReturn(String compareToNodeToken, boolean returnRequired, Node returnOrResult){
		String ruleDeclarationName;
		for(ASTNode astn : AspectWeaver.getAstNodes().get("RuleDeclaration")){
			ruleDeclarationName = astn.getFirst().getFirst().getToken();
			//check if the current rule declaration is the one with the name of the searching one
			if ( Pattern.matches(compareToNodeToken,  ruleDeclarationName) )
			{
				//collect all nodes inside the rule definition which have the same token as the returnOrResult node
				LinkedList<Node> children = AspectTools.getNodesWithName(astn, returnOrResult.getToken());
					//return xor of returnRequired and children.isEmpty()
					return returnRequired ^ children.isEmpty();
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
			condition += "self = "+callByAgent;
		}
		if (condition.isEmpty())
			return "true";
		else return condition;
	}
}