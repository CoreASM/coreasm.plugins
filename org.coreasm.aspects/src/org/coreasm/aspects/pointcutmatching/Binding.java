/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.HashMap;
import java.util.Map.Entry;

import org.coreasm.aspects.AspectTools;
import org.coreasm.engine.interpreter.ASTNode;

/**
 * @author marcel
 *
 */
public class Binding {
	
	private final ASTNode compareToNode;
	private final PointCutASTNode astNode;
	HashMap<String, ASTNode> binding;

    public Binding(ASTNode compareToNode, PointCutASTNode astNode, HashMap<String, ASTNode> binding){
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
     * Create one consistent binding by merging to existing bindings
     * @param baseBinding
     * @param mergeBinding
     */
    public Binding(Binding baseBinding, Binding mergeBinding, PointCutASTNode astNode) {
        this.compareToNode = baseBinding.getCompareToNode();
        this.astNode = astNode;
        binding = new HashMap<String, ASTNode>();
        if (baseBinding.exists() && mergeBinding.exists()){
            for(Entry<String, ASTNode> entry : baseBinding.getBinding().entrySet()){

                //check if a variable that is used in both bindings binds to the same asm construct in both cases
                if ( mergeBinding.getBinding().containsKey(entry.getKey()) )
                    if ( AspectTools.node2String(mergeBinding.getBindingPartner(entry.getKey())).equals(
                            AspectTools.node2String(entry.getValue())
                    ) ) //add
                        binding.put(entry.getKey(), (ASTNode)entry.getValue().cloneTree());
                    else {
                        //inconsistent binding -> exit constructor
                        binding = null;
                        return;
                    }
                else
                    //include not common bindings from baseBinding into the resulting binding
                    binding.put(entry.getKey(), (ASTNode)entry.getValue().cloneTree());
                ;
            }
            //include not common bindings from mergeBinding into the resulting binding
            for(Entry<String, ASTNode> entry : mergeBinding.getBinding().entrySet())   {
                if ( ! binding.containsKey(entry.getKey() ) ){
                    binding.put(entry.getKey(), (ASTNode)entry.getValue().cloneTree());
                }
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
		return ( binding != null && ! binding.isEmpty() )? true : false;
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

    public HashMap<String, ASTNode> getBinding(){
        return this.binding;
    }

	public ASTNode getCompareToNode(){
		return this.compareToNode;
	}

	public PointCutASTNode getPointcutASTNode(){
		return this.astNode;
	}

	public String toString(){
		return "binding between parameters of "+ getPointcutASTNode()+" and "+getCompareToNode()+"\n"+
				binding.toString();
	}
}
