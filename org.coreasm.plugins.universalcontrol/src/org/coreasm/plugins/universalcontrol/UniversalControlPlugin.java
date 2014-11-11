package org.coreasm.plugins.universalcontrol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.absstorage.AbstractStorage;
import org.coreasm.engine.absstorage.BooleanElement;
import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.Update;
import org.coreasm.engine.absstorage.UpdateMultiset;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter;
import org.coreasm.engine.interpreter.InterpreterException;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.kernel.KernelServices;
import org.coreasm.engine.parser.GrammarRule;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.parser.ParserTools.ArrayParseMap;
import org.coreasm.engine.plugin.InitializationFailedException;
import org.coreasm.engine.plugin.InterpreterPlugin;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugin.Plugin;
import org.coreasm.engine.plugins.number.NumberElement;
import org.coreasm.util.Tools;

/**
 * Plugin implementing the Universal Control Construct
 * @author Michael Stegmaier
 *
 */
public class UniversalControlPlugin extends Plugin implements ParserPlugin, InterpreterPlugin {
	static final String PLUGIN_NAME = UniversalControlPlugin.class.getSimpleName();
	static final VersionInfo VERSION_INFO = new VersionInfo(0, 0, 1, "beta");

	public static final String KEYWORD_DO = "do";

	public static final String KEYWORD_ALL = "all";
	public static final String KEYWORD_ANY = "any";
	public static final String KEYWORD_SINGLE = "single";

	public static final String KEYWORD_FIXED = "fixed";
	public static final String KEYWORD_VARIABLE = "variable";
	public static final String KEYWORD_SELECTION = "selection";

	public static final String KEYWORD_FOREVER = "forever";
	public static final String KEYWORD_ONCE = "once";
	public static final String KEYWORD_TIMES = "times";

	public static final String KEYWORD_RESETTING = "resetting";
	public static final String KEYWORD_ON = "on";

	public static final String KEYWORD_PARALLELLY = "parallelly";
	public static final String KEYWORD_SEQUENTIALLY = "sequentially";
	public static final String KEYWORD_STEPWISE = "stepwise";

	public static final String KEYWORD_IF = "if";
	public static final String KEYWORD_WHILE = "while";
	
	public static final String KEYWORD_END = "end";

	private static final String[] KEYWORDS = new String[] { KEYWORD_DO,
															KEYWORD_ALL, KEYWORD_ANY, KEYWORD_SINGLE,
															KEYWORD_VARIABLE, KEYWORD_FIXED, KEYWORD_SELECTION,
															KEYWORD_ONCE, KEYWORD_FOREVER, KEYWORD_TIMES,
															KEYWORD_RESETTING, KEYWORD_ON,
															KEYWORD_PARALLELLY, KEYWORD_SEQUENTIALLY, KEYWORD_STEPWISE,
															KEYWORD_IF, KEYWORD_WHILE,
															KEYWORD_END };
	private static final String[] OPERATORS = new String[] { };

	private Map<String, GrammarRule> parsers;

	private Map<Element, Map<Node, Integer>> repetitions;
	private Map<Element, Map<Node, ASTNode[]>> selections;
	private Map<Element, Map<Node, Integer>> currentRules;
	private ThreadLocal<Map<Node, UpdateMultiset>> composedUpdates;

	@Override
	public VersionInfo getVersionInfo() {
		return VERSION_INFO;
	}

	@Override
	public void initialize() throws InitializationFailedException {
		repetitions = new HashMap<Element, Map<Node, Integer>>();
		selections = new HashMap<Element, Map<Node, ASTNode[]>>();
		currentRules = new HashMap<Element, Map<Node, Integer>>();
		composedUpdates = new ThreadLocal<Map<Node, UpdateMultiset>>() {
			@Override
			protected Map<Node, UpdateMultiset> initialValue() {
				return new IdentityHashMap<Node, UpdateMultiset>();
			}
		};
	}

	private Map<Node, Integer> getRepetitions() {
		Map<Node, Integer> repetitions = this.repetitions.get(capi.getInterpreter().getSelf());
		if (repetitions == null) {
			repetitions = new IdentityHashMap<Node, Integer>();
			this.repetitions.put(capi.getInterpreter().getSelf(), repetitions);
		}
		return repetitions;
	}

	private Map<Node, ASTNode[]> getSelections() {
		Map<Node, ASTNode[]> selections = this.selections.get(capi.getInterpreter().getSelf());
		if (selections == null) {
			selections = new IdentityHashMap<Node, ASTNode[]>();
			this.selections.put(capi.getInterpreter().getSelf(), selections);
		}
		return selections;
	}
	
	private Map<Node, Integer> getCurrentRules() {
		Map<Node, Integer> currentRules = this.currentRules.get(capi.getInterpreter().getSelf());
		if (currentRules == null) {
			currentRules = new IdentityHashMap<Node, Integer>();
			this.currentRules.put(capi.getInterpreter().getSelf(), currentRules);
		}
		return currentRules;
	}

