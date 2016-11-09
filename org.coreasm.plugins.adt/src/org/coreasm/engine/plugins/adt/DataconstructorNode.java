package org.coreasm.engine.plugins.adt;


import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;


public class DataconstructorNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public DataconstructorNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.DECLARATION_CLASS,
        		"DataconstructorDefiniton",
        		null,
        		info
        		);
    }

    public DataconstructorNode(DataconstructorNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
}