/*	
 * DataconstructorNode.java 	1.0
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
    	
		/*for(NameNodeTuple child: this.getChildNodesWithNames()){
			if("Parameter".equals(child.name)){
				pNodes.add((ParameterNode) child.node);
			}
		}*/
    	
    	for(ASTNode child : getAbstractChildNodes()){
    		if(child instanceof ParameterNode){
    			pNodes.add((ParameterNode)child);
    		}
    	}
    	
    	return pNodes;
    }
    
}