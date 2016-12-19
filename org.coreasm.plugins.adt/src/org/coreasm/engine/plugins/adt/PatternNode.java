/*	
 * PatternNode.java 	1.0
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
    	ArrayList<PatternNode> subNodes = new ArrayList<PatternNode>();
    	
    	for(ASTNode child : getAbstractChildNodes()){
    		if(child instanceof PatternNode){
    			subNodes.add((PatternNode)child);
    		}
    	}
    	
    	return subNodes;
    }
}