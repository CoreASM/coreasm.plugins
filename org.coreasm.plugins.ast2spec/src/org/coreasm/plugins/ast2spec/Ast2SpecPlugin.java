package org.coreasm.plugins.ast2spec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.coreasm.plugins.ast2spec.ui.ExportAstDialog;
import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.CoreASMIssue;
import org.coreasm.engine.EngineException;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.absstorage.BooleanElement;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.plugin.ExtensionPointPlugin;
import org.coreasm.engine.plugin.InitializationFailedException;
import org.coreasm.engine.plugin.Plugin;
import org.coreasm.engine.plugins.signature.DerivedFunctionNode;
import org.coreasm.engine.plugins.signature.FunctionNode;

public class Ast2SpecPlugin extends Plugin implements ExtensionPointPlugin {
	public static final VersionInfo VERSION_INFO = new VersionInfo(0, 0, 1, "alpha");

	private static final String INIT_RULE_NAME = "initializeAST";

	private static final LinkedList<String> UNIVERSES = new LinkedList<String>();
	private static final String[] FUNCTIONS = { "parent: NODE -> NODE", "firstChild: NODE -> NODE",
			"nextSibling: NODE -> NODE", "class: NODE -> STRING", "grammarRule: NODE -> STRING",
			"concreteType: NODE -> STRING", "token: NODE -> STRING", "fkind: STRING -> FUNCTIONKIND", "fname: NODE -> STRING",
			//auxiliary functions
			"file: NODE -> STRING", "spec: NODE -> STRING", "lineNumber: NODE -> NUMBER", "indentation: NODE -> NUMBER"
	};
	private static final String[] ENUMS = {"FUNCTIONKIND = {DERIVED_FUNCTION, MONITORED_FUNCTION}"};

	private String nodeNames = "";

	private static File lastChoosenFile;

	private Node rootnode;

	private boolean withDefinitions;

	private boolean withMetaInformation;

	@Override
	public VersionInfo getVersionInfo() {
		return VERSION_INFO;
	}

	@Override
	public Map<EngineMode, Integer> getTargetModes() {
		return Collections.emptyMap();
	}

	@Override
	public Map<EngineMode, Integer> getSourceModes() {
		HashMap<EngineMode, Integer> sourceModes = new HashMap<EngineMode, Integer>();
		sourceModes.put(EngineMode.emParsingSpec, 10);
		return sourceModes;
	}

	@Override
	public void initialize() throws InitializationFailedException {
	}

	@Override
	public void fireOnModeTransition(EngineMode source, EngineMode target) throws EngineException {
		if (source.equals(EngineMode.emParsingSpec) && target.equals(EngineMode.emIdle))
			writeParseTreeToFile("created by ast2spec", capi.getParser().getRootNode(), new File(capi.getSpec()
					.getAbsolutePath()));
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
	public void writeParseTreeToFile(String comment, Node node, File specFile) {

		File file;

		ExportAstDialog chooser = new ExportAstDialog();
		chooser.setToolTipText("Select a file to store the CoreASM node "
				+ node.toString() + " as a dot graph\n" + comment);
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Coreasm file", new String[] { "casm", "coreasm" });
		chooser.setFileFilter(filter);
		chooser.setDialogTitle("Store generated CoreASM AST from node "
				+ specFile.getName());
		if (lastChoosenFile != null)
			chooser.setCurrentDirectory(lastChoosenFile);
		else
			chooser.setCurrentDirectory(specFile.getParentFile());

		int returnVal = chooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();
			this.withDefinitions = chooser.withDefinitions();
			this.withMetaInformation = chooser.withLineNumbers();
			lastChoosenFile = file;
			try {

				PrintStream stream = new PrintStream(file);
				printSpec(stream, node);
				stream.close();

			}
			catch (FileNotFoundException e) {
				throw new CoreASMIssue(
						"writeParseTreeToFile can not find file for writing!\n"
								+ e.getStackTrace().toString());
			}
		}
	}

