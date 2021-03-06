package org.coreasm.plugins.aspects.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMIssue;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.Node.NameNodeTuple;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.MacroCallRuleNode;
import org.coreasm.engine.kernel.SkipRuleNode;
import org.coreasm.engine.parser.CharacterPosition;
import org.coreasm.engine.plugins.blockrule.BlockRulePlugin;
import org.coreasm.engine.plugins.turboasm.SeqRuleNode;
import org.coreasm.plugins.aspects.AoASMPlugin;
import org.coreasm.plugins.aspects.AspectWeaver;
import org.coreasm.plugins.aspects.GraphViz;
import org.coreasm.plugins.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.PointCutASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.PointCutParameterNode;
import org.coreasm.util.Tools;
import org.coreasm.util.information.InformationDispatcher;
import org.coreasm.util.information.InformationObject.VerbosityLevel;

/**
 * @author Marcel Dausend
 * 
 */
public class AspectTools {

	/** declaration of final static strings */
	///@{
	public final static String RULESIGNATURE = "RuleSignature";	// from SignaturPlugin
	///@}
	/** ControlAPI used to reproduce text or dot output from a given AST */
	private static volatile ControlAPI capi;

	/**
	 * Information dispatcher used to create provide information to other
	 * registered plugins
	 */
	private static InformationDispatcher info = InformationDispatcher.getInstance(AoASMPlugin.PLUGIN_NAME);

	/**
	 * set the ControlAPI used by AspectTools
	 * 
	 * @param capi
	 *            ControlAPI
	 */
	public static synchronized void setCapi(ControlAPI capi)
	{
		AspectTools.capi = capi;
	}

	/**
	 * get the current ControlAPI which has been set for AspectTools
	 * 
	 * @return the currently assigned ControlAPI
	 */
	public static synchronized ControlAPI getCapi() {
		return AspectTools.capi;
	}

	public static String getNodeName(ASTNode node) {
		ASTNode parent = node.getParent();
		if ( parent == null ) throw new CoreASMError("Name of node cannot be returned because it has no parent.",node);
		//find name of node which will be replaced
		String nodeName = Node.DEFAULT_NAME;
		for (NameNodeTuple tuple : parent.getChildNodesWithNames()) {
			if (node == tuple.node)
				nodeName = tuple.name;
		}
		return nodeName;
	}

	/**
	 * wrapper method for methods from node that are used to insert code for
	 * debugging purposes, e.g. create dot graph after insertions
	 */
	///@{
	public static void addChildAfter(Node parent, Node insertionReference, String name, Node node) {
		// original call
		parent.addChildAfter(insertionReference, name, node);

		// additional behavior - create a new dot graph
		List<Node> newNodes = new LinkedList<Node>();
		newNodes.add(node);
		Node root = parent;
		while (root.getParent() != null)
			root = root.getParent();
		AspectTools.createDotGraph(nodes2dot(root), newNodes);
	}

	public static void addChild(Node parent, String name, Node node) {
		// original call depending on the argument 'name'
		if (name == null)
			parent.addChild(node);
		else
			parent.addChild(name, node);
		// additional behavior - create a new dot graph
		List<Node> newNodes = new LinkedList<Node>();
		newNodes.add(node);
		Node root = parent;
		while (root.getParent() != null)
			root = root.getParent();
		AspectTools.createDotGraph(nodes2dot(root), newNodes);
	}

	public static void addChild(Node parent, Node node) {
		addChild(parent, null, node);
	}

	///@}

	///@{
	/** variable used for createDotGraph */
	private static String timestamp = null;
	private static int currentDot = 0;
	private static final boolean CREATE_DOT_FILES = false;

	///@}

