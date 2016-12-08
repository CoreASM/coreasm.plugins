package org.coreasm.engine.plugins.adt;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

public class ParameterNode extends ASTNode{
    
	
    private static final long serialVersionUID = 1L;
    

    public ParameterNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.DECLARATION_CLASS,
        		"ParameterDefiniton",
        		null,
        		info
        		);
    }

    public ParameterNode(ParameterNode node) {
    	super(node);
    }

    public TypeconstructorNode getSecond(){
    	if(getFirst().getNext() instanceof TypeconstructorNode)
    		return (TypeconstructorNode)getFirst().getNext();
    	return null;
    }
   
    public String getName() {
        return getFirst().getToken();
    }
    
    public String getType(){
    	if(getFirst() instanceof TypeconstructorNode){
    		TypeconstructorNode tNode = (TypeconstructorNode) getFirst();
    		
    		//if the Type is at first place
    		if(tNode.hasVariables()){
    			return tNode.getTypeconstructorName();
    		}else{
    			//if there is a selektor and the type is at second place
    			return getSecond().getTypeconstructorName();
    		}
    	}else{
    		return null;
    	}
    }
    
    public String getSelektor(){
    	if(getFirst() instanceof TypeconstructorNode){
    		TypeconstructorNode tNode = (TypeconstructorNode) getFirst();
    		
    		//if there is a selektor, it is in the first place
    		if(!tNode.hasVariables()){
    			return tNode.getName();
    		}
    	}
    	
    	return null;

    }
}
