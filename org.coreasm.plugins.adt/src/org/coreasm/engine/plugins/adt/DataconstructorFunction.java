/*	
 * DataconstructorFunction.java 	1.0
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
import java.util.List;

import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.FunctionElement;

import CompilerRuntime.CoreASMError;

public class DataconstructorFunction extends FunctionElement{

	public final String DATACONSTRUCTOR_NAME;
	public final String  DATATYPE_NAME;
	private final ArrayList<String> PARAMETERS;
	
	public DataconstructorFunction(String dATATYPE_NAME, String dATACONSTRUCTOR_NAME, ArrayList<String> pARAMETERS) {
		super();
		DATACONSTRUCTOR_NAME = dATACONSTRUCTOR_NAME;
		DATATYPE_NAME = dATATYPE_NAME;
		PARAMETERS = pARAMETERS;
	}


	@Override
	public Element getValue(List<? extends Element> args) {
		
		if(checkArguments(args))
			return new DatatypeElement(DATATYPE_NAME, DATACONSTRUCTOR_NAME, new ArrayList<Element>(args));
		else
			return Element.UNDEF;
	}

	
	/*
	 * checks the validity of the arguments.
	 */
	protected boolean checkArguments(List<? extends Element> args) {

		if (args.size() != PARAMETERS.size())
			return false;
		else
			for (int i=0; i < PARAMETERS.size(); i++){
				
				//check if it is a Element
				if ( ! (args.get(i) instanceof Element))
					return false;
					
				//if it is a Element, typecheck it
				Element arg = (Element) args.get(i);
				if(!arg.getBackground().equals(PARAMETERS.get(i)))
				{
					System.out.println( new CoreASMError("Typechecking-Error at Dataconstructor " + DATACONSTRUCTOR_NAME + " of the datatype " + DATATYPE_NAME + " in the " + i  + ". argument. \n "
							+ "An Element of the type " + PARAMETERS.get(i) + " was expected, but there is an Element of the type " + arg.getBackground()));
					return false;
				}
			}
		return true;
	}
	
	
}


