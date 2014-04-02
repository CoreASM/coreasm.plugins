/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author Marcel Dausend
 *
 */
public class NamedPointCutDefinitionASTNode extends ASTNode {

	private static final long serialVersionUID = 1L;

	public static final String NODE_TYPE = NamedPointCutDefinitionASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public NamedPointCutDefinitionASTNode(NamedPointCutDefinitionASTNode self){
		super(self);
	}

	public NamedPointCutDefinitionASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, Node.OTHER_NODE, NamedPointCutDefinitionASTNode.NODE_TYPE, null, scannerInfo);
	}
	
	/**
	 * get the name of the pointcut
	 * @return token as name of the pointut
	 */
	public String getName(){
		return this.getFirst().getToken();
	}

	/**
	 * return the parameters of this node
	 * omit the BinOrASTNode at the end
	 * @return
	 */
	public List<ASTNode> getPointCutParameters(){
		List<ASTNode> params = new LinkedList<ASTNode>();
		ASTNode child = this.getFirst().getNext();
		while (child != null) {
			ASTNode next = child.getNext();
			if (next != null)
				params.add(child);
			child = next;
		}
		return params;
	}
	
	/**
	 * according to the general CoreASM definition, 
	 * a definition of a named pointcut has to have a uniq name.
	 * Under that premise, a NamedPointCutASTNode belongs is defined by exactly one
	 * NamedPointCutDefinitionASTNode with the same name.
	 * @param nptc
	 * @return true, if the usage and the defintion of the named pointcut have the same name
	 */
	public boolean isDefinitionOf(NamedPointCutASTNode nptc){
		if ( ! this.getName().equals(nptc.getName()) )
			return false;
		//check parameter number
		return this.getPointCutParameters().size() == nptc.getPointCutParameters().size();
	}
	
	/**
	 * returns the pointcut defined as direct child of this node
	 * @return BinorASTNode as the root of a maybe complex pointcut expression
	 */
	public BinOrASTNode getPointCut(){
		for(Node child : this.getChildNodes())
			if (child instanceof BinOrASTNode)
				return (BinOrASTNode)child;
		//\todo exception no pointcut (BinOrASTNode) defined by this NamedPointCutASTNode
		return null;
	}
	
	public Set<PointCutParameterNode> getUnboundPointCutParameters() {
		Set<PointCutParameterNode> unboundParams = new HashSet<PointCutParameterNode>();
		Set<String> params = new HashSet<String>();
		for (ASTNode param : getPointCutParameters())
			params.add(param.getToken());
		collectUnboundPointCutParamters(this, unboundParams, params);
		return unboundParams;
	}
	
	private static void collectUnboundPointCutParamters(ASTNode node, Set<PointCutParameterNode> unboundParams, Set<String> params) {
		if (node instanceof PointCutParameterNode) {
			PointCutParameterNode param = (PointCutParameterNode)node;
			if (!params.contains(param.getName()))
				unboundParams.add(param);
		}
		for (ASTNode child : node.getAbstractChildNodes())
			collectUnboundPointCutParamters(child, unboundParams, params);
	}

	/**
	 * return the parameter node of a parameter from the signature that is not
	 * used on the righthand side of the named pointcut definition
	 * 
	 * TODO consider that a parameter definition should appear on both sides of
	 * an or-expression
	 * 
	 * @return
	 */
	public List<ASTNode> requiredParametersContained() {
		List<ASTNode> parameters = this.getPointCutParameters();
		for (ASTNode child : this.getAbstractChildNodes()) {
			parameters = checkIsRequiredParameterExists(parameters, child);
			//param from signature has been found on right side
			if (parameters.isEmpty())
				break;
			}
		return parameters;
	}

	/**
	 * check if the node's token matches one of the parameters and if remove it
	 * from the parameter list
	 * 
	 * @param parameters
	 * @param node
	 * @return
	 */
	private List<ASTNode> checkIsRequiredParameterExists(List<ASTNode> parameters, ASTNode node) {
		List<ASTNode> parametersOutput = new LinkedList<ASTNode>(parameters);
		for (ASTNode astNode : parameters) {
			if (node instanceof PointCutParameterNode)
				if (astNode.getToken().equals(((PointCutParameterNode) node).getName())) {
					parametersOutput.remove(astNode);
					break;
				}
			if (node.getParent() instanceof NamedPointCutASTNode)
				if (astNode.getToken().equals(((FunctionRuleTermNode) node).getName()))
					parametersOutput.remove(astNode);
			for (ASTNode child : node.getAbstractChildNodes()) {
				if (parametersOutput.isEmpty())
					break;
				else
					parametersOutput = checkIsRequiredParameterExists(parametersOutput, child);
			}
		}
		return parametersOutput;
	}

}
