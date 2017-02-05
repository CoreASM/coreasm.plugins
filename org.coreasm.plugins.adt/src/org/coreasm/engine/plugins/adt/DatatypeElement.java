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
		return new DatatypeElement("Wildcard", "_" , new ArrayList<Element>());
	}
	
	/*
	 * creates a Variable-DatatypeElement with the given name
	 */
	public static DatatypeElement variable(String name){
		return new DatatypeElement("variable", name, new ArrayList<Element>());
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
		return datatype.equals("Wildcard");
	}
	
	public boolean isVariable(){
		return datatype.equals("variable");
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
