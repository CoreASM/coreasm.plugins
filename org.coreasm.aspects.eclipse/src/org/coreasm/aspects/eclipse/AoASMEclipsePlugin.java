package org.coreasm.aspects.eclipse;

import java.util.HashMap;
import java.util.Map;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.eclipse.ui.XReference;
import org.coreasm.aspects.eclipse.ui.providers.AspectOutlineContentProvider;
import org.coreasm.eclipse.util.Utilities;
import org.coreasm.util.information.InformationDispatcher;
import org.coreasm.util.information.InformationObject;
import org.coreasm.util.information.InformationObserver;
import org.eclipse.ui.texteditor.MarkerUtilities;

public class AoASMEclipsePlugin implements InformationObserver {
	
	public static final String MARKER_TYPE_POINTCUT_MATCH = "org.coreasm.aspects.eclipse.marker.PointCutMatchMarker";

	public AoASMEclipsePlugin() {
		InformationDispatcher.addObserver(this);
		Utilities.addOutlineContentProvider(new AspectOutlineContentProvider());
	}

	@Override
	public void informationCreated(InformationObject information) {
		switch (information.getVerbosity()) {
		case ERROR:
			System.err.println("AopASMEclipsePlugin:" + information);
			break;
		case COMMUNICATION:
			if (AoASMPlugin.PLUGIN_NAME.equals(information.getSender())) {
				Map<String, String> data = information.getData();
				HashMap<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("name", data.get("name"));
				MarkerUtilities.setMessage(attributes, "Pointcut matching between\n" + data.get("name"));
				Utilities.createMarker(MARKER_TYPE_POINTCUT_MATCH, data.get("file"), Integer.parseInt(data.get("line")), Integer.parseInt(data.get("column")), Integer.parseInt(data.get("length")), attributes);

				// create tree objects from pointcut data
				XReference.createTreeObjects(data);
			}
			break;
			
		default:
			System.out.println("AopASMEclipsePlugin:" + information);
		}
	}

	@Override
	public void clearInformation(InformationObject information) {
		if (AoASMPlugin.PLUGIN_NAME.equals(information.getSender())) {
			Utilities.removeMarkers(MARKER_TYPE_POINTCUT_MATCH);
			XReference.resetTree();
		}
	}
}
