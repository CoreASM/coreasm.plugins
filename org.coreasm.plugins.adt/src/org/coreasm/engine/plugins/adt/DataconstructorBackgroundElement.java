/*	
 * DataconstructorBackgroundElement.java 	1.0
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

import org.coreasm.engine.absstorage.BackgroundElement;
import org.coreasm.engine.absstorage.BooleanElement;
import org.coreasm.engine.absstorage.Element;

public class DataconstructorBackgroundElement extends BackgroundElement{

	public final String DATACONSTRUCTOR_BACKGROUND_NAME;
	private String  datatypeName;
	private ArrayList<String> parameters;
	
	public DataconstructorBackgroundElement(String dATACONSTRUCTOR_BACKGROUND_NAME, String DatatypeName, ArrayList<String> parameters) {
		super();
		DATACONSTRUCTOR_BACKGROUND_NAME = dATACONSTRUCTOR_BACKGROUND_NAME;
		this.parameters = parameters;
	}

	@Override
	public Element getNewValue() {
		return Element.UNDEF;
	}

	@Override
	protected BooleanElement getValue(Element e) {
		if(e instanceof DatatypeElement){
			return BooleanElement.TRUE;
		}else{
			return BooleanElement.FALSE;
		}
	}
	
	public String getParameterType(int index){
		return parameters.get(index);
	}

	public String getDatatypeName() {
		return datatypeName;
	}

	public static DataconstructorBackgroundElement wildcard() {
		return new DataconstructorBackgroundElement("_",null,new ArrayList<String>());
	}
	
	
}
