package org.coreasm.aspects;

import org.coreasm.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.aspects.pointcutmatching.CallASTNode;
import org.coreasm.aspects.pointcutmatching.NamedPointCutASTNode;
import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMIssue;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.MacroCallRuleNode;
import org.coreasm.engine.kernel.SkipRuleNode;
import org.coreasm.engine.plugins.blockrule.BlockRulePlugin;
import org.coreasm.engine.plugins.turboasm.SeqBlockRuleNode;
import org.coreasm.util.Tools;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * @author Marcel Dausend
 * 
 */
public class AspectTools {

	public final static String RULESIGNATURE = "RuleSignature"; //from SignaturPlugin
	
	/* ControlAPI used to reproduce text or dot output from a given AST*/
	private static ControlAPI capi = null;

	/**
	 * set the ControlAPI used by AspectTools
	 * @param ControlAPI capi
	 */
	public static void setCapi(ControlAPI capi) {
		AspectTools.capi = capi;
	}

	/**
	 * get the current ControlAPI which has been set for AspectTools
	 * @return the currently asigned ControlAPI 
	 */
	private static ControlAPI getCapi() {
		return AspectTools.capi;
	}

	public static void addChildAfter(Node parent, Node insertionReference, String name, Node node){
		parent.addChildAfter(insertionReference, name, node);
		List<Node> newNodes = new LinkedList<Node>();
		newNodes.add(node);
		Node root = parent;
		while(root.getParent()!=null)
			root = root.getParent();
		AspectTools.update(nodes2dot(root), newNodes);
		newNodes.clear();
	}

	public static void addChild(Node parent, String name, Node node){
		if (name == null)
			parent.addChild(node);
		else
			parent.addChild(name, node);
		List<Node> newNodes = new LinkedList<Node>();
		newNodes.add(node);
		Node root = parent;
		while(root.getParent()!=null)
			root = root.getParent();
		AspectTools.update(nodes2dot(root), newNodes);
		newNodes.clear();
	}

	public static void addChild(Node parent, Node node){
		addChild(parent, null, node);
	}

	private static String timestamp = null;
	private static int currentDot = 0;
	public static void update(String dot, List<Node> changed) {
		currentDot++;

		for (Node change : changed) {
			String toMatch = getDotNodeId(change)+" [label";
			dot.replace(toMatch, toMatch+"color=\"red\", ");
		}

		List<String> revised  = new LinkedList<String>(Arrays.asList(dot.split("\n")));

		GraphViz gv = new GraphViz();
		Iterator<String> it = revised.iterator();
		gv.start_graph();
		while (it.hasNext())
			gv.addln(it.next());
		gv.end_graph();

		// String type = "gif";
		String type = "dot";
		// String type = "fig"; // open with xfig
		// String type = "pdf";
		// String type = "ps";
		//String type = "svg"; // open with inkscape
		// String type = "png";
		// String type = "plain";
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		if (timestamp == null)
			timestamp = formatter.format(cal.getTime());

		String tmpDir = System.getProperty("java.io.tmpdir");
		File out = new File(tmpDir+"/"+timestamp+"/out."+currentDot+"."+ type);
		out.getParentFile().mkdirs();
		gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
	}

	private static File lastChoosenFile;
	/** Convert a given AST-node into a String representation.
	 * 
	 * @param	node to convert start from the string conversion of the AST
	 * @return	The String representation of a Node
	 * @returnval string representation of the given AST starting from node
	 */
	public static String node2String(ASTNode node) {
		String output = "";
		if (node.getGrammarRule().equals("KernelTerms") ||
				node.getGrammarRule().equals("BooleanTerm") ||
				node.getGrammarRule().equals("NUMBER") ||
				node.getGrammarRule().equals("ID") ) {
			output = node.getToken();
		}else if (node.getGrammarRule().equals("StringTerm") ) {
			output = "\"" + node.getToken() + "\"";
		}else if (node.getGrammarRule().equals("FunctionRuleTerm") || node instanceof NamedPointCutASTNode) {
			if (node.getAbstractChildNodes().size() == 1)
				output = node.getFirst().getToken();
			else {
				output = node.getFirst().getToken() + "( ";
				for (int i = 1; i < node.getAbstractChildNodes().size() - 1; i++) {
					output = output
							+ node2String(node.getAbstractChildNodes().get(
									i)) + ", ";
				}
				output = output
						+ node2String(node.getAbstractChildNodes().get(
								node.getAbstractChildNodes().size() - 1))
						+ " )";
			}
		}else if (node.getGrammarRule().equals("RuleOrFunctionElementTerm") ) {
			output = "@" + node.getFirst().getToken();
		}else{
				throw new CoreASMError(
						"No conversion rule node2String for GrammarRule \""
								+ node.getGrammarRule() + "\"", node);
		}
		return output;
	}

