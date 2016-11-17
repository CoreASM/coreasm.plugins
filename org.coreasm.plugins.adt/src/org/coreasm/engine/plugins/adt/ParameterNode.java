package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
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
    
   
    public String getName() {
        return getFirst().getToken();
    }
    
    public ASTNode getType(){
    	return getFirst();
    }
    
    public ASTNode getSelektor(){
    	return getFirst().getNext();
    }
}