	private Map<Node, UpdateMultiset> getComposedUpdates() {
		return composedUpdates.get();
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

			Parser<Node> constantTermParser = kernel.getConstantTermParser();
			Parser<Node> termParser = kernel.getTermParser();
			Parser<Node> ruleParser = kernel.getRuleParser();
			ParserTools pTools = ParserTools.getInstance(capi);
			
			Parser<Serializable> selectionParser = Parsers.or(
					pTools.getKeywParser(KEYWORD_ALL, PLUGIN_NAME),
					Parsers.array(	Parsers.or(pTools.getKeywParser(KEYWORD_ANY, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_SINGLE, PLUGIN_NAME)),
									Parsers.array(	Parsers.or(pTools.getKeywParser(KEYWORD_VARIABLE, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_FIXED, PLUGIN_NAME)).optional(),
													pTools.getKeywParser(KEYWORD_SELECTION, PLUGIN_NAME))));
			Parser<Object[]> repetitionParser = Parsers.array(	Parsers.or(	pTools.getKeywParser(KEYWORD_FOREVER, PLUGIN_NAME),
																			pTools.getKeywParser(KEYWORD_ONCE, PLUGIN_NAME),
																			Parsers.array(constantTermParser, pTools.getKeywParser(KEYWORD_TIMES, PLUGIN_NAME))),
																Parsers.array(pTools.getKeywParser(KEYWORD_RESETTING, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_ON, PLUGIN_NAME), termParser).optional());
			Parser<Node> computationParser = Parsers.or(pTools.getKeywParser(KEYWORD_PARALLELLY, PLUGIN_NAME),
														pTools.getKeywParser(KEYWORD_SEQUENTIALLY, PLUGIN_NAME),
														pTools.getKeywParser(KEYWORD_STEPWISE, PLUGIN_NAME));
			Parser<Object[]> conditionParser = Parsers.array(	Parsers.or(pTools.getKeywParser(KEYWORD_IF, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_WHILE, PLUGIN_NAME)),
																termParser);
			
			Parser<Object[]> parser1 = Parsers.array(
					selectionParser,
					repetitionParser.optional().atomic(),
					computationParser.optional().atomic());
			
			Parser<Object[]> parser2 = Parsers.array(
					selectionParser,
					computationParser.optional().atomic(),
					repetitionParser.optional().atomic());
			
			Parser<Object[]> parser3 = Parsers.array(
					repetitionParser,
					selectionParser.optional().atomic(),
					computationParser.optional().atomic());
			
			Parser<Object[]> parser4 = Parsers.array(
					repetitionParser,
					computationParser.optional().atomic(),
					selectionParser.optional().atomic());
			
			Parser<Object[]> parser5 = Parsers.array(
					computationParser,
					selectionParser.optional().atomic(),
					repetitionParser.optional().atomic());
			
			Parser<Object[]> parser6 = Parsers.array(
					computationParser,
					repetitionParser.optional().atomic(),
					selectionParser.optional().atomic());

			Parser<Node> parser = Parsers.array(
				pTools.getKeywParser(KEYWORD_DO, PLUGIN_NAME),
				Parsers.or(	parser1,
							parser2,
							parser3,
							parser4,
							parser5,
							parser6).optional(),
				conditionParser.optional(),
				pTools.plus(ruleParser),
				pTools.getKeywParser(KEYWORD_END, PLUGIN_NAME).optional()
			).map(
			new ArrayParseMap(PLUGIN_NAME) {
				public Node map(Object[] vals) {
					Node node = new UniversalControlNode(((Node)vals[0]).getScannerInfo());
					addChildren(node, vals);
					return node;
				}
			});
			parsers.put("Rule", new GrammarRule("UniversalControlRule", "'do' ('all' | (('any' | 'single') ('variable' | 'fixed')? 'selection'))? ('once' | 'forever' | (ConstantTerm 'times'))? ('resetting' 'on' Term)? ('parallely' | 'sequentialy' | 'stepwise')? (('if' | 'while) Term)? Rule+ 'end'?", parser, PLUGIN_NAME));
		}
		return parsers;
	}

	@Override
	public ASTNode interpret(Interpreter interpreter, ASTNode pos) throws InterpreterException {
		AbstractStorage storage = capi.getStorage();
		if (pos instanceof UniversalControlNode) {
			UniversalControlNode node = (UniversalControlNode)pos;

			Integer repetitions = getRepetitions().get(pos);
			if (repetitions == null)
				repetitions = 0;
			else if (node.getResetCondition() != null) {
				if (!node.getResetCondition().isEvaluated())
					return node.getResetCondition();
				if (!(node.getResetCondition().getValue() instanceof BooleanElement))
					throw new CoreASMError("The value of the reset condition must be a BooleanElement but was " + node.getResetCondition().getValue() + ".", node.getResetCondition());
				BooleanElement value = (BooleanElement)node.getResetCondition().getValue();
				if (value.getValue())
					repetitions = 0;
			}
			
			Node repetitionNode = node.getRepetitionNode();
			if (repetitionNode != null) {
				int repetitionCount;
				if (KEYWORD_ONCE.equals(repetitionNode.getToken()))
					repetitionCount = 1;
				else {
					if (!(repetitionNode instanceof ASTNode))
						throw new CoreASMError("Illegal node encountered.", repetitionNode);
					ASTNode repetitionASTNode = (ASTNode)repetitionNode;
					if (!repetitionASTNode.isEvaluated())
						return repetitionASTNode;
					if (!(repetitionASTNode.getValue() instanceof NumberElement))
						throw new CoreASMError("The value of the repetition count must be a NumberElement but was " + repetitionASTNode.getValue() + ".", repetitionASTNode);
					NumberElement value = (NumberElement)repetitionASTNode.getValue();
					repetitionCount = (int)value.getValue();
				}
				if (repetitionCount >= 0 && repetitions >= repetitionCount) {
					pos.setNode(null, new UpdateMultiset(), null);
					return pos;
				}
			}

			UpdateMultiset updates = new UpdateMultiset();

			if (node.getCondition() != null && !node.getCondition().isEvaluated()) {
				storage.pushState();
				return node.getCondition();
			}

			boolean conditionMet = true;
			if (node.getCondition() != null) {
				if (!(node.getCondition().getValue() instanceof BooleanElement))
					throw new CoreASMError("The value of the condition must be a BooleanElement but was " + node.getCondition().getValue() + ".", node.getCondition());
				BooleanElement value = (BooleanElement)node.getCondition().getValue();
				conditionMet = value.getValue();
			}

			if (conditionMet) {
				ASTNode[] selection = getSelections().get(pos);
				if (selection == null) {
					ArrayList<ASTNode> rules = new ArrayList<ASTNode>();
					for (ASTNode rule = node.getRuleBlock(); rule != null; rule = rule.getNext())
						rules.add(rule);
					int selectionSize = 1;
					if (KEYWORD_ALL.equals(node.getSelectionKeyword()))
						selectionSize = rules.size();
					else if (KEYWORD_ANY.equals(node.getSelectionKeyword()))
						selectionSize = Tools.randInt(rules.size() + 1);
					selection = new ASTNode[selectionSize];
					getSelections().put(pos, selection);
					for (int rulesToRemove = rules.size() - selectionSize; rulesToRemove > 0; rulesToRemove--)
						rules.remove(Tools.randInt(rules.size()));
					for (int i = 0; i < selection.length; i++)
						selection[i] = rules.get(i);
				}
				if (node.isStepwise()) {
					Integer currentRule = getCurrentRules().get(pos);
					if (currentRule == null || currentRule >= selection.length) {
						currentRule = 0;
						getCurrentRules().put(pos, currentRule);
					}
					selection = new ASTNode[] { selection[currentRule] };
				}

				ASTNode prevRule = null;
				for (ASTNode rule : selection) {
					if (!rule.isEvaluated()) {
						if (node.isSequential() && prevRule != null) {
							Set<Update> aggregatedUpdates = storage.performAggregation(prevRule.getUpdates());
							if (!storage.isConsistent(aggregatedUpdates))
								throw new CoreASMError("Inconsistent updates computed in sequence.", prevRule);
							storage.pushState();
							storage.apply(aggregatedUpdates);
						}
						return rule;
					}
					prevRule = rule;
				}

				if (node.isSequential()) {
					for (int i = 0; i < selection.length; i++) {
						if (i > 0)
							storage.popState();
						ASTNode rule = selection[i];
						updates = storage.compose(updates, rule.getUpdates());
					}
				}
				else {
					for (ASTNode rule : selection)
						updates.addAll(rule.getUpdates());
				}

				if (node.shouldLoop()) {
					UpdateMultiset composedUpdates = getComposedUpdates().get(pos);
					if (composedUpdates == null)
						composedUpdates = new UpdateMultiset();
					Set<Update> aggregatedUpdates = storage.performAggregation(updates);
					if (!storage.isConsistent(aggregatedUpdates))
						throw new CoreASMError("Inconsistent updates computed in loop.", pos);
					storage.apply(aggregatedUpdates);
					getComposedUpdates().put(pos, storage.compose(composedUpdates, updates));
					interpreter.getInterpreterInstance().clearTree(node.getCondition());
					for (ASTNode rule : selection)
						interpreter.getInterpreterInstance().clearTree(rule);
					return pos;
				}
			}

			if (node.getCondition() != null)
				storage.popState();

			if (node.shouldLoop())
				updates = getComposedUpdates().get(pos);
			if (updates == null)
				updates = new UpdateMultiset();

			pos.setNode(null, updates, null);
			
			if (node.isStepwise()) {
				int currentRule = getCurrentRules().get(pos) + 1;
				getCurrentRules().put(pos, currentRule);
				if (currentRule < getSelections().get(pos).length)
					return pos;
			}
			
			if (node.isVariableSelection())
				getSelections().remove(pos);
			
			getRepetitions().put(pos, repetitions + 1);

			return pos;
		}
		pos.setNode(null, new UpdateMultiset(), null);
		return pos;
	}
}