	public static String getRuleSignatureAsCoreASMList(CallASTNode astNode) {
		String ruleSignatureAsCoreASMList = "[";
		ASTNode param = astNode.getFirst();
		if (param.getGrammarRule().equals("StringTerm"))
			ruleSignatureAsCoreASMList+=AspectTools.node2String(param);
		else //include the result into string quotes
			ruleSignatureAsCoreASMList+="\""+AspectTools.node2String(param)+"\"";
		param=param.getNext();
		while (param != null) {
			ruleSignatureAsCoreASMList += AspectTools.node2String(param);
			param = param.getNext();
			if (param != null)
				ruleSignatureAsCoreASMList += ", ";
		}

		ruleSignatureAsCoreASMList += " ]";
		return ruleSignatureAsCoreASMList;
	}

	/**
	 * returns the program text a part of a CoreASM program starting from the
	 * given node of an valid ast tree
	 * 
	 * @param node
	 *            from where the program text is received
	 * @return program text as (fast and dirty) formated string
	 */
	private static String getCoreASMProgram(ControlAPI capi, Node node) {
		String result = "";
		result = getText(node, capi, new TextScope(node));
		return result;
	}

	private static int indexOfCasmFilename(String context) {
		int index;
		if (context.contains(".coreasm"))
			index = context.substring(0, context.indexOf(".coreasm")).lastIndexOf(' ') + 1;
		else if (context.contains(".casm"))
			index = context.substring(0, context.indexOf(".casm")).lastIndexOf(' ') + 1;
		else
			return -1;
		if (index < 0)
			return 0;
		return index;
	}

	private static int parseLineNumber(String context) {
		int beginIndex = indexOfCasmFilename(context);
		
		if (beginIndex < 0)
			return -1;
		
		context = context.substring(beginIndex);
		
		return Integer.parseInt(context.substring(context.indexOf(":") + 1, context.indexOf(",")));
	}

	private static int parseIndentation(String context) {
		int beginIndex = indexOfCasmFilename(context);
		
		if (beginIndex < 0)
			return -1;
		
		context = context.substring(beginIndex);
		
		return Integer.parseInt(context.substring(context.indexOf(",") + 1, context.indexOf(":", context.indexOf(","))));
	}

