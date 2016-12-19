/*	
 * DatatypeNode.java 	1.0
 * 
 *
 * Copyright (C) 2016 Matthias Jörg
 *
 * Licensed under the Academic Free License version 3.0 
 *   http://www.opensource.org/licenses/afl-3.0.php
 *   http://www.coreasm.org/afl-3.0.php
 *
 */

package org.coreasm.engine.plugins.adt;


import java.util.ArrayList;

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
    
    public String getDatatypeName(){
    	return ((TypeconstructorNode)this.getFirst()).getName();
    }
    
    public String getTypeconstructorName(){
    	return ((TypeconstructorNode)this.getFirst()).getTypeconstructorName();
    }
    
    public ArrayList<DataconstructorNode> getDataconstructorNodes(){
    	ArrayList<DataconstructorNode> dcNodes = new ArrayList<DataconstructorNode>();
    	
    	/*for(NameNodeTuple child: getChildNodesWithNames()){
			if("Dataconstructor".equals(child.name)){
				dcNodes.add((DataconstructorNode) child.node);
			}
		}
    	*/
    	
    	for(ASTNode child : getAbstractChildNodes()){
    		if(child instanceof DataconstructorNode){
    			dcNodes.add((DataconstructorNode)child);
    		}
    	}
    	
    	return dcNodes;
    }
    
    
}
