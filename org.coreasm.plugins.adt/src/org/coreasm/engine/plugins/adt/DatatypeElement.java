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
	
	public static DatatypeElement wildcard(){
		return new DatatypeElement("_", "" , new ArrayList<Object>());
	}
	
	public static DatatypeElement variable(String name){
		return new DatatypeElement(name, "", new ArrayList<Object>());
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
	
	public boolean isWildcard(){
		return datatype.equals("_");
	}
	
	public boolean isVariable(){
		return datatype.equals("variable");
	}

	public String getVariableName() {
		return getDataconstructor();
	}
}
