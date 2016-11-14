package org.coreasm.engine.plugins.adt;


import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;


public class PatternMatchNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public PatternMatchNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.FUNCTION_RULE_CLASS,
        		"PatternMatchDefinition",
        		null,
        		info
        		);
    }

    public PatternMatchNode(PatternMatchNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
}
