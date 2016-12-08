package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;
import java.util.List;

import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.FunctionElement;

public class DataconstructorFunction extends FunctionElement{

	public final String DATACONSTRUCTOR_NAME;
	private final String  DATATYPE_NAME;
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
			for (int i=0; i < PARAMETERS.size(); i++)
				if ( ! (args.get(i) instanceof Element)) {
					return false;
			}
		return true;
	}
	
}


