/*	
 * PatternMatchNode.java 	1.0
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

public class PatternMatchNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public PatternMatchNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.FUNCTION_RULE_CLASS,
        		"PatternsAndResult",
        		"PatternMatchNode",
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
    
    public ArrayList<PatternNode> getPatternNodes(){
    	ArrayList<PatternNode> patNodes = new ArrayList<PatternNode>();
    	
    	for(ASTNode child : getAbstractChildNodes()){
    		if(child instanceof PatternNode){
    			patNodes.add((PatternNode)child);
    		}
    	}
    	
    	return patNodes;
    }
    
    public ASTNode getResult(ASTNode pattern){
    	
    	ASTNode node = pattern;
    	
    	//look for the next Node, which isn't a PatternNode. It has to be a ResultNode
    	while(node instanceof PatternNode){
    		node = node.getNext();
    	}
    	
    	return node;
    	
    	
    }
    
    public ASTNode getValueNode(){
    	return getFirst();
    }
    
}
