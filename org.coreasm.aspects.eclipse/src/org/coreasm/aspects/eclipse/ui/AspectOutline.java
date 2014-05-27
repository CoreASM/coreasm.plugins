package org.coreasm.aspects.eclipse.ui;

import java.net.URL;
import java.util.ArrayList;

import org.coreasm.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.aspects.pointcutmatching.AspectASTNode;
import org.coreasm.aspects.pointcutmatching.NamedPointCutDefinitionASTNode;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.eclipse.editors.AstTools;
import org.coreasm.eclipse.editors.outlining.OutlineTreeNode;
import org.coreasm.eclipse.editors.outlining.RootOutlineTreeNode;
import org.coreasm.eclipse.util.Utilities;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.plugins.modularity.IncludeNode;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Tobias
 *
 * The outline for the aspect plugin
 * Creates a tree for each aspect in the file and stores them
 * in the rootList
 * 
 * Also containing custom outlinenode classes
 * e.g AdviceTreeNode, AspectTreeNode, PointcutTreeNode
 */
public class AspectOutline {
	
	// list with aspect roots
	private ArrayList<RootOutlineTreeNode> rootList = new ArrayList<RootOutlineTreeNode>(); 
	
	/**
	 * remove and send aspect roots to the outline view of
	 * coreasm
	 */
	public void removeRootsFromOutline() {
		for (RootOutlineTreeNode rootNode : rootList) {
			Utilities.removeExternOutlineRoot(rootNode);
		}
	}
	
	public void sendRootsToOutline() {
		for (RootOutlineTreeNode rootNode : rootList) {
			Utilities.addExternOutlineRoot(rootNode);
		}
	}
	
	/**
	 * @param root	the root node of the current file/parser
	 * 
	 * Main function to create the aspect tree
	 */
	public void createAspectTree(ASTNode root) {
		rootList.clear();
		
		if (root != null) {
			// get all aspect nodes an add them to the root list
			for (Node child : root.getChildNodes()) {
				if (child instanceof AspectASTNode) {
					AspectASTNode aspectNode = (AspectASTNode) child;
					AspectTreeNode aspectRoot = new AspectTreeNode(aspectNode);
									
					// append root children to groupNodes and allNodes
					for (Node aspectChildNode : child.getChildNodes()) {
						appendChild(aspectChildNode, aspectRoot);
					}
					
					// add
					rootList.add(aspectRoot);
				}
			}	
		}
	}
	
	/**
	 * @param aspectChildNode	child of the aspect node
	 * @param aspectRoot		aspect root of the outline
	 * 
	 * Appends a child to the aspect outline,
	 * depending on the Type of aspectChildNode, the right description is extracted and
	 * then the right outline node is created
	 * e.g. if aspectChildNode is a RuleNode -> new OutlineTreeNode.RuleTreeNode
	 */
	private void appendChild(Node aspectChildNode, RootOutlineTreeNode aspectRoot) {
		if (aspectChildNode instanceof ASTNode) {
			ASTNode astNode = (ASTNode) aspectChildNode;
			
			if (astNode.getGrammarRule().equals("UseClauses")) {		
				String description = astNode.getChildNodes().get(1).getToken();
				aspectRoot.addNode(new OutlineTreeNode.UseTreeNode(astNode.toString(), description));
			}
			
			else if (astNode.getGrammarRule().equals(AstTools.GRAMMAR_RULE)) {
				String description = astNode.getChildNodes().get(1).getChildNodes().get(0).getToken();
				aspectRoot.addNode(new OutlineTreeNode.RuleTreeNode(astNode.toString(), description));
			}
			
			else if (astNode.getGrammarRule().equals(AstTools.GRAMMAR_INIT)) {
				String description = astNode.getChildNodes().get(1).getToken();
				aspectRoot.addNode(new OutlineTreeNode.InitTreeNode(astNode.toString(), description));
			}
			
			else if (astNode.getGrammarRule().equals("Signature")) {
				String description = AspectTools.findId(astNode);
				aspectRoot.addNode(new OutlineTreeNode.SignatureTreeNode(astNode.toString(), description));
			}
			
			else if (astNode.getGrammarRule().equals("PropertyOption")) {	
				String suffix = AspectTools.findId(astNode);
				String description = astNode.getChildNodes().get(1).getToken();
				aspectRoot.addNode(new OutlineTreeNode.OptionTreeNode(astNode.toString(), description, suffix));
			}
			
			else if (astNode instanceof NamedPointCutDefinitionASTNode) {
				NamedPointCutDefinitionASTNode pointcutNode = 
						(NamedPointCutDefinitionASTNode) astNode;
				aspectRoot.addNode(new PointcutTreeNode(pointcutNode));
			}
			
			else if (astNode instanceof AdviceASTNode) {
				AdviceASTNode advNode = (AdviceASTNode) astNode;
				aspectRoot.addNode(new AdviceTreeNode(advNode));
			}
		}
		
		if (aspectChildNode instanceof IncludeNode) {
			IncludeNode iNode = (IncludeNode) aspectChildNode;
			String description = iNode.getFilename();	
			aspectRoot.addNode(
				new OutlineTreeNode.IncludeTreeNode(iNode.toString(), description, ""));
		}
	}
	
	public class AspectTreeNode extends RootOutlineTreeNode {
		public AspectTreeNode(AspectASTNode aspectNode) {
			super(aspectNode.toString(), aspectNode.getName(), "");			
			icon = null;
			iconURL = getIconURL("icons/aspect.gif");
		}
	}
	
	public class AdviceTreeNode extends OutlineTreeNode {
		public AdviceTreeNode(AdviceASTNode advNode) {
			super(advNode.toString(), advNode.getRealName());
			group = "Advices";
			iconURL = getIconURL("icons/advice.gif");
		}
	}
	
	public class PointcutTreeNode extends OutlineTreeNode {
		public PointcutTreeNode(NamedPointCutDefinitionASTNode pointcutNode) {
			super(pointcutNode.toString(), pointcutNode.getName());
			group = "Pointcuts";
			iconURL = getIconURL("icons/pointcut_def.gif");
		}
	}
	
	/**
	 * @param pluginPath
	 * @return	the URL of the icon
	 */
	private URL getIconURL(String pluginPath) {
		URL url = FileLocator.find(FrameworkUtil.getBundle(this.getClass()), new Path(pluginPath), null);	
		return url;
	}
}
