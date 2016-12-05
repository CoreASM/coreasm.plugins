package org.coreasm.engine.plugins.adt;


import java.util.ArrayList;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.interpreter.Node.NameNodeTuple;


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
    
    public boolean isWildcard(){
    	return "_".equals(getName());
    }
    
    public boolean hasSubPatterns(){
    	return (!getSubPattern().isEmpty());
    }
    
    public ArrayList<PatternNode> getSubPattern(){
    	ArrayList<PatternNode> dcNodes = new ArrayList<PatternNode>();
    	
    	for(NameNodeTuple child: this.getChildNodesWithNames()){
			if("Dataconstructor".equals(child.name)){
				dcNodes.add((PatternNode) child.node);
			}
		}
    	
    	return dcNodes;
    }
}