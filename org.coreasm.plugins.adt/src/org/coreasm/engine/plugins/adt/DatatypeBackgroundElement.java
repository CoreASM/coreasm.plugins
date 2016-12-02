package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;

import org.coreasm.engine.absstorage.BackgroundElement;
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
	protected Element getValue(Element e) {
		// TODO Auto-generated method stub
		return null;
	}

}
