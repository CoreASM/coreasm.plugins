/*	
 * SelektorNode.java 	1.0
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

/*
 * A SelektorNode is build for structures like "variable.selektor". 
 * Selektors like "selektor(variable)" are handled in a different way
 */
public class SelektorNode extends ASTNode {
    
	
    private static final long serialVersionUID = 1L;
    

    public SelektorNode(ScannerInfo info) {
        super(
        		ADTPlugin.PLUGIN_NAME,
        		ASTNode.FUNCTION_RULE_CLASS,
        		"SelektorDefinition",
        		null,
        		info
        		);
    }

    public SelektorNode(SelektorNode node) {
    	super(node);
    }
    
   
    public String getName() {
        return getFirst().getToken();
    }
    
    public String getSelektorName(){
    	return getFirst().getNext().getToken();
    }
}
