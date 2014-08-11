package org.coreasm.aspects.eclipse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.texteditor.MarkerUtilities;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.eclipse.ui.AspectOutline;
import org.coreasm.aspects.eclipse.ui.XReference;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.eclipse.util.Utilities;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.util.information.InformationDispatcher;
import org.coreasm.util.information.InformationObject;
import org.coreasm.util.information.InformationObserver;

public class AoASMEclipsePlugin implements InformationObserver {
	
	private AspectOutline asOutline = null;	// outline
	
	public static final String MARKER_TYPE_POINTCUT_MATCH = "org.coreasm.aspects.eclipse.marker.PointCutMatchMarker";

	public AoASMEclipsePlugin() {

		asOutline = new AspectOutline();
		
		InformationDispatcher.addObserver(this);
		//System.setProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY, Utilities.getAdditionalPluginsFolders());
	}

	@Override
	public void informationCreated(InformationObject information) {
		switch (information.getVerbosity()) {
		case ERROR:
			System.err.println("AopASMEclipsePlugin:" + information);
			break;
		case INFO:
			if (AoASMPlugin.PLUGIN_NAME.equals(information.getSender())) {
				try {
					// deserialize and check if message was a serialized node
					ASTNode n = (ASTNode)AspectTools.anyDeserialize(information.getMessage());
					if (n != null) {
						XReference.setRootNode(n);
						if (XReference.xRefView != null)
						XReference.xRefView.refresh();
						
						// create outline and send nodes to extern outline
						asOutline.createAspectTree(n);
						asOutline.sendRootsToOutline();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			break;
		case COMMUNICATION:
			if (AoASMPlugin.PLUGIN_NAME.equals(information.getSender())) {
//				if (AopASMPlugin.MESSAGE_POINTCUT_MATCH.equals(information.getMessage())) {
					Map<String, String> data = information.getData();
					HashMap<String, Object> attributes = new HashMap<String, Object>();
					attributes.put("name", data.get("name"));
					MarkerUtilities.setMessage(attributes, "Pointcut matching between\n" + data.get("name"));
					Utilities.createMarker(MARKER_TYPE_POINTCUT_MATCH, data.get("file"), Integer.parseInt(data.get("line")), Integer.parseInt(data.get("column")), Integer.parseInt(data.get("length")), attributes);
					
					// create tree objects from pointcut data
					XReference.createTreeObjects(data);
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
					XReference.resetTree();
				asOutline.clearRootsFromOutline();
		}
	}
}