	private void printSpec(PrintStream stream, Node rootnode) {
		this.rootnode = rootnode;
		printSpecHeader(stream, rootnode);
		stream.println();
		printFunctions(stream, rootnode);
		printEnums(stream, rootnode);
		//		stream.println();
		//		printAuxiliaryRules(stream);
		stream.println();
		printInitRule(stream, rootnode);
		stream.println();
		stream.print("enum NODE_NAMES_" + rootnode.getChildNodes().get(1).getToken() + " = {" + nodeNames + "}");
		if (withDefinitions) {
			stream.println();
			printUniverses(stream);
		}
		nodeNames = "";
	}

	void printSpecHeader(PrintStream stream, Node rootnode) {
		stream.println("CoreModule " + rootnode.getChildNodes().get(1).getToken() + "AST");
		stream.println();
		stream.println("use Standard");
	}

	private void printUniverses(PrintStream stream) {
		if (!UNIVERSES.contains("ASTNODE"))
			UNIVERSES.add("ASTNODE");
		if (!UNIVERSES.contains("NODE"))
			UNIVERSES.add("NODE");
		for (String universe : UNIVERSES)
			stream.println("universe " + universe);
	}

	private void printFunctions(PrintStream stream, Node rootnode) {
		for (String function : FUNCTIONS)
			stream.println("function " + function);
	}
	
	private void printEnums(PrintStream stream, Node rootnode) {
		for (String enumerate : ENUMS)
			stream.println("enum " + enumerate);
	}

	private void printInitRule(PrintStream stream, Node node) {
		//stream.println("init " + INIT_RULE_NAME);
		//stream.println();
		stream.println("rule " + INIT_RULE_NAME + node.getChildNodes().get(1).getToken() + " = {");
		printAST(stream, node);
		stream.println("}");
	}

	private void printAST(PrintStream stream, Node node) {
		printNode(stream, node);
	}

