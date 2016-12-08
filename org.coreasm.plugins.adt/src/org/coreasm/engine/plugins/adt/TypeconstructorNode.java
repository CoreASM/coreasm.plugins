package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

public class TypeconstructorNode extends ASTNode{
    
	
    private static final long serialVersionUID = 1L;
    

    public TypeconstructorNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.DECLARATION_CLASS,
        		"ParameterDefiniton",
        		null,
        		info
        		);
    }

    public TypeconstructorNode(TypeconstructorNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
    
    public boolean hasVariables(){
    	return (getFirst().getNext() != null);
    }
    
    public String getTypeconstructorName(){
    	ASTNode iterator = getFirst();
    	String result = iterator.getToken() + "(";
    	iterator = iterator.getNext();
    	
    	//if there isn't any parameter, don't concatenate the closing bracket
    	boolean repeat = (iterator!=null);
    	while(iterator!=null){
    		result += iterator.getToken() + ",";
    		iterator = iterator.getNext();
    	}
    	//Substring deletes the last comma, if there was any parameter, otherwise it deletes the opening bracket
    	result = result.substring(0, result.length()-1);
    	if(repeat)
    		result += ")";
    	return result;
    	
    }
}