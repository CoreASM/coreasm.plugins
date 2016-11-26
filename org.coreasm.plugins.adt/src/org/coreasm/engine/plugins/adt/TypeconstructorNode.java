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
    
    public String getTypeconstructorName(){
    	ASTNode iterator = getFirst();
    	String result = iterator.getToken() + "(";
    	while(iterator!=null){
    		iterator = iterator.getNext();
    		result += iterator.getToken();
    	}
    	return (result + ")");
    	
    }
}