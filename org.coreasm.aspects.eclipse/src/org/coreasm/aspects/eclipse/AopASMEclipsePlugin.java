package org.coreasm.aspects.eclipse;

import java.util.HashMap;
import java.util.Map;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.eclipse.util.Utilities;
import org.coreasm.util.information.InformationDispatcher;
import org.coreasm.util.information.InformationObject;
import org.coreasm.util.information.InformationObserver;
import org.eclipse.ui.texteditor.MarkerUtilities;

public class AopASMEclipsePlugin implements InformationObserver {
	
	public static final String MARKER_TYPE_POINTCUT_MATCH = "org.coreasm.aspects.eclipse.marker.PointCutMatchMarker";

	public AopASMEclipsePlugin() {
		InformationDispatcher.addObserver(this);
	}

	@Override
	public void informationCreated(InformationObject information) {
		switch (information.getVerbosity()) {
		case ERROR:
			System.err.println("AopASMEclipsePlugin:" + information);
			break;
		case INFO:
			System.out.println("AopASMEclipsePlugin:" + information.getMessage());
			break;
		case COMMUNICATION:
			if (AoASMPlugin.PLUGIN_NAME.equals(information.getSender())) {
//				if (AopASMPlugin.MESSAGE_POINTCUT_MATCH.equals(information.getMessage())) {
					Map<String, String> data = information.getData();
					HashMap<String, Object> attributes = new HashMap<String, Object>();
					attributes.put("name", data.get("name"));
					MarkerUtilities.setMessage(attributes, "PointCut Match for " + data.get("name"));
					Utilities.createMarker(MARKER_TYPE_POINTCUT_MATCH, data.get("file"), Integer.parseInt(data.get("line")), Integer.parseInt(data.get("column")), Integer.parseInt(data.get("length")), attributes);
//				}
			}
			break;
		default:
			System.out.println("AopASMEclipsePlugin:" + information);
		}
	}

	@Override
	public void clearInformation(InformationObject information) {
		if (AoASMPlugin.PLUGIN_NAME.equals(information.getSender())) {
//			if (AopASMPlugin.MESSAGE_POINTCUT_MATCH.equals(information.getMessage()))
				Utilities.removeMarkers(MARKER_TYPE_POINTCUT_MATCH);
		}
	}
}