	/**
	 * Writes a program into a file with the given filename inside the current
	 * workspace.
	 * @param capi 
	 * 
	 * @param comment for the files header
	 * @param node starting point to extract the program
	 * @param pathOfSpecification path of the program's file
	 */
	public static void writeProgramToFile(ControlAPI capi, String comment, Node node, String pathOfSpecification) {

		File file;

		JFileChooser chooser = new JFileChooser();
		chooser.setToolTipText("Select a file to store the CoreASM code for "+node.toString()+"\n"+comment);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("CoreASM",
				"casm", "coreasm");
		chooser.setFileFilter(filter);
		chooser.setDialogTitle("Store generated CoreASM program from node "+node.toString());
		if (lastChoosenFile!=null)
			chooser.setCurrentDirectory(lastChoosenFile);
		else chooser.setCurrentDirectory(new File(pathOfSpecification));
		int returnVal = chooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				lastChoosenFile = file;
			try {

				PrintWriter out = new PrintWriter(new FileWriter(file));
				out.write("// " + comment + "\n" + getCoreASMProgram(capi, node));
				out.close();

			} catch (FileNotFoundException e) {
				throw new CoreASMIssue(
						"writeParseTreeToFile can not find file for writing!\n"
								+ e.getStackTrace().toString());
			} catch (IOException e) {
				throw new CoreASMIssue(
						"writeParseTreeToFile can not access file for writing!\n"
								+ e.getStackTrace().toString());
			}
		}
	}

	/**
	 * Writes a dot graph into a file with the given filename inside the current
	 * workspace.
	 * 
	 * @param comment for the header
	 * @param node to start from the generation of the dot graph
	 */
	public static void writeParseTreeToFile(String comment, Node node) {

		File file;

		JFileChooser chooser = new JFileChooser();
		chooser.setToolTipText("Select a file to store the CoreASM node "
				+ node.toString() + " as a dot graph\n" + comment);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("dot graph",
				"dot");
		chooser.setFileFilter(filter);
		chooser.setDialogTitle("Store generated CoreASM AST from node "+node.toString());
		if (lastChoosenFile!=null)
			chooser.setCurrentDirectory(lastChoosenFile);
		int returnVal = chooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				lastChoosenFile = file;
			try {
				PrintWriter out = new PrintWriter(new FileWriter(file));
				out.write("// " + comment + "\n" + nodes2dot(node));
				out.close();

			} catch (FileNotFoundException e) {
				throw new CoreASMIssue(
						"writeParseTreeToFile can not find file for writing!\n"
								+ e.getStackTrace().toString());
			} catch (IOException e) {
				throw new CoreASMIssue(
						"writeParseTreeToFile can not access file for writing!\n"
								+ e.getStackTrace().toString());
			}
		}
	}

	/**
	 * build a (hopefully) unique node id
	 * @param node
	 * @return
	 */
	static String getDotNodeId(Node node){
		long hash = 0;
		while (node != null) {
			hash += node.hashCode()+node.getConcreteNodeType().hashCode();
			node = node.getParent();
		}
		return Long.toString( hash ) ;
	}

	/**
	 * Creates dot graph from tree starting from the given node.
	 * 
	 * @param node to start from the dot graph extraction
	 * @return dot graph as String
	 */
	static String nodes2dot(Node node) {
		String dot = "";
		java.util.Date today = new java.util.Date();
		dot += "//" + new java.sql.Timestamp(today.getTime()) + "\n";
		dot += "graph " + node.getToken() + " {\n";
		dot += getLabel(node)+ "\n";
		// recursion
		for (Node child : node.getChildNodes()) {
			dot += nodes2dotRecursion(child);
		}
		dot += "}";
		return dot;
	}

	/**
	 * recursive case of the nodes2dot method (without time stamp and brackets)
	 * 
	 * @param node to continue the dot graph extraction
	 * @return  dot graph as String
	 */
	private static String nodes2dotRecursion(Node node) {
		String dot = "";
		dot += getLabel(node) + "\n";
		dot += "\"" + getDotNodeId(node.getParent()) + "\" -- \""
				+ getDotNodeId(node) + "\"\n";
		// recursion
		for (Node child : node.getChildNodes()) {
			dot += nodes2dotRecursion(child);
		}
		return dot;
	}

	/**
	 * Provide labels for dot graph
	 * 
	 * @param node to get label string from
	 * @return label string
	 */
	private static String getLabel(Node node) {
		String output = "";
		if (node instanceof ASTNode) {
			ASTNode astn = (ASTNode) node;
			if (astn.getToken() != null) {
				output = Tools.convertToEscapeSqeuence(astn.getToken()) + " : "
						+ (astn.getConcreteNodeType());
			} else {
				output = "";
				if (astn.getGrammarRule().isEmpty())
					output += astn.getGrammarClass();
				else
					output += astn.getGrammarRule();
			}
		} else if (node instanceof Node) {
			output = "";
			if (((Node) node).getToken() != null)
				output += Tools.convertToEscapeSqeuence(((Node) node)
						.getToken())
						+ " : "
						+ ((Node) node).getConcreteNodeType();
			else
				output += ((Node) node).getConcreteNodeType();
		} else {
			output = "\n";
			output += node.toString();
		}
		//add linenumber and indentation to dot file label
		if ( getCapi() != null ){
			String context = node.getContext(getCapi().getParser(), getCapi().getSpec());
			int linenumber = Integer.valueOf(context.split("[:,]")[1]);
			int indentation = Integer.valueOf(context.split("[:,]")[2]);
			output = "("+linenumber+","+indentation+") "+output;
		}
		return "\"" + getDotNodeId(node) + "\"" + "[label=\"" + output +"\"]";
	}

	/**
	 * this method return the name of the given ASTNode depending on its kind
	 * @param node ASTNode to retrieve the name from
	 * @return name of the given ASTNode
	 */
	public static String constructName(ASTNode node) {
		String token ="";
		if (	node instanceof MacroCallRuleNode || 
				node instanceof FunctionRuleTermNode)
			token = constructName(node.getFirstASTNode());
		else if (node.getToken() != null)
			token = node.getToken();
		else throw new CoreASMError(
						"no token can be returnd for node"
								+ node.toString());
		return token;
	}

	//public helper rules
	/**
	 * Recursively, finds all Nodes which are part of an aspect and returns them as hash-map.
	 * ASTNodes are stored in a List and can be accessed via its GrammarRule
	 * key: GrammarRule; value: List of ASTNodes
	 * 
	 \dot
		digraph collectASTNodesByGrammar {
		node [shape=circle];
		node1 [fillcolor=lightblue,style=filled,label="" ]
		start -> node1;
		node1 -> start [label="if node" ]
		
	 * @param currentNode is used to start the search
	 * @return a hashmap of all ASTNodes of the current program
	 */
	static HashMap<String, LinkedList<ASTNode>> collectASTNodesByGrammar(
			ASTNode currentNode) {

		// recursive call for all children to add their elements to the HashMap astNodes
		// key: GrammarRule, value List of ASTNodes
		HashMap<String, LinkedList<ASTNode>> astNodes = new HashMap<String, LinkedList<ASTNode>>();
		for (ASTNode child : currentNode.getAbstractChildNodes()) {
			HashMap<String, LinkedList<ASTNode>> tmpHash = collectASTNodesByGrammar(child);
			for (String key : tmpHash.keySet()) {
				if (astNodes.containsKey(key))
					astNodes.get(key).addAll(tmpHash.get(key));
				else
					astNodes.put(key, tmpHash.get(key));
			}
		}

		// add all ASTNodes by default to the list of ASTNodes in the HashMap
		// astNodes:
		/** \TODO {restrict to a relevant subset of nodes by specializing the switch-case statement for <code>GrammarRule</code>} */
		LinkedList<ASTNode> tmpList = new LinkedList<ASTNode>();
		if ( currentNode.getGrammarRule().equals("MacroCallRule") || true){
				//if its the first entry, put it to the hashmap
				if (astNodes.get(currentNode.getGrammarRule()) != null) {
					tmpList.addAll(astNodes.get(currentNode.getGrammarRule()));
				}
				tmpList.add(currentNode);
				// if its not the first entry, replace the existing entry (necessary to extend the list of ASTNodes for an existing key)
				astNodes.put(currentNode.getGrammarRule(), tmpList);
		}
		return astNodes;
	}

	/**
	 * This method collects and returns all directly and transitively nested nodes which match the given name or regular expression
	 * @param node the name of this node will be checked
	 * @param nameOrRegEx the name or regex has to match the token of the given node
	 * @return list of child nodes of the given node which token matches nameOrRegEx 
	 */
	public static LinkedList<Node> getNodesWithName(Node node, String nameOrRegEx)
	{
		//if the given string is no correct regex return null
		if (Pattern.compile(nameOrRegEx) == null)
			return null;
		
		LinkedList<Node> matchingNodes = new LinkedList<Node>();
		if (node.getToken() !=  null)
			if (Pattern.matches(nameOrRegEx, node.getToken())){
				matchingNodes.add(node);
			}
		if ( !node.getChildNodes().isEmpty() ){
			for(Node child : node.getChildNodes())
				matchingNodes.addAll(getNodesWithName(child, nameOrRegEx));
		}
		return matchingNodes;
	}

	/**
	 * This method returns only the first transitively nested node which matches the given name or regular expression
	 * @param node the name of this node will be checked
	 * @param nameOrRegEx the name or regex has to match the token of the given node
	 * @return first child node of the given node which token matches nameOrRegEx 
	 */
	public static Node getFirstChildWithName(Node node, String nameOrRegEx)
	{
		//if the given string is no correct regex return null
		if (Pattern.compile(nameOrRegEx) == null)
			return null;
		
		if (node.getToken() !=  null)
			if (Pattern.matches(nameOrRegEx, node.getToken())){
				return node;
			}
		if ( !node.getChildNodes().isEmpty() ){
			for(Node child : node.getChildNodes())
				getFirstChildWithName(child, nameOrRegEx);
		}
		return null;
	}

	/**
	 * returns the next parent the given type or null starting from the given node
	 * 
	 * @param node	to start searching from
	 * @param cls	class of the parent searching for
	 * @return	parent of type T or null
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getParentOfType(ASTNode node, Class<T> cls){
		ASTNode parent = node.getParent();
		while( parent != null ){
			if (parent.getClass().equals(cls))
				return (T)parent;
			parent = parent.getParent();
		}
		return null;
	}
	/**
	 * returns all ASTNode children of type T starting from the given node 
	 * 
	 * @param node node to start searching from
	 * @param cls class of nodes searching for
	 * @return list of all children of the given node of type cls
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getChildrenOfType(ASTNode node, Class<T> cls){
		LinkedList<T> childrenOfType = new LinkedList<T>();
		List<Node> children = node.getChildNodes();
		for (Node child : children)
			if (child.getClass().equals(cls))
				childrenOfType.add((T)child);
			else if ( child instanceof ASTNode )childrenOfType.addAll(getChildrenOfType((ASTNode)child, cls));
		return childrenOfType;
	}

	// create AST constructs for weaving
	public final static String SEQBLOCKRULE = "SeqBlockRule";
	public final static String BLOCKRULE = "BlockRule";
	public final static String MACROCALLRULE = "MacroCallRule";
	public final static String RETURNRULE ="ReturnRule";
	public final static String LOCALRULE ="LocalRule";
	/**
	 * Creates and returns a MacroRuleCallNode from a given AdviceASTNode
	 * 
	 * @param advice ast node used as a basis to generate a MacroCallRule
	 * @return return a ASTNode for the MacroRuleCall
	 */
	private static MacroCallRuleNode createMacroCallRule(AdviceASTNode advice) {
		// create new macroCallRule which calls the adviceRule
		MacroCallRuleNode adviceMacroCallRule = new MacroCallRuleNode(
				advice.getScannerInfo());
		FunctionRuleTermNode functionRuleNode = new FunctionRuleTermNode(
				advice.getScannerInfo());

		List<Node> adviceSignature = advice.getFirst().getChildNodes();
		for(Node node : adviceSignature)
			if (adviceSignature.indexOf(node)==0)
				//alpha marks the rule call name for the interpreter
				functionRuleNode.addChild("alpha", node.cloneTree());
			else if (adviceSignature.indexOf(node)%2==0)
				//lambda marks all parameter nodes for the interpreter
				functionRuleNode.addChild("lambda", node.cloneTree());
			else
				//operator nodes '(' and ',' must have no marks
				functionRuleNode.addChild(node.cloneTree());
		AspectTools.addChild(adviceMacroCallRule, functionRuleNode);
		return adviceMacroCallRule;
	}

	/**
	 * Creates a node for the given constructType using the provided ScannerInfo
	 * 
	 * @param constructType  type of construct to create, i.e. SeqBlockRuleNode
	 * @param info provides the ScannerInfo for the new ASTNode, i.e. SeqBlockRuleNode
	 * @return rootNode of the new construct
	 */
	@SuppressWarnings("unchecked")
	public static <A extends ASTNode> A create(String constructType,
			ScannerInfo info) {
		if (constructType.equals(SEQBLOCKRULE)) {
				return (A) createSeqBlockRuleNode(info);
		}else if (constructType.equals(BLOCKRULE)) {
				return (A) createBlockRule(info);
		}else if (constructType.equals(MACROCALLRULE)) {
				throw new CoreASMError(
						"MacroCallRule ASTNode can only created from a given AdviceASTNode!");
		}else {
			throw new CoreASMError("Could not create " + constructType);
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Creates a node for the given constructType using the provided ASTNode
	 * 
	 * @param constructType type of construct to create, i.e. SeqBlockRuleNode
	 * @param node provides the ScannerInfo for the new ASTNode, i.e. SeqBlockRuleNode
	 * @return  rootNode of the new construct
	 */
	public static <A extends ASTNode> A create(String constructType,
			ASTNode node) {
		if (constructType.equals(SEQBLOCKRULE) ) {
				LinkedList<ASTNode> insertionList = new LinkedList<ASTNode>();
				insertionList.add(node);
				return (A) create(SEQBLOCKRULE, insertionList);
			}
		else if (constructType.equals(BLOCKRULE)) {
				LinkedList<ASTNode> insertionList = new LinkedList<ASTNode>();
				insertionList.add(node);
				return (A) create(BLOCKRULE, insertionList);
		}else if (constructType.equals(MACROCALLRULE) ){
				if (node instanceof AdviceASTNode)
					return (A) createMacroCallRule((AdviceASTNode) node);
				else
					throw new CoreASMError(
							"MacroCallRule ASTNode can only created from a given AdviceASTNode!");
		}else
			throw new CoreASMError("Could not create " + constructType);
	}

	/**
	 *
	 * @param constructType  type of construct to create, i.e. SeqBlockRuleNode
	 * @param insertionList  list of nodes to be inserted
	 * @return  starting point for the block construct to be generated; continue with insert
	 */
	public static <A extends ASTNode> A create(String constructType,
			LinkedList<ASTNode> insertionList) {
		if (!insertionList.isEmpty()) {
			A node = null;
			if (constructType.equals(SEQBLOCKRULE) ) {
					node = (A) createSeqBlockRuleNode(insertionList.getFirst()
							.getScannerInfo());
			}else if (constructType.equals(BLOCKRULE)) {
					node = (A) createBlockRule(insertionList.getFirst()
							.getScannerInfo());
			}
			if (node != null)
				AspectTools.insert(node, insertionList);
			return node;
		}
		return null;
	}

	/**
	 * This method returns a new SeqBlockRuleNode element.
	 * 
	 * @param info scanner info
	 * @return new seqblock rule element
	 */
	private static SeqBlockRuleNode createSeqBlockRuleNode(ScannerInfo info) {
		SeqBlockRuleNode seqBlockRuleNode = new SeqBlockRuleNode(info);
		Node seqBlock = new Node(AoASMPlugin.PLUGIN_NAME, "seqblock",
				info, Node.KEYWORD_NODE);
		seqBlockRuleNode.addChild(seqBlock);
		return seqBlockRuleNode;
	}

	/**
	 * 
	 * add a node to an existing seqblock as first child or create a new
	 * seqblock, add the given node as first child to this one and add the new
	 * seqblock as second child to the given seqblock
	 * 
	 * @param seqblock
	 * @param node
	 * @return the seqblock which has not yet a second child (next part)
	 */
	private static SeqBlockRuleNode addChildToSeqBlockRuleNode(
			SeqBlockRuleNode seqblock, ASTNode node) {
		if (seqblock.getFirstRule() == null) {
			seqblock.addChild(node);
			return seqblock;

		} else if (seqblock.getFirstRule() != null
				&& seqblock.getSecondRule() == null) {
			// add new seqblock with node as first child
			SeqBlockRuleNode newSeqBlockRule = new SeqBlockRuleNode(
					seqblock.getScannerInfo());
			newSeqBlockRule.addChild(node);
			seqblock.addChild(newSeqBlockRule);
			return newSeqBlockRule;
		}
		return null;
	}

	/**
	 * If the given seqblock has no second child, either the given node or a
	 * skip rule is inserted to complete the seqblock.
	 * 
	 * @param astNode
	 * @param node
	 * @return
	 */
	public static void close(ASTNode astNode, ASTNode node) {
		if (astNode.getGrammarRule().equals(SEQBLOCKRULE)) {
				SeqBlockRuleNode seqBlock;
				if (astNode instanceof SeqBlockRuleNode) {
					seqBlock = (SeqBlockRuleNode) astNode;
					if (seqBlock.getFirstRule() != null
							&& seqBlock.getSecondRule() == null) {
						if (node == null) {
							seqBlock.addChild(new SkipRuleNode(seqBlock
									.getScannerInfo()));
						} else if (node != null) {
							seqBlock.addChild(node);
						}
					}
					Node endseqBlock = new Node(AoASMPlugin.PLUGIN_NAME,
							"endseqblock", seqBlock.getScannerInfo(),
							Node.KEYWORD_NODE);
					seqBlock.addChild(endseqBlock);
				}
		}else{}
	}

	/**
	 * Create a BlockRule element and return it.
	 * 
	 * @param info
	 * @return
	 */
	private static ASTNode createBlockRule(ScannerInfo info) {
		ASTNode node = new ASTNode(BlockRulePlugin.PLUGIN_NAME,
				ASTNode.RULE_CLASS, "BlockRule", null, info);
		Node par = new Node(AoASMPlugin.PLUGIN_NAME, "par", info,
				Node.KEYWORD_NODE);
		Node endpar = new Node(AoASMPlugin.PLUGIN_NAME, "endpar", info,
				Node.KEYWORD_NODE);
		node.addChild(par);
		node.addChild(endpar);
		return node;
	}

	/**
	 * Generic insert method for ASTNodes like SeqBlockNode or BlockRule,...
	 * 
	 * @param node
	 * @param insertion
	 * @return modified node
	 */
	@SuppressWarnings("unchecked")
	public static <A extends ASTNode> A insert(ASTNode node, ASTNode insertion) {
		if (node.getGrammarRule().equals(BLOCKRULE)) {
			AspectTools.addChildAfter(node, node.getFirstCSTNode(),
						insertion.getToken(), insertion);
		}else if (node.getGrammarRule().equals(SEQBLOCKRULE)) {
				if (node instanceof SeqBlockRuleNode)
					return (A) AspectTools.addChildToSeqBlockRuleNode(
							(SeqBlockRuleNode) node, insertion);
		}
		return (A) node;
	}

	/**
	 * Generic insert method for lists of ASTNodes into constructs like
	 * SeqBlockNode or BlockRule,...
	 * 
	 * @param node
	 * @param insertionsList
	 * @return rootNode of the modified construct where the elements have been
	 *         inserted
	 */
	private static <A extends ASTNode> A insert(ASTNode node, LinkedList<ASTNode> insertionsList) {
		if (node.getGrammarRule().equals(BLOCKRULE)) {
				for (ASTNode insertion : insertionsList) {
					AspectTools.insert(node, insertion);
				}
		}else if(node.getGrammarRule().equals(SEQBLOCKRULE)){
				if (node instanceof SeqBlockRuleNode)
					if (insertionsList.size() > 1) {
						return AspectTools.insert(
								AspectTools.insert(node,
										insertionsList.remove()),
								insertionsList);
					} else {
						node = AspectTools
								.insert(node, insertionsList.remove());
						AspectTools.close(node, null);
					}
		}
		return (A) node;
	}

	/**used for debugging purpose within @see getText(..) */
	 public static Stack<TextScope> scopes = new Stack<TextScope>();

	/**
	 * the method reproduces the text from the AST of a specification by using context information about each nodes which provides the linenumber and indentation
	 * @param n		node to start/continue the text extraction from
	 * @param capi	current capi holding the current parser and the related original specification
	 * @param ts	a text scope is used to deal with node insertions which break the total order of text fragments
	 * @return		the recreated text from the ast (without) comments and tokens which were eliminated by the scanner/parser
	 */
	public static String getText(Node n, ControlAPI capi, TextScope ts) {

		/* get the context information for formated text reproduction */
		//@{
		String context = n.getContext(capi.getParser(), capi.getSpec());
		int linenumber = parseLineNumber(context);
		int indentation = parseIndentation(context);
		//@}

		/* initialize output variable text*/
		String text = "";

		/* non-recursive case - node is a leaf of the AST:
		 * insert new linebreaks before token and increase
		 * insert new spaces/tabs before token
		 * insert token */
		if (n.getChildNodes().isEmpty()) {
			if (! (ts.getLineNumber() > linenumber)){
				while (ts.getLineNumber() < linenumber) {  // missing lines are inserted
					text += ts.newLine();
				}
				while (ts.getIndentation() < indentation)  // indentation correction
					text += ts.insertText(" ");
				if ( n.getToken() != null )
					text += ts.insertText(n.getToken());
			}
		} else { /* recursive case - node has children */

			/* if AST has been modified so that the current node's line number
			 * is less then the current line number.
			 * Than, a new text scope (c.f. @see TextScope) is created to align
			 * new tokens relatively to the scope's line number and indenation */
			if (ts.getLineNumber() > linenumber) {
				ts = new TextScope(n, ts);
				scopes.push(ts);//for debugging

				/* recursion - call getText for each child node of this node*/
				for (Node child : n.getChildNodes()) {
					text += getText(child, capi, ts);
				}

				/* restore the text scope used which has been used
				 * before the recursion */
				ts = ts.getPreviousScope();
				scopes.pop();//for debugging
			} else { /* recursion for unmodified ASTs */
				for (Node child : n.getChildNodes()) {
					text += getText(child, capi, ts);
				}
			}
		}
		return text;
	}
}

/**
 * the class TextScope is used to store the information about the current alignment context used to @see getText from an AST
 * @author Marcel Dausend */
class TextScope{

	TextScope previousTextScope;
	int lineNumber;
	int indentation;
	/* additional information which may be useful during debugging */
	Node beginOfScope;

	/**
	 * a text scope has a node where it has been create and maybe a previous scope
	 * @param n			node where the text scope starts
	 * @param scope		preceeding text scope
	 */
	public TextScope(Node n, TextScope scope){
		this(n);
		this.previousTextScope = scope;
	}

	/**
	 * the first text scope starts with an initail node n
	 * @param n	node where the text scope starts
	 */
	public TextScope(Node n) {
		this.beginOfScope  = n;
		this.lineNumber = 1;
		this.indentation = 1;
		this.previousTextScope = null;
	}

	/**
	 * return a line break and increments the indentation
	 * @return	"\n"
	 */
	public String newLine(){
		indentation=1;
		lineNumber++;
		return "\n";
	}

	/**
	 * the current text scope with its information as String
	 * @return information about the text scope (without the previous scope)
	 */
	public String toString(){
		return this.beginOfScope.toString()+" ( "+this.lineNumber+", "+this.indentation+") ";
	}

	/**
	 * return the given text and increases the indentation
	 * @param text
	 * @return	text
	 */
	public String insertText(String text){
		indentation += text.length();
		return text;
	}

	/**
	 *
	 * @return current indentation of this text scope
	 */
	public int getIndentation(){
		return indentation;
	}

	/**
	 *
	 * @return	current line number of this text scope
	 */
	public int getLineNumber(){
		return lineNumber;
	}

	/**
	 * returns true if the given node is the node where the text scope has been initialized
	 * @param n node to compare against
	 * @return	true if the text scope start with Node n, else false
	 */
	public boolean isBeginOfScope(Node n){
		return n.equals(beginOfScope);
	}

	/**
	 * returns the previous text scope or null, if no previous text scope exists
	 * @return previous text scope
	 */
	public TextScope getPreviousScope(){
		return previousTextScope;
	}

}