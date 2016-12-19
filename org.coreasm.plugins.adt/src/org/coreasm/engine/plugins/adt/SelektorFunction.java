/*	
 * SelektorFunction.java 	1.0
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

import java.util.List;

import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.FunctionElement;

public class SelektorFunction extends FunctionElement {

	private String datatype; //name of the datatype
	private String dataconstructor; //name of the specific dataconstructor
	private int place; //index of the parameter
	
	
	
	public SelektorFunction(String datatype, String dataconstructor, int index) {
		super();
		this.datatype = datatype;
		this.dataconstructor = dataconstructor;
		this.place = index;
	}

	/*
	 * returns the parameter with this selectorName, 
	 * returns an undefined Element, if the given Element hasn't got this selector,
	 * the only argument is the datatype
	 * 
	 */
	public Element getValue(List<? extends Element> args) {
		
		//if there is not (only) one argument, return a undefined Element
		if (!checkArguments(1, args)){
			return Element.UNDEF;
		}
		
		String valueName = args.toString();

		DatatypeElement value = (DatatypeElement) args.get(0);
		
		// if it is neither the same datatype nor the same dataconstructor, return a undefined Element
		if(!(datatype.equals(value.getDatatype()) && dataconstructor.equals(value.getDataconstructor()))){
			return Element.UNDEF;
		}
		return value.getParameter(place);
	}

	protected FunctionClass getSelektorFunctionClass() {
		return FunctionClass.fcDerived;
	}
	
    /*
	 * checks the validity of the arguments.
	 */
	protected boolean checkArguments(int count, List<? extends Element> args) {

		if (args.size() != count)
			return false;
		else 
			for (int i=0; i < count; i++)
				if ( ! (args.get(i) instanceof Element)) {
					return false;
				}
		
		return true;
	}

}
