/*	
 * TypeconstructorNode.java 	1.0
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

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

public class TypeconstructorNode extends ASTNode{
    
	
    private static final long serialVersionUID = 1L;
    

    public TypeconstructorNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.DECLARATION_CLASS,
        		"ParameterDefiniton",
        		null,
        		info
        		);
    }

    public TypeconstructorNode(TypeconstructorNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
    
    
    public String getTypeconstructorName(){
    	ASTNode iterator = getFirst();
    	String result = iterator.getToken() + "(";
    	iterator = iterator.getNext();
    	
    	//if there isn't any parameter, don't concatenate the closing bracket
    	boolean repeat = (iterator!=null);
    	while(iterator!=null){
    		result += iterator.getToken() + ",";
    		iterator = iterator.getNext();
    	}
    	//Substring deletes the last comma, if there was any parameter, otherwise it deletes the opening bracket
    	result = result.substring(0, result.length()-1);
    	if(repeat)
    		result += ")";
    	return result;
    	
    }
}