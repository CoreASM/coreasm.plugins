/*	
 * DatatypeElement.java 	1.0
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

import org.coreasm.engine.absstorage.Element;

public class DatatypeElement extends Element {
	private String datatype;
	private String dataconstructor;
	private ArrayList<Element> parameter;
	private final static String WILDCARD = "Wildcard";
	private final static String VARIABLE = "variable";
	
	
	public DatatypeElement(String datatype, String dataconstructor, ArrayList<Element> parameter) {
		super();
		this.datatype = datatype;
		this.dataconstructor = dataconstructor;
		this.parameter = parameter;
	}
	
	/*
	 * creates a default Wildcard-DatatypeElement
	 */
	public static DatatypeElement wildcard(){
		return new DatatypeElement(WILDCARD, "_" , new ArrayList<Element>());
	}
	
	/*
	 * creates a Variable-DatatypeElement with the given name
	 */
	public static DatatypeElement variable(String name){
		return new DatatypeElement(VARIABLE, name, new ArrayList<Element>());
	}
	
	public Element getParameter(int index){
		return parameter.get(index);
	}

	public ArrayList<Element> getParameter() {
		return parameter;
	}

	public String getDatatype() {
		return datatype;
	}

	public String getDataconstructor() {
		return dataconstructor;
	}
	
	public boolean isWildcard(){
		return datatype.equals(WILDCARD);
	}
	
	public boolean isVariable(){
		return datatype.equals(VARIABLE);
	}
	
	public static String getWildcardTyp(){
		return WILDCARD;
	}

	public static String getVariableTyp(){
		return VARIABLE;
	}
	
	public String getVariableName() {
		if(isVariable())
			return getDataconstructor();
		return "";
	}
	
	@Override
	public String getBackground(){
		return getDatatype();
	}
}
