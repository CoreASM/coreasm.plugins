/**
 * 
 */
package org.coreasm.plugins.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.Map.Entry;

import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.interpreter.ASTNode;

/**
 * @author marcel
 *
 */
public class Binding {
	
	private final ASTNode compareToNode;
	private final PointCutASTNode astNode;
	private HashMap<String, ASTNode> binding;

	public Binding(ASTNode compareToNode, PointCutASTNode astNode, HashMap<String, ASTNode> binding) {
		this.compareToNode = compareToNode;
		this.astNode = astNode;
		this.binding = binding;
	}

	public Binding(ASTNode compareToNode, PointCutASTNode astNode){
		this.compareToNode = compareToNode;
		this.astNode = astNode;
		this.binding = null;
	}

	/**
	 * Create one consistent binding by unifying two existing bindings
	 * 
	 * @param baseBinding
	 * @param mergeBinding
	 */
	public Binding(Binding baseBinding, Binding mergeBinding, PointCutASTNode astNode) {
		this.compareToNode = baseBinding.getCompareToNode();
		this.astNode = astNode;
		binding = new HashMap<String, ASTNode>();
		if (baseBinding.exists() && mergeBinding.exists()) {
			//add all bindings if consistent, otherwise return as non matching
			for (Entry<String, ASTNode> entry : baseBinding.getBinding().entrySet()) {
				if (!addBinding(entry.getKey(), entry.getValue())) {
					binding = null;
					return;
				}
			}
			//add unique bindings from mergeBinding into the resulting binding
			for (Entry<String, ASTNode> entry : mergeBinding.getBinding().entrySet()) {
				if (!binding.containsKey(entry.getKey()))
					addBinding(entry.getKey(), entry.getValue());
			}
		}
		else
			binding = null;
	}

	//clear the binding, if a parameter matching fails during the matching process
	public void clear() {
		binding.clear();
	}

	//return true if a binding exists for the given compareToNode
	public boolean exists(){
		return (binding != null);
	}

	public boolean addBinding(String parameterOfArgsASTNode, ASTNode parameterOfCompareToNode){
		if (binding == null)
			binding = new HashMap<String, ASTNode>();

		if (! binding.containsKey(parameterOfArgsASTNode) && ! parameterOfArgsASTNode.equals("_")){
			this.binding.put(parameterOfArgsASTNode, parameterOfCompareToNode);
			return true;
		}
		else{
			//if token of the already bound ASTNode is not equal to the current AstNode
			//no unique, consistent binding for the given parameterOfArgsASTNode is possible
			if (parameterOfCompareToNode == null && binding.get(parameterOfArgsASTNode) == null)
				return true;
			if (parameterOfCompareToNode == null || binding.get(parameterOfArgsASTNode) == null)
				throw new CoreASMError(
						"Name "
								+ parameterOfArgsASTNode
								+ " already bound to a different construct during pointcut matching between "
								+ compareToNode.unparseTree() + " and " + astNode.unparseTree()
								+ ". Consistency check must be specified inside advice because it a runtime check.",
						astNode);
			return parameterOfArgsASTNode.equals("_") ||
					binding.get(parameterOfArgsASTNode).unparseTree().equals(parameterOfCompareToNode.unparseTree());
		}
	}

	public boolean hasBindingPartner(String key) {
		return binding.containsKey(key);
	}

	public ASTNode getBindingPartner(String key){
			return binding.get(key);
	}

	public HashMap<String, ASTNode> getBinding() {
		return this.binding;
	}

	public ASTNode getCompareToNode(){
		return this.compareToNode;
	}

	public PointCutASTNode getPointcutASTNode(){
		return this.astNode;
	}

	@Override
	public String toString(){
		ASTNode advice = getPointcutASTNode();
		while (!(advice instanceof AdviceASTNode))
			advice = advice.getParent();
		String variableBinding = bindindValueToString().toString();
		if (!variableBinding.isEmpty())
			variableBinding = "\nwith bound variables\n"+variableBinding;
		return "advice " + ((AdviceASTNode) advice).getRealName() + " and "
				+ getCompareToNode().unparseTree()
				+ variableBinding;
	}

	/**
	 * returns a copy of the binding where all values are unparsed to Strings
	 * 
	 * @return binding with values as Strings
	 */
	private HashMap<String, String> bindindValueToString() {
		HashMap<String, String> output = new HashMap<String, String>();
		for (Entry<String, ASTNode> entry : this.binding.entrySet()) {
			if (entry.getValue() != null)
				output.put(entry.getKey(), entry.getValue().unparseTree());
		}
		return output;
	}
}
