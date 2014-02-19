/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.aspects.AspectTools;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.ScannerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Marcel Dausend
 *
 */
public class ArgsASTNode extends PointCutASTNode {
	
	//a binding between the parameters of a compareToNode and this args expression is stored to ease weaving
	private HashMap<ASTNode, Binding> bindings = new HashMap<ASTNode, Binding>();
	private Binding parameterBinding;
	
	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = ArgsASTNode.class.getSimpleName();

	/**
	 * needed for clone method
	 * @param self
	 */
	public ArgsASTNode(ArgsASTNode self){
		super(self);
	}
	
	public ArgsASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, ArgsASTNode.NODE_TYPE, null, scannerInfo);
	}
	/**
	 *  returns the bindings which exist for the given node
	 *  if the complete pointcut expression has been evaluated to true
	 * @param compareToNode binding is returned
	 * @return bindings for compareToNode
	 */
	public Binding getBindings(ASTNode compareToNode) {
		return bindings.get(compareToNode);
	}
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see org.coreasm.aspects.pointcutmatching.PointCutASTNode#matches(org.coreasm.engine.interpreter.ASTNode)
	 */
	public Binding matches(ASTNode compareToNode) {
		
		//create a new instance of an argument binding for this compareToNode
		parameterBinding = new Binding(compareToNode, this);
		
		ArrayList<ASTNode> children = (ArrayList<ASTNode>)this.getAbstractChildNodes();

		ArrayList<ASTNode> compareChildren = (ArrayList<ASTNode>)compareToNode.getFirst().getAbstractChildNodes();
		//remove id node of the macro call rule
		//TODO keep name in binding for later use with proceed
		compareChildren.remove(0);

		//check if both macro call and this have the same arity
		if (children.size()==compareChildren.size())
			//check if each parameter of the call has the same type like the one of this args
			for (int i = 0;i<children.size();i++) {
				//if any parameter does not have the same type, return false
				if ( children.get(i) instanceof FunctionRuleTermNode &&
					compareChildren.get(i) instanceof FunctionRuleTermNode )
				{	
					if(! areASTNodesBindable((FunctionRuleTermNode)children.get(i), (FunctionRuleTermNode)compareChildren.get(i)))
						return new Binding(compareToNode, this);
				}
				else if(! areASTNodesBindable(children.get(i), compareChildren.get(i)))
					return new Binding(compareToNode, this);
			}
		else
			return new Binding(compareToNode, this);
		//all params of args match the compareToNode macro call
		LinkedList<ArgsASTNode> returnList = new LinkedList<ArgsASTNode>();
		returnList.add(this);
		{
			//if the matching is successful, add store binding
			bindings.put(compareToNode, parameterBinding);
			return parameterBinding;
		}
	}
	
	/**
	 * this method checks if both function rule terms (of a macro rule call) are bindable
	 * @param one
	 * @param two
	 * @return
	 */
	private boolean areASTNodesBindable(FunctionRuleTermNode one, FunctionRuleTermNode two){
		if (one.getAbstractChildNodes().size()== two.getAbstractChildNodes().size())
		{
			//bind function name of one to function name of two
			if (! parameterBinding.addBinding(AspectTools.constructName(one),two))
			{
				parameterBinding.clear();
				return false;
			}
			//from one (leave out the id node of the rule function node; 
			//check if all parameters are bindable
			for (int i = 1; i < one.getAbstractChildNodes().size(); i++) {
				//if any parameter does not have the same type, return false
				if (one.getAbstractChildNodes().get(i) instanceof FunctionRuleTermNode &&
					two.getAbstractChildNodes().get(i) instanceof FunctionRuleTermNode	)
					if (! areASTNodesBindable(
							(FunctionRuleTermNode)one.getAbstractChildNodes().get(i), 
							(FunctionRuleTermNode)two.getAbstractChildNodes().get(i)
							)
						)
					{
						parameterBinding.clear();
						return false;
					}
					else if (! areASTNodesBindable(
						one.getAbstractChildNodes().get(i), 
						two.getAbstractChildNodes().get(i)
						)
					)
				{
						parameterBinding.clear();
					return false;
				}
			}
			return true;
		}
		else
			parameterBinding.clear();
			return false;
	}
	
	private boolean areASTNodesBindable(ASTNode one, ASTNode two){
		
		String nameOfOne = AspectTools.constructName(one);
		String nameOfTwo = AspectTools.constructName(two);
		
		/**
		 * define cases when binding is not possible
		 *
		 *	binding is possible:
		 *	ID -> any
		 *	FunctionRuleTerm -> FunctionRuleTerm
		 *	BooleanTerm -> BooleanTerm if equal
		 *	StringTerm	-> StringTerm if equal
		 *	KernelTerms -> KernelTerms if equal
		 *	NUMBER	-> NUMBER	if equal
		 *
		 *	the case where a terminal node can be bound to either 
		 *	a variable or a function is not taken into account:
		 *	this would need modify the body of each advice and create seperate rules with conditions to check this bindings at runtime.
		 *	That would mean, that the call of one and the same advice would lead to execution of different rules!
		 */		
		if (one.getGrammarRule().equals(two.getGrammarRule())) {
			//the tokens must be the same in these cases
			if (one.getGrammarRule().equals("NUMBER") ||
				one.getGrammarRule().equals("StringTerm") ||
				one.getGrammarRule().equals("BooleanTerm") ||
				one.getGrammarRule().equals("KernelTerms") ) {
				if (! nameOfOne.equals(nameOfTwo))
				{	
					parameterBinding.clear();
					return false;
				}
			}else{
				throw new CoreASMError(
						"values comparision not yet implemented for the GrammarRule "
								+ one.getGrammarRule());
			}
		}
		else if( one instanceof FunctionRuleTermNode && ! (two instanceof FunctionRuleTermNode) )
		{
			parameterBinding.clear();
			return false;
		}//TODO insert cas both are FunctionRuleTermNode
		else if ( one.getGrammarRule().equals("FunctionRuleTerm") )
			//can only bind to another function rule term
			//\see areASTNodesBindable(FunctionRuleTermNode one, FunctionRuleTermNode two)
			return false;
		
		//returns true if the new binding is possible, i.e. that this binding does not already exist
		//use a clone node for the binding
		return parameterBinding.addBinding(nameOfOne, (ASTNode)two.cloneTree());
	}
	
