package org.coreasm.engine.plugins.adt;


import java.util.ArrayList;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.interpreter.Node.NameNodeTuple;


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
    	return ((TypeconstructorNode)this.getFirst()).getName();
    }
    
    public String getTypeconstructorName(){
    	return ((TypeconstructorNode)this.getFirst()).getTypeconstructorName();
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
