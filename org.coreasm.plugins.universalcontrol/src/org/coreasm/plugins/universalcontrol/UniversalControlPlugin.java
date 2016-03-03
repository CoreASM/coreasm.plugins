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
import org.coreasm.engine.absstorage.FunctionElement;
import org.coreasm.engine.absstorage.InvalidLocationException;
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

	public static final String KEYWORD_PERFORM = "perform";

	public static final String KEYWORD_ALL = "all";
	public static final String KEYWORD_ANY = "any";
	public static final String KEYWORD_SINGLE = "single";

	public static final String KEYWORD_FIXED = "fixed";
	public static final String KEYWORD_VARIABLE = "variable";
	public static final String KEYWORD_NON_EMPTY = "nonempty";
	public static final String KEYWORD_SELECTION = "selection";

	public static final String KEYWORD_ALWAYS = "always";
	
	public static final String KEYWORD_ATMOST = "atmost";
	public static final String KEYWORD_TIMES = "times";
	public static final String KEYWORD_UNTIL = "until";
	public static final String KEYWORD_NO_UPDATES = "noupdates";
	public static final String KEYWORD_NO_CHANGE = "nochange";

	public static final String KEYWORD_RESETTING = "resetting";
	public static final String KEYWORD_ON = "on";

	public static final String KEYWORD_IN = "in";
	public static final String KEYWORD_PARALLEL = "parallel";
	public static final String KEYWORD_SEQUENCE = "sequence";
	public static final String KEYWORD_RULE_BY_RULE = "rulebyrule";
	public static final String KEYWORD_STEPWISE = "stepwise";

	public static final String KEYWORD_IF = "if";
	public static final String KEYWORD_WHILE = "while";
	public static final String KEYWORD_ITERATE = "iterate";
	
	public static final String KEYWORD_END = "end";

	private static final String[] KEYWORDS = new String[] { KEYWORD_PERFORM,
															KEYWORD_ALL, KEYWORD_ANY, KEYWORD_NON_EMPTY, KEYWORD_SINGLE,
															KEYWORD_VARIABLE, KEYWORD_FIXED, KEYWORD_SELECTION,
															KEYWORD_ALWAYS,
															KEYWORD_ATMOST, KEYWORD_TIMES, KEYWORD_UNTIL, KEYWORD_NO_UPDATES, KEYWORD_NO_CHANGE,
															KEYWORD_RESETTING, KEYWORD_ON,
															KEYWORD_IN, KEYWORD_PARALLEL, KEYWORD_SEQUENCE, KEYWORD_RULE_BY_RULE, KEYWORD_STEPWISE,
															KEYWORD_IF, KEYWORD_WHILE, KEYWORD_UNTIL, KEYWORD_ITERATE,
															KEYWORD_END };
	private static final String[] OPERATORS = new String[] { };

	private Map<String, GrammarRule> parsers;

	private Map<Element, Map<Node, Integer>> repetitions;
	private Map<Element, Map<Node, ASTNode[]>> selections;
	private Map<Element, Map<Node, Integer>> currentRules;
	private ThreadLocal<Map<Node, UpdateMultiset>> composedUpdates;
	private ThreadLocal<Map<Node, Object>> stepLocks;

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
		stepLocks = new ThreadLocal<Map<Node, Object>>() {
			@Override
			protected Map<Node, Object> initialValue() {
				return new IdentityHashMap<Node, Object>();
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
	
	private void lockStep(Node node) {
		Map<Node, Object> locks = stepLocks.get();
		locks.put(node, null);
	}
	
	private boolean isStepLocked(Node node) {
		return stepLocks.get().containsKey(node);
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
					Parsers.array(	Parsers.or(	Parsers.array(pTools.getKeywParser(KEYWORD_ANY, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_NON_EMPTY, PLUGIN_NAME).optional()), 
												pTools.getKeywParser(KEYWORD_SINGLE, PLUGIN_NAME)),
									Parsers.array(	Parsers.or(pTools.getKeywParser(KEYWORD_VARIABLE, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_FIXED, PLUGIN_NAME)),
													pTools.getKeywParser(KEYWORD_SELECTION, PLUGIN_NAME))));
			Parser<Object[]> repetitionParser = Parsers.array(	Parsers.or(	pTools.getKeywParser(KEYWORD_ALWAYS, PLUGIN_NAME),
																			Parsers.array(pTools.getKeywParser(KEYWORD_ATMOST, PLUGIN_NAME), constantTermParser, pTools.getKeywParser(KEYWORD_TIMES, PLUGIN_NAME)),
																			Parsers.array(pTools.getKeywParser(KEYWORD_UNTIL, PLUGIN_NAME), Parsers.or(	pTools.getKeywParser(KEYWORD_NO_UPDATES, PLUGIN_NAME),
																																						pTools.getKeywParser(KEYWORD_NO_CHANGE, PLUGIN_NAME)))),
																Parsers.array(pTools.getKeywParser(KEYWORD_RESETTING, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_ON, PLUGIN_NAME), termParser).optional());
			Parser<Serializable> computationParser = Parsers.or(Parsers.array(	pTools.getKeywParser(KEYWORD_IN, PLUGIN_NAME),
																				Parsers.or(pTools.getKeywParser(KEYWORD_PARALLEL, PLUGIN_NAME), pTools.getKeywParser(KEYWORD_SEQUENCE, PLUGIN_NAME))),
																				pTools.getKeywParser(KEYWORD_RULE_BY_RULE, PLUGIN_NAME),
																				pTools.getKeywParser(KEYWORD_STEPWISE, PLUGIN_NAME));
			Parser<Serializable> conditionParser = Parsers.or(	Parsers.array(	Parsers.or(	pTools.getKeywParser(KEYWORD_IF, PLUGIN_NAME),
																							pTools.getKeywParser(KEYWORD_WHILE, PLUGIN_NAME)),
																				termParser),
																pTools.getKeywParser(KEYWORD_ITERATE, PLUGIN_NAME));
			
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
				pTools.getKeywParser(KEYWORD_PERFORM, PLUGIN_NAME),
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
			parsers.put("Rule", new GrammarRule("UniversalControlRule", "'perform' ('all' | ((('any' 'nonempty'?) | 'single') ('variable' | 'fixed') 'selection'))? ('always' | ('atmost' ConstantTerm 'times') | 'until' ('noupdates' | 'nochange'))? ('resetting' 'on' Term)? (('in' ('parallel' | 'sequence')) | 'rulebyrule' | 'stepwise')? (('if' | 'while' | 'iterate') Term)? Rule+ 'end'?", parser, PLUGIN_NAME));
		}
		return parsers;
	}

	@Override
	public ASTNode interpret(Interpreter interpreter, ASTNode pos) throws InterpreterException {
		AbstractStorage storage = capi.getStorage();
		if (pos instanceof UniversalControlNode) {
			UniversalControlNode node = (UniversalControlNode)pos;
			
//			if (isStepLocked(node)) {
//				pos.setNode(null, new UpdateMultiset(), null);
//				return pos;
//			}

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
				if (KEYWORD_NO_UPDATES.equals(repetitionNode.getToken())
				|| KEYWORD_NO_CHANGE.equals(repetitionNode.getToken()))
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
					if (repetitionCount < 0)
						throw new CoreASMError("The value of the repetition count must be possitive but was " + repetitionCount + ".", repetitionASTNode);
				}
				if (repetitions >= repetitionCount) {
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
					else if (KEYWORD_ANY.equals(node.getSelectionKeyword())) {
						if (node.isNonEmptySelection())
							selectionSize = Tools.randInt(rules.size()) + 1;
						else
							selectionSize = Tools.randInt(rules.size() + 1);
					}
					selection = new ASTNode[selectionSize];
					getSelections().put(pos, selection);
					for (int rulesToRemove = rules.size() - selectionSize; rulesToRemove > 0; rulesToRemove--)
						rules.remove(Tools.randInt(rules.size()));
					for (int i = 0; i < selection.length; i++)
						selection[i] = rules.get(i);
				}
				if (node.isRuleByRule() || node.isStepwise()) {
					Integer currentRule = getCurrentRules().get(pos);
					if (currentRule == null || currentRule >= selection.length) {
						lockStep(pos);
						currentRule = 0;
						getCurrentRules().put(pos, currentRule);
					}
					else if (node.isStepwise() && !isStepLocked(pos)) {
						currentRule++;
						if (currentRule < selection.length)
							lockStep(pos);
						else
							selection = new ASTNode[0];
						getCurrentRules().put(pos, currentRule);
					}
					if (selection.length > 0)
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
					if (!node.isIterate() || !updates.isEmpty()) {
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
					}
					else {
						storage.popState();
						pos.setNode(null, getComposedUpdates().remove(pos), null);
					}
					return pos;
				}
			}

			if (node.getCondition() != null)
				storage.popState();

			if (node.shouldLoop())
				updates = getComposedUpdates().get(pos);
			if (updates == null)
				updates = new UpdateMultiset();
			
			if (node.isStepwise() && !isStepLocked(pos))
				updates = null;

			pos.setNode(null, updates, null);
			
			if (conditionMet && node.isStepwise()) {
				int currentRule = getCurrentRules().get(pos);
				if (currentRule < getSelections().get(pos).length) {
					interpreter.clearTree(getSelections().get(pos)[getCurrentRules().get(pos)]);
					return pos;
				}
				getCurrentRules().remove(pos);
			}
			
			if (conditionMet && node.isRuleByRule()) {
				int currentRule = getCurrentRules().get(pos) + 1;
				getCurrentRules().put(pos, currentRule);
				if (currentRule < getSelections().get(pos).length)
					return pos;
			}
			
			if (node.isVariableSelection())
				getSelections().remove(pos);
			
			getRepetitions().put(pos, repetitions + 1);
			if (repetitionNode != null) {
				if (KEYWORD_NO_UPDATES.equals(repetitionNode.getToken()))
					getRepetitions().put(pos, (updates.isEmpty() ? 1 : 0));
				else if (KEYWORD_NO_CHANGE.equals(repetitionNode.getToken())) {
					boolean noChange = true;
					for (Update u : updates) {
						try {
							FunctionElement function = capi.getStorage().getFunction(u.loc.name);
							if (function.isReadable()) {
								if (!u.value.equals(capi.getStorage().getValue(u.loc))) {
									noChange = false;
									break;
								}
							}
						} catch (InvalidLocationException e) {
							throw new CoreASMError("Encountered invalid location: " + u.loc, pos);
						}
					}
					getRepetitions().put(pos, (noChange ? 1 : 0));
				}
			}

			return pos;
		}
		else if (pos instanceof TrueGuardNode) {
			pos.setNode(null, null, BooleanElement.TRUE);
			return pos;
		}
		pos.setNode(null, new UpdateMultiset(), null);
		return pos;
	}
}
