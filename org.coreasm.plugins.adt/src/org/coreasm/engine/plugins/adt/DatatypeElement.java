package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;

import org.coreasm.engine.absstorage.Element;

public class DatatypeElement extends Element {
	private String datatype;
	private String dataconstructor;
	private ArrayList<Object> parameter;
	
	
	public DatatypeElement(String datatype, String dataconstructor, ArrayList<Object> parameter) {
		super();
		this.datatype = datatype;
		this.dataconstructor = dataconstructor;
		this.parameter = parameter;
	}
	
	public Object getParameter(int index){
		return parameter.get(index);
	}

	public ArrayList<Object> getParameter() {
		return parameter;
	}

	public String getDatatype() {
		return datatype;
	}

	public String getDataconstructor() {
		return dataconstructor;
	}
}
