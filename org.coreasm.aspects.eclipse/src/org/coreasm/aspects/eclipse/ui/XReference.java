package org.coreasm.aspects.eclipse.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.coreasm.aspects.eclipse.ui.providers.TreeObject;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.eclipse.editors.AstTools;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;

/**
 * @author Tobias
 *
 * The logical part of the Cross Reference
 * Creates a cross reference tree for each file, so even when multiple files are
 * supported it should work. 
 */
public class XReference {
	
	// the root node of the parser
	public static ASTNode rootNode;
	
	// Holds the cross reference root nodes for each file
	private static HashMap<String, TreeObject> rootNodesPerFile = new HashMap<String, TreeObject>();
	
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
		TreeObject functionParentObj = addFunctionParent(functionPos, invisibleRootObj);
		TreeObject functionObj = addNodeWithPos(function, null, functionPos, functionParentObj);
		TreeObject advicedByObj = addNode("adviced by", "arrow.gif", functionObj);
		addNodeWithPos(advice, "advice.gif", advicePos, advicedByObj);
	}
	
	/**
	 * Gets called after parsing is finished,
	 * so when parsing starts again we have a clean tree and no duplicates
	 */
	public static void resetTree() {
		rootNodesPerFile.clear();
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
	 * @param functionPos	Position of the function in the editor
	 * @param rootObj		Root in the cross reference
	 * @return				Cross reference node
	 * 
	 * Gets the parent node from the function that is at functionPos.
	 * Extract the name of that node
	 * Create and return cross reference node
	 */
	private static TreeObject addFunctionParent(int functionPos, TreeObject rootObj) {
		String description = "FunctionParent";
		String icon = "";
		int parentPos = 0;
		
		// get parent node
		Node n = AspectTools.getParentNode(functionPos, rootNode);
		
		// extract description
		if (n instanceof ASTNode) {
			ASTNode astNode = (ASTNode) n;
			if (astNode.getGrammarRule().equals(AstTools.GRAMMAR_RULE)) {
				description = astNode.getChildNodes().get(1).getChildNodes().get(0).getToken();
				icon = "rule.gif";
			}
			else if (astNode.getGrammarRule().equals("Signature")) {
				description = AspectTools.findId(astNode);
				icon = "sign.gif";
			}
			
			parentPos = astNode.getScannerInfo().charPosition;
		} 
		
		// create and return crossref node
		return addNodeWithPos(description, icon, parentPos, rootObj);
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
		if (rootNodesPerFile != null)
			return rootNodesPerFile.get(fileName);
		else
			return null;
	}
	
	public static void setRootNode(ASTNode node) {
		rootNode = node;
	}
}
