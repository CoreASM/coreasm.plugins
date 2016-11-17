package org.coreasm.engine.plugins.adt;


import java.util.ArrayList;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
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
    
    public ArrayList<ParameterNode> getParameterNodes(){
    	ArrayList<ParameterNode> pNodes = new ArrayList<ParameterNode>();
    	
		for(Node n: this.getChildNodes()){
			if(n instanceof ParameterNode){
				pNodes.add((ParameterNode) n);
			}
		}
    	
    	return pNodes;
    }
    
    public String getDataconstructorName(){
    	return getFirst().getToken();
    }
}