	private void printNode(PrintStream stream, Node node) {

		// Add node name to String of nodeNames
		if (!nodeNames.isEmpty())
			nodeNames += ", ";
		nodeNames += nodeToString(node);

		// Add node to universe NODE
		printAssignment(stream, "NODE", nodeToString(node),
				BooleanElement.TRUE_NAME, 1);

		if (node.getToken() != null)
			printAssignment(stream, "token", nodeToString(node),
					"\"" + node.getToken() + "\"", 2);

		// set the concreteType of a node
		if (node.getConcreteNodeType() != null) {
			if (!UNIVERSES.contains(node.getConcreteNodeType().toUpperCase()))
				UNIVERSES.add(node.getConcreteNodeType().toUpperCase());

			printAssignment(stream, node.getConcreteNodeType().toUpperCase(),
					nodeToString(node), BooleanElement.TRUE_NAME, 2);

			printAssignment(stream, "concreteType", nodeToString(node), ("\""
					+ node.getConcreteNodeType() + "\"").toUpperCase(), 2);
		}

		/**
		 * meta information
		 */
		if (this.withMetaInformation) {
			Node spec = node;
			String aspectName = "";
			while (spec != null) {
				if (spec instanceof ASTNode && "AspectASTNode".equals(((ASTNode) spec).getGrammarRule()))
				{
					if (((ASTNode) spec).getFirst() != null)
						aspectName = ((ASTNode) spec).getFirst().getToken();
					break;
				}
				spec = spec.getParent();
			}

			if (spec != null)
				printAssignment(stream, "spec", nodeToString(node), "\""
						+ (aspectName == null ? "AspectSpecfication" : aspectName) + "\"", 2);
			else
				printAssignment(stream, "spec", nodeToString(node), "\""
						+ "OriginalSpecification" + "\"", 2);
			if (this.rootnode.getChildNodes().get(1).getToken() != null)
				printAssignment(stream, "file", nodeToString(node), "\""
						+ this.rootnode.getChildNodes().get(1).getToken() + "\"", 2);
		}

		/**
		 * relevant only for ASTNodes
		 */
		if (node instanceof ASTNode) {
			ASTNode astnode = (ASTNode) node;
			// Add node to universe ASTNODE
			printAssignment(stream, "ASTNODE", nodeToString(astnode),
					BooleanElement.TRUE_NAME, 2);

			if ((astnode.getGrammarRule() != null) && astnode.getGrammarRule().equals("Signature")) {
				ASTNode signature = astnode.getFirst();
				if (signature instanceof DerivedFunctionNode)
					printAssignment(stream, "fkind", nodeToString(astnode), "DERIVED_FUNCTION", 2);
				else if (signature instanceof FunctionNode) {
					FunctionNode function = (FunctionNode)signature;
					printAssignment(stream, "fkind", nodeToString(astnode), "\""+function.getFunctionClass().toString().substring(2).toUpperCase()+"_FUNCTION\"", 2);
				}
			}

			if (astnode instanceof FunctionRuleTermNode) {
				FunctionRuleTermNode frNode = (FunctionRuleTermNode)astnode;
				if (frNode.hasName())
					printAssignment(stream, "fname", nodeToString(astnode), "\""+frNode.getName()+"\"", 2);
			}

			// functions which have no content for getGrammarRule()
			if (astnode.getGrammarRule().length() == 0) {
				if (!UNIVERSES.contains(astnode.getGrammarClass().toUpperCase()
						.concat("_CLASS")))
					UNIVERSES.add(astnode.getGrammarClass().toUpperCase()
							.concat("_CLASS"));
				// Add the current Node to its Universe
				printAssignment(stream, astnode.getGrammarClass().toUpperCase()
						.concat("_CLASS"), nodeToString(astnode),
						BooleanElement.TRUE_NAME, 3);
				// set the function grammarRule for the current node
				printAssignment(
						stream,
						"grammarRule",
						nodeToString(astnode),
						"\""
								+ astnode.getGrammarClass().toUpperCase()
										.concat("_CLASS") + "\"", 3);
			}
			else if (astnode.getGrammarRule().equals("NUMBER")) {
				// set the function grammarRule for the current node
				printAssignment(stream, "grammarRule", nodeToString(astnode),
						"\"" + astnode.getGrammarRule().toUpperCase() + "\"", 3);
			}
			else {
				// Add a universe for the GrammarRule CoreASM (clashes with
				// keyword
				// add a universe for each different GrammarRule
				if (!UNIVERSES.contains(astnode.getGrammarRule().toUpperCase()))
					UNIVERSES.add(astnode.getGrammarRule().toUpperCase());
				// Add the current Node to its Universe
				if (!astnode.getGrammarRule().equalsIgnoreCase(node.getConcreteNodeType()))//prevent duplicates
					printAssignment(stream, astnode.getGrammarRule().toUpperCase(),
							nodeToString(astnode), BooleanElement.TRUE_NAME, 3);
				// set the function grammarRule for the current node
				printAssignment(stream, "grammarRule", nodeToString(astnode),
						"\"" + astnode.getGrammarRule().toUpperCase() + "\"", 3);
			}

			printAssignment(stream, "class", nodeToString(astnode), "\""
					+ astnode.getGrammarClass() + "\"", 3);

		}

		/**
		 * relevant for all kind of nodes
		 */

		if (node.getParent() != null) {
			printAssignment(stream, "parent", nodeToString(node),
					nodeToString(node.getParent()), 2);
		}
		else
			printAssignment(stream, "parent", nodeToString(node), "undef", 2);

		if (node.getFirstCSTNode() != null)
			printAssignment(stream, "firstChild", nodeToString(node),
					nodeToString(node.getFirstCSTNode()), 2);

		Node prevChild = null;
		for (Node child : node.getChildNodes()) {

			if (prevChild != null)
				printAssignment(stream, "nextSibling", nodeToString(prevChild),
						nodeToString(child), 2);
			printNode(stream, child);
			prevChild = child;
		}

	}

	private static void printAssignment(PrintStream stream, String function, String arg, String value, int indentation) {
		for (int i = indentation; i > 0; i--)
			stream.print('\t');
		stream.println(function + "(" + arg + ") := " + value);
	}

	private static String nodeToString(Node node) {
		return "Node" + node.hashCode();
	}
}
