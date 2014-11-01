/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.plugins.aspects.AoASMPlugin;
import org.coreasm.plugins.aspects.errorhandling.MatchingError;
import org.coreasm.plugins.aspects.utils.AspectTools;

/**
 * @author Marcel Dausend
 *
 */
public class ArgsASTNode extends PointCutASTNode {
	
	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = ArgsASTNode.class.getSimpleName();

	/**
	 * needed for clone method
	 * @param self
	 */
	public ArgsASTNode(ArgsASTNode self){
		super(self);
	}
	
	/**
	 * 
	 * @param scannerInfo
	 */
	public ArgsASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, ArgsASTNode.NODE_TYPE, null, scannerInfo);
	}
	
	@Override
	public Binding matches(ASTNode compareToNode) {
		FunctionRuleTermNode fnNode = (FunctionRuleTermNode) compareToNode;
		Iterator<ASTNode> argIterator = fnNode.getArguments().iterator();
		String pointCutToken = null;
		Binding resultingBinding = null;
		Node node;
		ASTNode astn = null;
		if (argIterator.hasNext())
			astn = argIterator.next();
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
					//check if the pointcut token is a regular expression
					if (Pattern.compile(pointCutToken) != null) {
						if (!Pattern.matches(pointCutToken, astn.unparseTree()))
							return new Binding(compareToNode, this);

						String name = parameterNode.getName();
						if (name == null) {
							if (resultingBinding == null)
								resultingBinding = new Binding(compareToNode, this, new HashMap<String, ASTNode>());
						}
						else {
							if (resultingBinding == null)
								resultingBinding = new Binding(compareToNode, this);
							if (Kernel.GR_ID.equals(astn.getGrammarRule())) {
								FunctionRuleTermNode functionRuleTermNode = new FunctionRuleTermNode(
										astn.getScannerInfo());
								AspectTools.addChild(functionRuleTermNode, "alpha", astn.cloneTree());
								astn = functionRuleTermNode;
							}
							if (!resultingBinding.addBinding(name, astn))
								throw new CoreASMError("Name " + name
										+ " already bound to a different construct during pointcut matching between "
										+ compareToNode.unparseTree() + " and " + this.unparseTree(), this);
						}
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

		if (astn != null || parameterNode instanceof PointCutParameterNode)
			return new Binding(compareToNode, this);

		return resultingBinding;
	}
}
