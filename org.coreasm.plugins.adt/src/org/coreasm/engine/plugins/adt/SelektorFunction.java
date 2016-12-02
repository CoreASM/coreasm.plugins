package org.coreasm.engine.plugins.adt;

import java.util.List;

import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.FunctionElement;
import org.coreasm.engine.interpreter.Interpreter;

public class SelektorFunction extends FunctionElement {

	private String datatype; //name of the datatype
	private String dataconstructor; //name of the specific dataconstructor
	private int place; //index of the parameter
	private Interpreter interpreter;
	
	
	
	public SelektorFunction(String datatype, String dataconstructor, int place, Interpreter interpreter) {
		super();
		this.datatype = datatype;
		this.dataconstructor = dataconstructor;
		this.place = place;
		this.interpreter = interpreter;
	}

	/*
	 * the argument is the datatype
	 * 
	 */
	public Element getValue(List<? extends Element> args) {
		
		String valueName = args.get(1).toString();
		DatatypeElement value = (DatatypeElement) interpreter.getEnv(valueName);
		
		// if there is either (only) one argument nor the same datatype nor the same dataconstructor, return a undefined Element
		if(!(checkArguments(1, args)) && datatype.equals(value.getDatatype()) && dataconstructor.equals(value.getDataconstructor())){
			return Element.UNDEF;
		}
		Object returnvalue = value.getParameter(place);
		String type = ((DataconstructorBackgroundElement)interpreter.getEnv(dataconstructor)).getParameterType(place);


		return new Element(); //TODO
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
