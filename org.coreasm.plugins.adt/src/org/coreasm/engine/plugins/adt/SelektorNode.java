package org.coreasm.engine.plugins.adt;


import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;


public class SelektorNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public SelektorNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.DECLARATION_CLASS,
        		"SelektorDefinition",
        		null,
        		info
        		);
    }

    public SelektorNode(SelektorNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
}
