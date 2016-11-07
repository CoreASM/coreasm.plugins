package org.coreasm.engine.plugins.adt;


import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;


public class DatatypeNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public DatatypeNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.DECLARATION_CLASS,
        		"DatatypeDefinition",
        		null,
        		info
        		);
    }

    public DatatypeNode(DatatypeNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
}
