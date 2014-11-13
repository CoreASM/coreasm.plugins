package org.coreasm.plugins.assertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.compiler.interfaces.CompilerPlugin;
import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.EngineException;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.absstorage.BooleanElement;
import org.coreasm.engine.absstorage.UpdateMultiset;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter;
import org.coreasm.engine.interpreter.InterpreterException;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.KernelServices;
import org.coreasm.engine.parser.GrammarRule;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.parser.ParserTools.ArrayParseMap;
import org.coreasm.engine.plugin.ExtensionPointPlugin;
import org.coreasm.engine.plugin.InitializationFailedException;
import org.coreasm.engine.plugin.InterpreterPlugin;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugin.Plugin;

/**
 * Plugin that introduces the concept of assertions into CoreASM
 * @author Michael Stegmaier
 *
 */
public class AssertionPlugin extends Plugin implements ParserPlugin, InterpreterPlugin, ExtensionPointPlugin {
	static final String PLUGIN_NAME = AssertionPlugin.class.getSimpleName();
	static final VersionInfo VERSION_INFO = new VersionInfo(0, 0, 1, "beta");
	
	public static final String KEYWORD_ASSERT = "assert";
	public static final String KEYWORD_INVARIANT = "invariant";
	public static final String OPERATOR_MESSAGE = ":";
	
	private static final String[] KEYWORDS = new String[] { KEYWORD_ASSERT, KEYWORD_INVARIANT };
	private static final String[] OPERATORS = new String[] { OPERATOR_MESSAGE };
	
	private Map<EngineMode, Integer> targetModes;
	
	private Map<String, GrammarRule> parsers;
	
	private Set<InvariantNode> invariants;
	
	@Override
	public VersionInfo getVersionInfo() {
		return VERSION_INFO;
	}

	@Override
	public void initialize() throws InitializationFailedException {
	}

	@Override
	public String[] getKeywords() {
		return KEYWORDS;
	}

	@Override
	public Set<Parser<? extends Object>> getLexers() {
		return Collections.emptySet();
	}

	@Override
	public String[] getOperators() {
		return OPERATORS;
	}

	@Override
	public Parser<Node> getParser(String nonterminal) {
		return null;
	}

	@Override
	public Map<String, GrammarRule> getParsers() {
		if (parsers == null) {
			parsers = new HashMap<String, GrammarRule>();
			KernelServices kernel = (KernelServices)capi.getPlugin("Kernel").getPluginInterface();
			
			Parser<Node> termParser = kernel.getTermParser();
			ParserTools pTools = ParserTools.getInstance(capi);
			
			Parser<Node> assertParser = Parsers.array(
				pTools.getKeywParser(KEYWORD_ASSERT, PLUGIN_NAME),
				termParser,
				Parsers.array(
						pTools.getOprParser(OPERATOR_MESSAGE),
						termParser).optional()
			).map(
			new ArrayParseMap(PLUGIN_NAME) {
				public Node map(Object[] vals) {
					Node node = new AssertNode(((Node)vals[0]).getScannerInfo());
					addChildren(node, vals);
					return node;
				}
			});
			parsers.put("Rule", new GrammarRule("AssertRule", "'assert' Term (':' Term)?", assertParser, PLUGIN_NAME));
			Parser<Node> invariantParser = Parsers.array(
				pTools.getKeywParser(KEYWORD_INVARIANT, PLUGIN_NAME),
				termParser,
				Parsers.array(
						pTools.getOprParser(OPERATOR_MESSAGE),
						termParser).optional()
			).map(
			new ArrayParseMap(PLUGIN_NAME) {
				public Node map(Object[] vals) {
					Node node = new InvariantNode(((Node)vals[0]).getScannerInfo());
					addChildren(node, vals);
					return node;
				}
			});
			parsers.put(Kernel.GR_HEADER, new GrammarRule("InvariantDeclaration", "'invariant' Term (':' Term)?", invariantParser, PLUGIN_NAME));
		}
		return parsers;
	}

	@Override
	public ASTNode interpret(Interpreter interpreter, ASTNode pos) throws InterpreterException {
		if (pos instanceof AssertNode) {
			AssertNode assertNode = (AssertNode)pos;
			if (!assertNode.getTerm().isEvaluated())
				return assertNode.getTerm();
			if (!(assertNode.getTerm().getValue() instanceof BooleanElement))
				throw new CoreASMError("The value of an assertion term must be a BooleanElement but was " + assertNode.getTerm().getValue() + ".", assertNode.getTerm());
			BooleanElement value = (BooleanElement)assertNode.getTerm().getValue();
			if (!value.getValue()) {
				if (assertNode.getMessageTerm() == null)
					throw new CoreASMError("Assertion " + assertNode.getTerm().unparseTree() + " failed.", assertNode);
				if (!assertNode.getMessageTerm().isEvaluated())
					return assertNode.getMessageTerm();
				throw new CoreASMError("Assertion " + assertNode.getTerm().unparseTree() + " failed with message: " + assertNode.getMessageTerm().getValue(), assertNode);
			}
			capi.getInterpreter().getInterpreterInstance().clearTree(assertNode);
		}
		pos.setNode(null, new UpdateMultiset(), null);
		return pos;
	}

	@Override
	public Map<EngineMode, Integer> getTargetModes() {
		if (targetModes == null) {
			targetModes = new HashMap<EngineMode, Integer>();
			targetModes.put(EngineMode.emInitializingState, DEFAULT_PRIORITY);
			targetModes.put(EngineMode.emStepSucceeded, DEFAULT_PRIORITY);
		}
		return targetModes;
	}

	@Override
	public Map<EngineMode, Integer> getSourceModes() {
		return Collections.emptyMap();
	}

	@Override
	public void fireOnModeTransition(EngineMode source, EngineMode target) throws EngineException {
		switch (target) {
		case emInitializingState:
			invariants = new HashSet<InvariantNode>();
			ASTNode node = capi.getParser().getRootNode().getFirst();
			while (node != null) {
				if (node instanceof InvariantNode)
					invariants.add((InvariantNode)node);
				node = node.getNext();
			}
			break;
		case emStepSucceeded:
			Interpreter interpreter = capi.getInterpreter().getInterpreterInstance();
			for (InvariantNode invariant : invariants) {
				interpreter.interpret(invariant.getTerm(), interpreter.getSelf());
				if (!(invariant.getTerm().getValue() instanceof BooleanElement))
					throw new CoreASMError("The value of an invariant term must be a BooleanElement but was " + invariant.getTerm().getValue() + ".", invariant.getTerm());
				BooleanElement value = (BooleanElement)invariant.getTerm().getValue();
				capi.getInterpreter().getInterpreterInstance().clearTree(invariant);
				if (!value.getValue()) {
					if (invariant.getMessageTerm() == null)
						throw new CoreASMError("Invariant " + invariant.getTerm().unparseTree() + " violated.", invariant);
					interpreter.interpret(invariant.getMessageTerm(), interpreter.getSelf());
					throw new CoreASMError("Invariant " + invariant.getTerm().unparseTree() + " violated with message: " + invariant.getMessageTerm().getValue(), invariant);
				}
			}
			break;
		default:
			break;
		}
	}

}
