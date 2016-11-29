package org.coreasm.engine.plugins.adt;

import java.util.List;

import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.FunctionElement;

public class SelektorFunction extends FunctionElement {

	private String datatype; //name of the datatype
	private String dataconstructor; //name of the specific dataconstructor
	private int place; //index of the parameter
	
	
	
	public SelektorFunction(String datatype, String dataconstructor, int place) {
		super();
		this.datatype = datatype;
		this.dataconstructor = dataconstructor;
		this.place = place;
	}

	@Override
	public Element getValue(List<? extends Element> args) {
		//generated dynamically 
		return null;
	}

	protected FunctionClass getSelektorFunctionClass() {
		return FunctionClass.fcDerived;
	}

}
