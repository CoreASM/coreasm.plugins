/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.LinkedList;
import java.util.List;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.aspects.AspectTools;
import org.coreasm.aspects.AspectWeaver;
import org.coreasm.aspects.pointcutmatching.Binding;
import org.coreasm.aspects.pointcutmatching.PointCutASTNode.PointCutMatchingResult;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.MacroCallRuleNode;
import org.coreasm.engine.plugins.blockrule.BlockRulePlugin;


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
	
	private List<ProceedASTNode> proceedNodes = new LinkedList<ProceedASTNode>();;

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
		super(AopASMPlugin.PLUGIN_NAME, ASTNode.DECLARATION_CLASS, AdviceASTNode.NODE_TYPE, null, scannerInfo);
		adviceId = "adv".concat(Integer.toString(numberOfAdviceNodes));
		numberOfAdviceNodes++;
	}

	public static void resetAdviceCounter(){
		numberOfAdviceNodes=0;
	}
	
	/**
	 * Override the advice name in the identifier with the generic unique value adviceName!
	 * Exchange FunctionRuleTermNodes with name "proceed" with a ProceedASTNode
	 * ExcerptOfSigniturePlugin.createRuleElement(ASTNode currentRuleDeclaration, Interpreter interpreter)
	 */
	@Override
	public void addChild( String name, Node node){
		super.addChild(name, node);
		
		//real name cannot be set, when e.g. using cloneTree, because children do not exist at this time
		if (! this.getAbstractChildNodes().isEmpty()){
			this.realName = this.getFirst().getFirst().getToken();
			this.getFirst().getFirst().setToken(adviceId);
		}
		
		//if node is a FunctionRuleTermNode od MacroCallRule with token proceed, a new ProceedASTNode has to be inserted instead
		if ( node.getPluginName().equals("BlockRulePlugin")){//if the child of advice is the BlockRule, i.e. the advice's body
			LinkedList<Node> proceedNodes = AspectTools.getNodesWithName(node, "proceed");
			ASTNode proceedNode = null;
			Node parent = null;
			Node insertionReference = null;
			for (Node proceedId : proceedNodes) {
				if (proceedId.getParent() instanceof FunctionRuleTermNode)
					proceedNode = (FunctionRuleTermNode)proceedId.getParent();
				if (proceedNode != null && proceedNode.getParent() instanceof MacroCallRuleNode)
					proceedNode = (MacroCallRuleNode)proceedNode.getParent();
				if(proceedNode != null) {
					proceedNodes.add(proceedNode);
					parent = proceedNode.getParent();
					insertionReference = proceedNode.removeFromTree();
					if (proceedNode instanceof FunctionRuleTermNode)
						parent.addChildAfter(insertionReference, AspectTools.constructName(proceedNode), (FunctionRuleTermNode)proceedNode.cloneTree());
					else if (proceedNode instanceof MacroCallRuleNode)
						parent.addChildAfter(insertionReference, AspectTools.constructName(proceedNode), (MacroCallRuleNode)proceedNode.cloneTree());
				}
			}
			
		}
	}
	
	/**
	 * returns the boolean value of the pointcut expression of this advice
	 * 
	 * @param candidate given pointcut of the advice
	 * @return result of the matching
	 * @throws Exception 
	 */
	public PointCutMatchingResult matches(ASTNode candidate) throws Exception{
		//pointcut cannot be null (which is assured by parsing)
		return getPointCut().matches(candidate);
	}
	
	public PointCutASTNode getPointCut(){
			return getPointCut(this);
	}
	
	private PointCutASTNode getPointCut(ASTNode currentNode){
		for (ASTNode child : currentNode.getAbstractChildNodes()) {
			if (child instanceof NamedPointCutASTNode)
                return ((NamedPointCutASTNode)child).getPointCutASTNode();
            else if (child instanceof PointCutASTNode)
				return (PointCutASTNode)child;
        }
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

	/**
	 * returns the clone of this object taking into account the given binding.
	 * If the provided binding is null, the unchanged cloneTree of this object is returned.
	 * 
	 * @param binding a binding String->ASTNode used to replace the advices' parameters to enable the proper generation of MacroCallRules for the current weaving candidate 
	 * @return cloneTree with parameters bound to the parameters of a MacroCallRule
	 */
	public AdviceASTNode cloneWithBinding(Binding binding) {
		//if the given binding is null, the this object is returned
		if (binding == null)
			return (AdviceASTNode)this.cloneTree();
		else
		//clone this object taking into account the given binding
		{
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
		
		if (binding.getBindingPartner(node.getToken())!=null && //a binding for the current token exists
				node.getParent().getGrammarRule().equals("RuleSignature") && //node is part of the advice signature
				node.getParent().getFirst()!=node)	//but node is not the name of the signature
			{
				ASTNode parentNode = node.getParent();
				Node insertionReference = node.removeFromTree();
				parentNode.addChildAfter(insertionReference, binding.getBindingPartner(node.getToken()).getToken(), binding.getBindingPartner(node.getToken()));
			}
		
		if ( node.getAbstractChildNodes().size()>0)
			for(ASTNode child : node.getAbstractChildNodes())
				cloneWithBinding(child, binding);

		
	}

	/**
	 * @return the proceedNodes
	 */
	public List<ProceedASTNode> getProceedNodes() {
		return proceedNodes;
	}
	
}
