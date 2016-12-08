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
	
	public static DatatypeElement wildcard(){
		return new DatatypeElement("_", "" , new ArrayList<Element>());
	}
	
	public static DatatypeElement variable(String name){
		return new DatatypeElement(name, "", new ArrayList<Element>());
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
		return datatype.equals("_");
	}
	
	public boolean isVariable(){
		return datatype.equals("variable");
	}

	public String getVariableName() {
		return getDataconstructor();
	}
}