//	/**
//	 * this method returns an String expression which can be used to check if the parameters of the given candidate match the arguments of this ArgsASTNode
//	 */
//	public String generateExpressionString(ASTNode candidate) {
//		String expression="";
//		if(this.getAbstractChildNodes().size()==candidate.getFirst().getAbstractChildNodes().size()-1)
//		for (int i = 0;i<this.getAbstractChildNodes().size();i++) {
//			expression = expression.concat(
//					"( "+AspectTools.node2String(this.getAbstractChildNodes().get(i))+" = "+AspectTools.node2String(candidate.getFirst().getAbstractChildNodes().get(i+1))+" )"
//					);
//			if (i < this.getAbstractChildNodes().size()-1 )
//			expression= expression+" and ";
//		}else expression = "false";
//		return expression;
//	}

	@Override
	public String generateExpressionString() {
		// static condition which has already been checked
		boolean withinCflow=false;
		ASTNode node = this;
		while(!(node.getParent() instanceof AdviceASTNode)){
			if (node instanceof CFlowASTNode || node instanceof CFlowBelowASTNode || node instanceof CFlowTopASTNode)
				withinCflow =true;
			node=node.getParent();
		}
		if (withinCflow) {
			return "argsCall("+getRuleSignatureAsCoreASMList(this)+")!={}";
		}else
			return "true";
	}
	
	private String getRuleSignatureAsCoreASMList(ArgsASTNode astNode){
		String ruleSignatureAsCoreASMList = "[";
		ASTNode param = astNode.getFirst();
		while (param != null) {
			ruleSignatureAsCoreASMList += AspectTools.node2String(param);
			param = param.getNext();
			if (param != null)
				ruleSignatureAsCoreASMList += ", ";
		}

		ruleSignatureAsCoreASMList += " ]";
		return ruleSignatureAsCoreASMList;
	}
}
