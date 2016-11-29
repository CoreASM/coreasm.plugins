package org.coreasm.engine.plugins.adt;


import java.util.ArrayList;

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
    
    public String getVariableName(){
    	return getName();
    }
    
    public ArrayList<DataconstructorNode> getDataconstructorNodes(){
    	ArrayList<DataconstructorNode> dcNodes = new ArrayList<DataconstructorNode>();
    	
    	for(NameNodeTuple child: this.getChildNodesWithNames()){
			if("Dataconstructor".equals(child.name)){
				dcNodes.add((DataconstructorNode) child.node);
			}
		}
    	
    	return dcNodes;
    }
    
}
