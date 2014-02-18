/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;

import org.coreasm.aspects.AspectTools;
import org.coreasm.engine.interpreter.ASTNode;

/**
 * @author marcel
 *
 */
public class Binding {
	
	private final ASTNode compareToNode;
	private final ArgsASTNode argsAstNode;
	HashMap<String, ASTNode> binding; 
	
	public Binding(ASTNode compareToNode, ArgsASTNode argsAstNode){
		this.compareToNode = compareToNode;
		this.argsAstNode = argsAstNode;
		binding = new HashMap<String, ASTNode>();
	}
	
	//clear the binding, if a parameter matching fails during the matching process
	public void clear() {
		binding.clear();
	}
	
	//return true if a binding exists for the given compareToNode
	public boolean exists(){
		return !binding.isEmpty();
	}

	public boolean addBinding(String parameterOfArgsASTNode, ASTNode parameterOfCompareToNode){
		if (! binding.containsKey(parameterOfArgsASTNode) && ! parameterOfArgsASTNode.equals("_")){
			this.binding.put(parameterOfArgsASTNode, parameterOfCompareToNode);
			return true;
		}
		else{
			//if token of the already bound ASTNode is not equal to the current AstNode
			//no unique, consistent binding for the given parameterOfArgsASTNode is possible
			return parameterOfArgsASTNode.equals("_") ||
					AspectTools.constructName(binding.get(parameterOfArgsASTNode)).equals(AspectTools.constructName(parameterOfCompareToNode));
		}
	}
	
	public ASTNode getBindingPartner(String key){
			return binding.get(key);
	}
	
	public ASTNode getCompareToNode(){
		return this.compareToNode;
	}
	
	public ASTNode getArgsASTNode(){
		return this.argsAstNode;
	}
	
	public String toString(){
		return "binding between parameters of "+getArgsASTNode()+" and "+getCompareToNode()+"\n"+
				binding.toString();
	}
}
