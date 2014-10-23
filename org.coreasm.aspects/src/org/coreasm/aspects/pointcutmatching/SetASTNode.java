package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.RuleOrFuncElementNode;
import org.coreasm.engine.kernel.UpdateRuleNode;

/**
 * @author Marcel Dausend
 *
 */
public class SetASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = SetASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public SetASTNode(SetASTNode self){
		super(self);
	}

	public SetASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, Node.OTHER_NODE, SetASTNode.NODE_TYPE, null, scannerInfo);
	}

	@Override
	public Binding matches(ASTNode compareToNode) throws AspectException {
		if (!(compareToNode.getParent() instanceof UpdateRuleNode
		&& compareToNode.equals(compareToNode.getParent().getFirst())))
			return new Binding(compareToNode, this);
        FunctionRuleTermNode fnNode = (FunctionRuleTermNode)compareToNode;
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
						if (!Pattern.matches(pointCutToken, astn.unparseTree().trim()))
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
								AspectTools.addChild(ruleOrFuncElemNode, new Node(null, "@", astn.getScannerInfo(), Node.OPERATOR_NODE));
								AspectTools.addChild(ruleOrFuncElemNode, "alpha", astn.cloneTree());
								if ( ! resultingBinding.addBinding(name, ruleOrFuncElemNode))
									throw new CoreASMError("Name "+name+ " already bound to a different construct during pointcut matching between "+compareToNode.unparseTree()+" and "+this.unparseTree(), this); 
							}
							else {
								if (Kernel.GR_ID.equals(astn.getGrammarRule())) {
									FunctionRuleTermNode functionRuleTermNode = new FunctionRuleTermNode(astn.getScannerInfo());
									AspectTools.addChild(functionRuleTermNode, "alpha", astn.cloneTree());
									astn = functionRuleTermNode;
								}
								if ( ! resultingBinding.addBinding(name, astn))
									throw new CoreASMError("Name "+name+ " already bound to a different construct during pointcut matching between "+compareToNode.unparseTree()+" and "+this.unparseTree(), this); 
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
		
		return resultingBinding;
	}
}