/*	
 * DatatypeBackgroundElement.java 	1.0
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

public class DatatypeBackgroundElement extends BackgroundElement {

	/**
	 * The name of the datatype
	 */
	public final String DATATYPE_BACKGROUND_NAME;
	private ArrayList<DataconstructorBackgroundElement> datatypeConstructors; 
	
	
	public DatatypeBackgroundElement(String dATATYPE_BACKGROUND_NAME,
			ArrayList<DataconstructorBackgroundElement> datatypeConstructors) {
		super();
		DATATYPE_BACKGROUND_NAME = dATATYPE_BACKGROUND_NAME;
		this.datatypeConstructors = datatypeConstructors;
	}

	@Override
	public Element getNewValue() {
		return Element.UNDEF;
	}

	@Override
	/*
	 * Returns a <code>TRUE</code> boolean for 
	 * Datatype Elements. Otherwise <code>FALSE<code> is returned.
	 * 
	 * (non-Javadoc)
	 * @see org.coreasm.engine.absstorage.AbstractUniverse#getValue(org.coreasm.engine.absstorage.Element)
	 */
	protected BooleanElement getValue(Element e) {
		if(e instanceof DatatypeElement){
			return BooleanElement.TRUE;
		}else{
			return BooleanElement.FALSE;
		}
	}


	public String getDATATYPE_BACKGROUND_NAME() {
		return DATATYPE_BACKGROUND_NAME;
	}

	public ArrayList<DataconstructorBackgroundElement> getDatatypeConstructors() {
		return datatypeConstructors;
	}

}
