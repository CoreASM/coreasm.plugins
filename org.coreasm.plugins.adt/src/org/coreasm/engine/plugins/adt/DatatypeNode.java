package org.coreasm.engine.plugins.adt;


import java.util.ArrayList;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
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
    
    public String getDatatypeName(){
    	return this.getFirst().getToken();
    }
    
    public ArrayList<DataconstructorNode> getDataconstructorNodes(){
    	ArrayList<DataconstructorNode> dcNodes = new ArrayList<DataconstructorNode>();
    	
		for(Node n: this.getChildNodes()){
			if(n instanceof DataconstructorNode){
				dcNodes.add((DataconstructorNode) n);
			}
		}
    	
    	return dcNodes;
    }
    
}
