package org.coreasm.engine.plugins.adt;


import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;


public class PatternNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public PatternNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.FUNCTION_RULE_CLASS,
        		"Pattern",
        		null,
        		info
        		);
    }

    public PatternNode(PatternNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
}