	/**
	 * create a dot graph from contained within the given string 'dot' and
	 * highlight the nodes from the list 'changed'
	 * 
	 * @param dot
	 *            description of a dot graph
	 * @param changed
	 *            nodes that should be marked as changed
	 */
	public static void createDotGraph(String dot, List<Node> changed) {
		if (!CREATE_DOT_FILES)
			return;
		
		currentDot++;

		for (Node change : changed) {
			String toMatch = getDotNodeId(change) + " [label";
			dot.replace(toMatch, toMatch + "color=\"red\", ");
		}

		List<String> revised = new LinkedList<String>(Arrays.asList(dot.split("\n")));

		GraphViz gv = new GraphViz();
		Iterator<String> it = revised.iterator();
		while (it.hasNext()) {
			String line = it.next();
			//			if (line.startsWith("graph") /* first line */
			//					|| it.hasNext() == false /* last line */
			//					|| line.contains("keyword")
			//					|| line.contains("operator")
			//					|| line.contains("id"))
				gv.addln(line);
		}

		// possible types are
		// dot, fig, gif, pdf, plain, png, ps, svg
		String type = "dot";
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		// store current time, so that all dot graphs are created
		// within the one directory
		if (timestamp == null)
			timestamp = formatter.format(cal.getTime());

		String tmpDir = System.getProperty("java.io.tmpdir");
		File out = new File(tmpDir + "/" + timestamp + "/out." + currentDot + "." + type);
		out.getParentFile().mkdirs();
		//gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
		try {
			PrintWriter output = new PrintWriter(new FileWriter(out));
			output.write(gv.getDotSource());
			output.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** used to remember the last path that has been selected to store a file */
	private static File lastChoosenFile;

	public static String getRuleSignatureAsCoreASMList(PointCutASTNode astNode) {
		String ruleSignatureAsCoreASMList = "[";
		ASTNode param = astNode.getFirst();
		while (param != null) {
			ruleSignatureAsCoreASMList += "\"" + ((PointCutParameterNode) param).getPattern() + "\"";
			param = param.getNext();
			if (param != null)
				ruleSignatureAsCoreASMList += ", ";
		}
		ruleSignatureAsCoreASMList += " ]";
		return ruleSignatureAsCoreASMList;
	}

	/**
	 * return a rule or function signature as coreasm string
	 * 
	 * @param astNode
	 *            should be a rule definition or function rule term
	 * @return string in coreasm syntax representing the signature of the given
	 *         node
	 */
	public static String getRuleSignatureAsCoreASMList(ASTNode astNode) {
		String ruleSignatureAsCoreASMList = "";
		ASTNode node;
		if (astNode.getGrammarRule().equals(Kernel.GR_RULEDECLARATION) || astNode instanceof FunctionRuleTermNode) {
			if (astNode.getGrammarRule().equals(Kernel.GR_RULEDECLARATION))
			{
				ruleSignatureAsCoreASMList += "[";
			}
			//signature node
			node = astNode.getFirst();
			ASTNode signatureElement;
			//add name of rule/function surrounded by quotes
			ruleSignatureAsCoreASMList += "\"" + node.getFirst().getToken() + "\"";
			//add parameters
			for (int i = 1; i < node.getAbstractChildNodes().size(); i++) {
				signatureElement = node.getAbstractChildNodes().get(i);
				ruleSignatureAsCoreASMList += ", ";
				ruleSignatureAsCoreASMList += getRuleSignatureAsCoreASMList(signatureElement);
				//if its not the last signature element, insert a colon, too.
			}

			if (astNode.getGrammarRule().equals(Kernel.GR_RULEDECLARATION))
			{
				ruleSignatureAsCoreASMList += "]";
			}
		}
		else //ID, NUMBER, BOOLEAN, StringTerm, BooleanTerm, KernelTerms
		{
			ruleSignatureAsCoreASMList = astNode.getToken();
		}
		return ruleSignatureAsCoreASMList;
	}

	private static String getCoreASMProgram(Node rootNode) {
		// First step: Break lines
		String result = rootNode.unparseTree().replace(" use ", "\nuse ").replaceFirst("\nuse ", "\n\nuse ").replace(" include ", "\ninclude ").replace(" init ", "\n\ninit ").replace(" rule ", "\n\nrule ").replace(" function ", "\nfunction ").replace(" derived ",  "\n\nderived ").replace(" universe ", "\nuniverse ").replace(" enum ", "\nenum ");
		result = result.replace(" aspect ", "\naspect ");
		result = result.replace(" assert ", "\nassert ");
		result = result.replace(" seqblock ", "\nseqblock\n").replace(" endseqblock ", "\nendseqblock\n").replace(" endseqblock\n", "\nendseqblock\n");
		result = result.replace(" par ", "\npar\n").replace("\npar ", "\npar\n").replace(" endpar ", "\nendpar\n").replace("\nendpar ", "\nendpar\n").replace(" endpar\n", "\nendpar\n");
		result = result.replace(" seq ", "\nseq\n").replace("\nseq ", "\nseq\n").replace(" next ", "\nnext\n").replace("\nnext ", "\nnext\n").replace(" endseq ", "\nendseq\n").replace("\nendseq ", "\nendseq\n").replace(" endseq\n", "\nendseq\n");
		result = result.replace(" then ", " then\n").replace(" else ", "\nelse\n").replace("\nelse ", "\nelse\n");
		result = result.replace(" skip ", "\nskip\n").replace("\nskip ", "\nskip\n").replace(" skip\n", "\nskip\n");
		result = result.replace(" CoreModule ", "\n\nCoreModule ");
		// Second step: Add indentation
		String[] lines = result.split("\n");
		result = "";
		boolean extraIndentationThen = false;
		boolean extraIndentationElse = false;
		boolean extraIndentationSeq = false;
		boolean extraIndentationNext = false;
		int indentation = 0;
		for (String line : lines) {
			line = line.trim();
			if (indentation > 0 && line.isEmpty())
				continue;
			if (line.startsWith("use") && line.contains("TabBlocks"))
				continue;
			if (line.startsWith("use ") || line.startsWith("include ") || line.startsWith("init ") || line.startsWith("rule ") || line.startsWith("function ") || line.startsWith("derived ") || line.startsWith("universe ") || line.startsWith("enum ")) {
				extraIndentationThen = false;
				extraIndentationElse = false;
				extraIndentationSeq = false;
				extraIndentationNext = false;
				indentation = 0;
			}
			if (extraIndentationNext) {
				result += "\t";
				extraIndentationNext = false;
			}
			else if ("next".equals(line)) {
				extraIndentationSeq = false;
				extraIndentationNext = true;
			}
			else if ("endseq".equals(line))
				extraIndentationSeq = false;
			if (extraIndentationSeq)
				result += "\t";
			if (extraIndentationThen) {
				result += "\t";
				extraIndentationThen = false;
			}
			if (extraIndentationElse) {
				result += "\t";
				if (!line.endsWith("then"))
					extraIndentationElse = false;
			}
			if ("endseqblock".equals(line) || "endpar".equals(line) || "endseq".equals(line))
				indentation--;
			for (int i = 0; i < indentation; i++)
				result += "\t";
			result += line + "\n";
			if ("seqblock".equals(line) || "par".equals(line))
				indentation++;
			if (line.endsWith("then"))
				extraIndentationThen = true;
			if ("else".equals(line))
				extraIndentationElse = true;
			if ("seq".equals(line))
				extraIndentationSeq = true;
		}
		return result;
	}

	/**
	 * Writes a program into a file with the given filename inside the current
	 * workspace.
	 * 
	 * @param capi
	 * 
	 * @param comment
	 *            for the files header
	 * @param node
	 *            starting point to extract the program
	 * @param pathOfSpecification
	 *            path of the program's file
	 */
	public static void writeProgramToFile(ControlAPI capi, String comment, Node node, String pathOfSpecification) {

		File file;

//		JFileChooser chooser = new JFileChooser();
//		chooser.setToolTipText("Select a file to store the CoreASM code for " + node.toString() + "\n" + comment);
//		FileNameExtensionFilter filter = new FileNameExtensionFilter("CoreASM", "casm", "coreasm");
//		chooser.setFileFilter(filter);
//		chooser.setDialogTitle("Store generated CoreASM program from node " + node.toString());
//		if (lastChoosenFile != null)
//			chooser.setCurrentDirectory(lastChoosenFile);
//		else
//			chooser.setCurrentDirectory(new File(pathOfSpecification));
//		int returnVal = chooser.showSaveDialog(null);
//		if (returnVal == JFileChooser.APPROVE_OPTION) {
//			file = chooser.getSelectedFile();
		
		if (pathOfSpecification.contains("-woven."))
			return;
		
		file = new File(pathOfSpecification.substring(0, pathOfSpecification.lastIndexOf('.')) + "-woven.casm");
		lastChoosenFile = file;
		try {

			PrintWriter out = new PrintWriter(new FileWriter(file));
			out.write("// " + comment + "\n" + getCoreASMProgram(node));
			out.close();
			Map<String, String> data = new HashMap<String, String>();
			data.put("filename", file.getAbsolutePath());
			info.createInformation("Woven specification created.", VerbosityLevel.INFO, data);
		}
		catch (FileNotFoundException e) {
			throw new CoreASMIssue("writeParseTreeToFile can not find file for writing!\n"
			        + e.getStackTrace().toString());
		}
		catch (IOException e) {
			throw new CoreASMIssue("writeParseTreeToFile can not access file for writing!\n"
			        + e.getStackTrace().toString());
		}
//		}
	}

	/**
	 * Writes a dot graph into a file with the given filename inside the current
	 * workspace.
	 * 
	 * @param comment
	 *            for the header
	 * @param node
	 *            to start from the generation of the dot graph
	 */
	public static void writeParseTreeToFile(String comment, Node node) {

		File file;

		JFileChooser chooser = new JFileChooser();
		chooser.setToolTipText("Select a file to store the CoreASM node " + node.toString() + " as a dot graph\n"
		        + comment);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("dot graph", "dot");
		chooser.setFileFilter(filter);
		chooser.setDialogTitle("Store generated CoreASM AST from node " + node.toString());
		if (lastChoosenFile != null)
			chooser.setCurrentDirectory(lastChoosenFile);
		int returnVal = chooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();
			lastChoosenFile = file;
			try {
				PrintWriter out = new PrintWriter(new FileWriter(file));
				out.write("// " + comment + "\n" + nodes2dot(node));
				out.close();

			}
			catch (FileNotFoundException e) {
				throw new CoreASMIssue("writeParseTreeToFile can not find file for writing!\n"
				        + e.getStackTrace().toString());
			}
			catch (IOException e) {
				throw new CoreASMIssue("writeParseTreeToFile can not access file for writing!\n"
				        + e.getStackTrace().toString());
			}
		}
	}

	/**
	 * build a (hopefully) unique node id
	 * 
	 * @param node
	 * @return
	 */
	static String getDotNodeId(Node node) {
		long hash = 0;
		while (node != null) {
			hash += node.hashCode() + node.getConcreteNodeType().hashCode();
			node = node.getParent();
		}
		return Long.toString(hash);
	}

	/**
	 * Creates dot graph from tree starting from the given node.
	 * 
	 * @param node
	 *            to start from the dot graph extraction
	 * @return dot graph as String
	 */
	public synchronized static String nodes2dot(Node node) {
		if ( !CREATE_DOT_FILES ) return null;
		String dot = "";
		java.util.Date today = new java.util.Date();
		dot += "//" + new java.sql.Timestamp(today.getTime()) + "\n";
		dot += "graph " + node.getToken() + " {\n";
		dot += getLabel(node) + "\n";
		// recursion
		for (Node child : node.getChildNodes()) {
			dot += nodes2dotRecursion(child, 1);
		}
		dot += "}";
		return dot;
	}

	/**
	 * recursive case of the nodes2dot method (without time stamp and brackets)
	 * 
	 * @param node
	 *            to continue the dot graph extraction
	 * @return dot graph as String
	 */
	private synchronized static String nodes2dotRecursion(Node node, int depth) {
		String dot = "";
		String indent = "";
		int localDepth = depth;
		while (localDepth > 0) {
			indent += "    ";
			localDepth--;
		}
		dot += indent + getLabel(node) + "\n";
		dot += indent + "\"" + getDotNodeId(node.getParent()) + "\" -- \"" + getDotNodeId(node) + "\"\n";
		// recursion
		for (Node child : node.getChildNodes()) {
			dot += nodes2dotRecursion(child, depth + 1);
		}
		return dot;
	}

	/**
	 * Provide labels for dot graph
	 * 
	 * @param node
	 *            to get label string from
	 * @return label string
	 */
	private synchronized static String getLabel(Node node) {
		String output = "";
		if (node instanceof ASTNode) {
			ASTNode astn = (ASTNode) node;
			if (astn.getToken() != null) {
				output = Tools.convertToEscapeSqeuence(astn.getToken()) + " : " + (astn.getConcreteNodeType());
			}
			else {
				output = "";
				if (astn.getGrammarRule().isEmpty())
					output += astn.getGrammarClass();
				else
					output += astn.getGrammarRule();
			}
		}
		else if (node instanceof Node) {
			output = "";
			if (node.getToken() != null)
				output += Tools.convertToEscapeSqeuence(node.getToken()) + " : "
				        + node.getConcreteNodeType();
			else
				output += node.getConcreteNodeType();
		}
		else {
			output = "\n";
			output += node.toString();
		}
		// add linenumber and indentation to dot file label
		if (AspectTools.getCapi() != null) {
			CharacterPosition pos = node.getScannerInfo().getPos(capi.getParser().getPositionMap());
			int linenumber = capi.getSpec().getLine(pos.line).line;
			int indentation = pos.column;
			output = "(" + linenumber + "," + indentation + ") " + output;
		}
		return "\"" + getDotNodeId(node) + "\"" + "[label=\"" + output + "\"]";
	}

	// public helper rules
	/**
	 * Recursively, finds all Nodes which are part of an aspect and returns them
	 * as hash-map. ASTNodes are stored in a List and can be accessed via its
	 * GrammarRule key: GrammarRule; value: List of ASTNodes
	 * 
	 * \dot digraph collectASTNodesByGrammar { node [shape=circle]; node1
	 * [fillcolor=lightblue,style=filled,label="" ] start -> node1; node1 ->
	 * start [label="if node" ]
	 * \enddot
	 * 
	 * @param currentNode
	 *            is used to start the search
	 * @return a hashmap of all ASTNodes of the current program
	 */
	public static void collectASTNodesByGrammar(HashMap<String, LinkedList<ASTNode>> astNodes, ASTNode currentNode) {
		// add all ASTNodes by default to the list of ASTNodes in the HashMap
		// astNodes:
		LinkedList<ASTNode> list = astNodes.get(currentNode.getGrammarRule());
		if (list == null) {
			list = new LinkedList<ASTNode>();
			astNodes.put(currentNode.getGrammarRule(), list);
		}
		// extend the list of ASTNodes for an existing key
		list.add(currentNode);
		
		// recursive call for all children to add their elements to the HashMap astNodes
		// key: GrammarRule, value List of ASTNodes
		for (ASTNode child : currentNode.getAbstractChildNodes())
			collectASTNodesByGrammar(astNodes, child);
	}

	/**
	 * This method collects and returns all directly and transitively nested
	 * nodes which match the given name or regular expression
	 * 
	 * @param node
	 *            the name of this node will be checked
	 * @param nameOrRegEx
	 *            the name or regex has to match the token of the given node
	 * @return list of child nodes of the given node which token matches
	 *         nameOrRegEx
	 */
	public static LinkedList<Node> getNodesWithName(Node node, String nameOrRegEx) {
		// if the given string is no correct regex return null
		if (Pattern.compile(nameOrRegEx) == null)
			return null;

		LinkedList<Node> matchingNodes = new LinkedList<Node>();
		if (node.getToken() != null)
			if (Pattern.matches(nameOrRegEx, node.getToken())) {
				matchingNodes.add(node);
			}
		if (!node.getChildNodes().isEmpty()) {
			for (Node child : node.getChildNodes())
				matchingNodes.addAll(getNodesWithName(child, nameOrRegEx));
		}
		return matchingNodes;
	}

	/**
	 * This method returns only the first transitively nested node which matches
	 * the given name or regular expression
	 * 
	 * @param node
	 *            the name of this node will be checked
	 * @param nameOrRegEx
	 *            the name or regex has to match the token of the given node
	 * @return first child node of the given node which token matches
	 *         nameOrRegEx
	 */
	public static Node getFirstChildWithName(Node node, String nameOrRegEx) {
		// if the given string is no correct regex return null
		if (Pattern.compile(nameOrRegEx) == null)
			return null;

		if (node.getToken() != null)
			if (Pattern.matches(nameOrRegEx, node.getToken())) {
				return node;
			}
		if (!node.getChildNodes().isEmpty()) {
			for (Node child : node.getChildNodes())
				getFirstChildWithName(child, nameOrRegEx);
		}
		return null;
	}

	/**
	 * returns the next parent the given type or null starting from the given
	 * node
	 * 
	 * @param node
	 *            to start searching from
	 * @param cls
	 *            class of the parent searching for
	 * @return parent of type T or null
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getParentOfType(ASTNode node, Class<T> cls) {
		ASTNode parent = node.getParent();
		while (parent != null) {
			if (parent.getClass().equals(cls))
				return (T) parent;
			parent = parent.getParent();
		}
		return null;
	}

	/**
	 * returns all ASTNode children of type T starting from the given node
	 * 
	 * @param node
	 *            node to start searching from
	 * @param cls
	 *            class of nodes searching for
	 * @return list of all children of the given node of type cls
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getChildrenOfType(ASTNode node, Class<T> cls) {
		LinkedList<T> childrenOfType = new LinkedList<T>();
		List<Node> children = node.getChildNodes();
		for (Node child : children)
			if (child.getClass().equals(cls))
				childrenOfType.add((T) child);
			else if (child instanceof ASTNode)
				childrenOfType.addAll(getChildrenOfType((ASTNode) child, cls));
		return childrenOfType;
	}

	// create AST constructs for weaving
	public final static String SEQBLOCKRULE = "SeqRule";
	public final static String BLOCKRULE = "BlockRule";
	public final static String MACROCALLRULE = "MacroCallRule";
	public final static String RETURNTERM = "ReturnTerm";
	public final static String LOCALRULE = "LocalRule";

	/**
	 * Creates and returns a MacroRuleCallNode from a given AdviceASTNode
	 * 
	 * @param advice
	 *            ast node used as a basis to generate a MacroCallRule
	 * @return return a ASTNode for the MacroRuleCall
	 */
	private static MacroCallRuleNode createMacroCallRule(AdviceASTNode advice) {
		// create new macroCallRule which calls the adviceRule
		MacroCallRuleNode adviceMacroCallRule = new MacroCallRuleNode(advice.getScannerInfo());
		FunctionRuleTermNode functionRuleNode = new FunctionRuleTermNode(advice.getScannerInfo());

		List<Node> adviceSignature = advice.getFirst().getChildNodes();
		for (Node node : adviceSignature)
			if (adviceSignature.indexOf(node) == 0)
				// alpha marks the rule call name for the interpreter
				functionRuleNode.addChild("alpha", node.cloneTree());
			else if (adviceSignature.indexOf(node) % 2 == 0)
				// lambda marks all parameter nodes for the interpreter
				functionRuleNode.addChild("lambda", node.cloneTree());
			else
				// operator nodes '(' and ',' must have no marks
				functionRuleNode.addChild(node.cloneTree());
		AspectTools.addChild(adviceMacroCallRule, functionRuleNode);
		return adviceMacroCallRule;
	}
	
	public static boolean isRuleName(String name) {
		for (ASTNode ruleDefinition : AspectWeaver.getInstance().getRuleDefinitions()) {
			if (name.equals(ruleDefinition.getFirst().getFirst().getToken()))
				return true;
		}
		return false;
	}

	/**
	 * Creates a node for the given constructType using the provided ScannerInfo
	 * 
	 * @param constructType
	 *            type of construct to create, i.e. SeqBlockRuleNode
	 * @param info
	 *            provides the ScannerInfo for the new ASTNode, i.e.
	 *            SeqBlockRuleNode
	 * @return rootNode of the new construct
	 */
	@SuppressWarnings("unchecked")
	public static <A extends ASTNode> A create(String constructType, ScannerInfo info) {
		if (constructType.equals(SEQBLOCKRULE)) {
			return (A) createSeqBlockRuleNode(info);
		}
		else if (constructType.equals(BLOCKRULE)) {
			return (A) createBlockRule(info);
		}
		else if (constructType.equals(MACROCALLRULE)) {
			throw new CoreASMError("MacroCallRule ASTNode can only created from a given AdviceASTNode!");
		}
		else {
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
	public static <A extends ASTNode> A create(String constructType, ASTNode node) {
		if (constructType.equals(SEQBLOCKRULE)) {
			LinkedList<ASTNode> insertionList = new LinkedList<ASTNode>();
			insertionList.add(node);
			return (A) create(SEQBLOCKRULE, insertionList);
		}
		else if (constructType.equals(BLOCKRULE)) {
			LinkedList<ASTNode> insertionList = new LinkedList<ASTNode>();
			insertionList.add(node);
			return (A) create(BLOCKRULE, insertionList);
		}
		else if (constructType.equals(MACROCALLRULE)) {
			if (node instanceof AdviceASTNode)
				return (A) createMacroCallRule((AdviceASTNode) node);
			else
				throw new CoreASMError("MacroCallRule ASTNode can only created from a given AdviceASTNode!");
		}
		else
			throw new CoreASMError("Could not create " + constructType);
	}

	/**
	 * 
	 * @param constructType
	 *            type of construct to create, i.e. SeqBlockRuleNode
	 * @param insertionList
	 *            list of nodes to be inserted
	 * @return starting point for the block construct to be generated; continue
	 *         with insert
	 */
	public static <A extends ASTNode> A create(String constructType, LinkedList<ASTNode> insertionList) {
		if (!insertionList.isEmpty()) {
			A node = null;
			if (constructType.equals(SEQBLOCKRULE)) {
				node = (A) createSeqBlockRuleNode(insertionList.getFirst().getScannerInfo());
			}
			else if (constructType.equals(BLOCKRULE)) {
				node = (A) createBlockRule(insertionList.getFirst().getScannerInfo());
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
	 * @param info
	 *            scanner info
	 * @return new seqblock rule element
	 */
	private static SeqRuleNode createSeqBlockRuleNode(ScannerInfo info) {
		SeqRuleNode seqBlockRuleNode = new SeqRuleNode(info);
		Node seqBlock = new Node(AoASMPlugin.PLUGIN_NAME, "seqblock", info, Node.KEYWORD_NODE);
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
	private static SeqRuleNode addChildToSeqBlockRuleNode(SeqRuleNode seqblock, ASTNode node) {
		if (seqblock.getFirstRule() == null) {
			seqblock.addChild(node);
			return seqblock;

		}
		else if (seqblock.getFirstRule() != null && seqblock.getSecondRule() == null) {
			// add new seqblock with node as first child
			SeqRuleNode newSeqBlockRule = new SeqRuleNode(seqblock.getScannerInfo());
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
			SeqRuleNode seqBlock;
			if (astNode instanceof SeqRuleNode) {
				seqBlock = (SeqRuleNode) astNode;
				if (seqBlock.getFirstRule() != null && seqBlock.getSecondRule() == null) {
					if (node == null) {
						seqBlock.addChild(new SkipRuleNode(seqBlock.getScannerInfo()));
					}
					else if (node != null) {
						seqBlock.addChild(node);
					}
				}
				Node endseqBlock = new Node(AoASMPlugin.PLUGIN_NAME, "endseqblock", seqBlock.getScannerInfo(),
				        Node.KEYWORD_NODE);
				seqBlock.addChild(endseqBlock);
			}
		}
		else {
		}
	}

	/**
	 * Create a BlockRule element and return it.
	 * 
	 * @param info
	 * @return
	 */
	private static ASTNode createBlockRule(ScannerInfo info) {
		ASTNode node = new ASTNode(BlockRulePlugin.PLUGIN_NAME, ASTNode.RULE_CLASS, "BlockRule", null, info);
		Node par = new Node(AoASMPlugin.PLUGIN_NAME, "par", info, Node.KEYWORD_NODE);
		Node endpar = new Node(AoASMPlugin.PLUGIN_NAME, "endpar", info, Node.KEYWORD_NODE);
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
			AspectTools.addChildAfter(node, node.getFirstCSTNode(), Node.DEFAULT_NAME, insertion);
		}
		else if (node.getGrammarRule().equals(SEQBLOCKRULE)) {
			if (node instanceof SeqRuleNode)
				return (A) AspectTools.addChildToSeqBlockRuleNode((SeqRuleNode) node, insertion);
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
		}
		else if (node.getGrammarRule().equals(SEQBLOCKRULE)) {
			if (node instanceof SeqRuleNode)
				if (insertionsList.size() > 1) {
					return AspectTools.insert(AspectTools.insert(node, insertionsList.remove()), insertionsList);
				}
				else {
					node = AspectTools.insert(node, insertionsList.remove());
					AspectTools.close(node, null);
				}
		}
		return (A) node;
	}

	public static InformationDispatcher getInformationDispatcher() {
		return info;
	}
}