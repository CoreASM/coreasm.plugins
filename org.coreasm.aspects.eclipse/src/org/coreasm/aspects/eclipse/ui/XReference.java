package org.coreasm.aspects.eclipse.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.coreasm.aspects.eclipse.ui.providers.TreeObject;
import org.coreasm.aspects.eclipse.ui.views.XReferenceView;

/**
 * @author Tobias
 *
 * The logical part of the Cross Reference
 * Creates a cross reference tree for each file, so even when multiple files are
 * supported it should work. 
 */
public class XReference {
	// connection to the view
	public static XReferenceView xRefView;
	
	// Holds the cross reference root nodes for each file
	private static HashMap<String, TreeObject> rootNodesPerFile = new HashMap<String, TreeObject>();
	
	public static String currentFileName;
	
	/**
	 * @param data	The data send by the createMarker() function
	 * 
	 * Creates cross reference tree objects for the pointcut
	 * Gets called when a marker is created
	 */
	public static void createTreeObjects(Map<String, String> data) {
		// extract the data
		String fileName = new File(data.get("file")).getName();
		String aspect = data.get("aspect");
		String advice = data.get("advice");
		String function = data.get("function");
		int functionPos = Integer.parseInt(data.get("functionPos"));
		int advicePos = Integer.parseInt(data.get("advicePos"));
		int aspectPos = Integer.parseInt(data.get("aspectPos"));
		
		// create an invisible root node with the filename
		TreeObject invisibleRootObj = addInvisibleRoot(fileName);
		
		// create a tree which shows what the aspect advices
		TreeObject aspectObj = addNodeWithPos(aspect, "aspect.gif", aspectPos, invisibleRootObj);
		TreeObject adviceObj = addNodeWithPos(advice, "advice.gif", advicePos, aspectObj);
		TreeObject advicesObj = addNode("advices", "arrow.gif", adviceObj);
		addNodeWithPos(function, null, functionPos, advicesObj);
		
		// create a tree which shows from who the function is adviced by
		TreeObject functionParentObj = addNodeWithPos(data.get("ruleName"), "rule.gif", Integer.parseInt(data.get("rulePos")), invisibleRootObj);
		TreeObject functionObj = addNodeWithPos(function, null, functionPos, functionParentObj);
		TreeObject advicedByObj = addNode("adviced by", "arrow.gif", functionObj);
		addNodeWithPos(advice, "advice.gif", advicePos, advicedByObj);

		if (xRefView != null)
			xRefView.refresh();
	}
	
	/**
	 * Gets called after parsing is finished,
	 * so when parsing starts again we have a clean tree and no duplicates
	 */
	public static void resetTree() {
		if (currentFileName != null)
			rootNodesPerFile.get(currentFileName).getChildren().clear();
	}
	
	/**
	 * @param description	Name of the node
	 * @param icon			Icon location
	 * @param rootObj		Root in the cross reference
	 * @return				Cross reference node
	 * 
	 * Adds a simple cross reference node to the root and returns it
	 */
	private static TreeObject addNode(String description, String icon, TreeObject rootObj) {
		TreeObject nodeObj = rootObj.getChild(description);
		if (nodeObj != null)
			return nodeObj;
		
		nodeObj = new TreeObject(description, icon);
		rootObj.addChild(nodeObj);
		
		return nodeObj;
	}
	
	/**
	 * @param description	Name of the node
	 * @param icon			Icon location
	 * @param pos			Position in the editor
	 * @param rootObj		Root in the the cross reference
	 * @return				Cross reference node
	 * 
	 * Adds a new node to the root and returns it
	 * With position in the editor so highlighting is enabled
	 */
	private static TreeObject addNodeWithPos(String description, String icon, int pos, TreeObject rootObj) {
		TreeObject aspectObj = rootObj.getChild(description);
		if (aspectObj != null)
			return aspectObj;
		
		aspectObj = new TreeObject(description, icon, pos);
		rootObj.addChild(aspectObj);
		
		return aspectObj;
	}
	
	/**
	 * @param fileName	Filename of the current open file
	 * @return			Cross reference node
	 * 
	 * Creates and returns an invisible node and adds it to rootNodesPerFile
	 */
	private static TreeObject addInvisibleRoot(String fileName) {
		TreeObject invisibleRootNode = rootNodesPerFile.get(fileName);
		if (invisibleRootNode != null)
			return invisibleRootNode;
		
		invisibleRootNode = new TreeObject("");
		rootNodesPerFile.put(fileName, invisibleRootNode);
		
		return invisibleRootNode;
	}
	
	public static TreeObject getTreeForFile(String fileName) {
		TreeObject root = rootNodesPerFile.get(fileName);
		if (root == null) {
			root = addInvisibleRoot(fileName);
			addNode("Cross references are showing after parsing", "aspect.gif", root);
		}
		
		return root;
	}
}
