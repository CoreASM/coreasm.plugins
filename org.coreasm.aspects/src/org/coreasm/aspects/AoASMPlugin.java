/**
 * 
 * @file AoASMPlugin.java
 * 
 * 
 * @author Copyright (C) 2012 Marcel Dausend
 * 
 * @date Last modified by $Author: Marcel Dausend $ on $Date: 2014-03-18 $.
 * 
 * @version 0.0.2 (alpha)
 * 
 *          Licensed under the Academic Free License version 3.0
 *          http://www.opensource.org/licenses/afl-3.0.php
 *          http://www.coreasm.org/afl-3.0.php
 * 
 */
package org.coreasm.aspects;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.aspects.pointcutmatching.AgentPointCutASTNode;
import org.coreasm.aspects.pointcutmatching.ArgsASTNode;
import org.coreasm.aspects.pointcutmatching.AspectASTNode;
import org.coreasm.aspects.pointcutmatching.BinAndASTNode;
import org.coreasm.aspects.pointcutmatching.BinOrASTNode;
import org.coreasm.aspects.pointcutmatching.Binding;
import org.coreasm.aspects.pointcutmatching.CFlowASTNode;
import org.coreasm.aspects.pointcutmatching.CFlowBelowASTNode;
import org.coreasm.aspects.pointcutmatching.CFlowTopASTNode;
import org.coreasm.aspects.pointcutmatching.CallASTNode;
import org.coreasm.aspects.pointcutmatching.ExpressionASTNode;
import org.coreasm.aspects.pointcutmatching.GetASTNode;
import org.coreasm.aspects.pointcutmatching.NamedPointCutASTNode;
import org.coreasm.aspects.pointcutmatching.NamedPointCutDefinitionASTNode;
import org.coreasm.aspects.pointcutmatching.NotASTNode;
import org.coreasm.aspects.pointcutmatching.PointCutParameterNode;
import org.coreasm.aspects.pointcutmatching.SetASTNode;
import org.coreasm.aspects.pointcutmatching.WithinASTNode;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.EngineException;
import org.coreasm.engine.Specification;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.absstorage.BackgroundElement;
import org.coreasm.engine.absstorage.FunctionElement;
import org.coreasm.engine.absstorage.RuleElement;
import org.coreasm.engine.absstorage.UniverseElement;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.KernelServices;
import org.coreasm.engine.parser.CharacterPosition;
import org.coreasm.engine.parser.GrammarRule;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.plugin.ExtensionPointPlugin;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugin.Plugin;
import org.coreasm.engine.plugin.VocabularyExtender;
import org.coreasm.engine.plugins.blockrule.BlockRulePlugin;
import org.coreasm.util.information.InformationDispatcher;
import org.coreasm.util.information.InformationObject.VerbosityLevel;

//@formatter:off
/**
* @mainpage Aspect-Orientation-Plugin for CoreASM
*
* This plug-in depends on the SignaturePlugin and extends CoreASM to
* AspectOriented ASM programs.
*
* This plug-in provides the following additional ASM constructs:
* <ol>
*     <li><b>aspect*</b></li>
*     <ol>
*         <li><b>signature*</b></li>
*         <li><b>ruleDefinition*</b>
*         <li><b>advice*</b></li>
*         <ol>
*             <li><b>locator</b></li>
*             <ol>
*                 <li><b>pointcut</b></li>
*                 <li><b>:</b></li>
*                 <li><b>rule block</b></li>
*             </ol>
*         </ol>
*     </ol>
* </ol>
*
* \todo we still have some work todo!
* \bug and there are still some bugs :(
* \note dot can be used within comment via 
* /dot 
*     digraph name{}
* /enddot. 
*
* Please use colorscheme = pastel19 @see
* http://www.graphviz.org/doc/info/colors.html
* \attention Should only be used by those who know what they are doing.
* \warning Not certified for use within mission critical or life sustaining systems.
* Note the following example code:
* \code
*     Window win = new Window(parent);
*     win.show();
* \endcode
*
* \exception StringIndexOutOfRangeException if index is not between
* <code>0</code> and
* <code>length() - 1</code>.
* @see NewClass
* @see http://java.sun.com implementation of 
* @see org.coreasm.engine.plugin.ParserPlugin,
* @see org.coreasm.engine.plugin.VocabularyExtender and
* @see org.coreasm.engine.plugin.ExtensionPointPlugin extends 
* @see org.coreasm.engine.plugin.Plugin
*/
//@formatter:on
public class AoASMPlugin extends Plugin
		implements
		ParserPlugin,
		VocabularyExtender,
		ExtensionPointPlugin {

	/** version of the aspect-oriented CoreASM-Plugin */
	private static final VersionInfo VERSION_INFO = new VersionInfo(0, 0, 3,
			"alpha");
	/** name of the aspect-oriented CoreASM-Plugin */
	public static final String PLUGIN_NAME = AoASMPlugin.class
			.getSimpleName();
	/** the Plugin-names where this plugin depends on */
	private Set<String> dependencyNames = null;

	/**
	 * map of parsers defined in \link getParsers() \endlink
	 */
	private Map<String, GrammarRule> parsers = null;

	/**
	 * Information dispatcher used to create provide information to other
	 * registered plugins
	 */
	private static InformationDispatcher info = InformationDispatcher.getInstance(PLUGIN_NAME);

	/**
	 * @name Keywords and Operators
	 *       final strings used in the \link getParsers() \endlink method
	 */
	///@{
	/** \brief keyword used for the parser */
	private static final String KW_ASPECT = "aspect";
	private static final String KW_ADVICE = "advice";
	private static final String KW_BEFORE = "before";
	private static final String KW_AFTER = "after";
	private static final String KW_AROUND = "around";
	private static final String KW_BEGIN = "begin";
	private static final String KW_END = "end";
	private static final String KW_RULECALL = "call";
	private static final String KW_GET = "get";
	private static final String KW_SET = "set";
	private static final String KW_WITHIN = "within";
	private static final String KW_ARGS = "args";
	private static final String KW_CFLOW = "cflow";
	private static final String KW_CFLOWBELOW = "cflowbelow";
	private static final String KW_CFLOWTOP = "cflowtop";
	private static final String KW_BY = "by";
	public static final String KW_WITHOUT = "without";
	private static final String KW_AS = "as";
	private static final String KW_POINTCUT = "pointcut";
	private static final String KW_AGENT = "agent";
	///@}

	///@{
	/** \brief operator used for the parser */
	private static final String OP_COLON = ":";
	private static final String OP_AND = "and";
	private static final String OP_OR = "or";
	private static final String OP_NOT = "not";
	///@}

	///@{
	/**
	 * collection of keyword and operator strings use by the parser in \link
	 * getParsers() \endlink
	 */
	private final String[] keywords = { KW_ASPECT, KW_ADVICE, KW_BEFORE,
			KW_AFTER, KW_AROUND, KW_BEGIN, KW_END, KW_RULECALL, KW_GET, KW_SET, KW_WITHIN,
			KW_ARGS, KW_CFLOW, KW_CFLOWBELOW, KW_CFLOWTOP, KW_BY, KW_WITHOUT, OP_AND, OP_OR,
			OP_NOT, KW_AS, KW_POINTCUT, KW_AGENT };

	/**
	 * \attention operators OP_AND, OP_OR, and OP_NOT, are treated as keywords
	 * because CoreASM does not allow duplicate definitions of operators in the
	 * global context.
	 */
	private final String[] operators = { OP_COLON };

	///@}

	/**
	 * {@inheritDoc} notify the user that the plugin has been loaded
	 * successfully
	 * (output
	 * appears on the debug console) and add an interpreter listener to the
	 * ControlAPI capi
	 */
	@Override
	public void initialize() {

	}

	/**
	 * @return return the version info of the plugin (i.e. for the about dialog)
	 */
	@Override
	public VersionInfo getVersionInfo() {
		return VERSION_INFO;
	}

	//@formatter:off
	/**
	 * {@inheritDoc} 
	 * \attention this plugin depends on the \link org.coreasm.engine.plugins.signature.SignaturePlugin \endlink and the
	 * \link org.coreasm.engine.plugins.conditionalrule.ConditionalRulePlugin \endlink
	 * 
	 * @return set required CoreASM plugins
	 */
	//@formatter:on
	@Override
	public Set<String> getDependencyNames() {
		if (dependencyNames == null) {
			dependencyNames = new HashSet<String>();
			dependencyNames.add("SignaturePlugin");
			dependencyNames.add("ConditionalRulePlugin");
		}
		return dependencyNames;
	}

	/**
	 * {@inheritDoc} \retval empty set
	 * no lexers defined for this plugin
	 */
	@Override
	public Set<Parser<?>> getLexers() {
		return Collections.emptySet();
	}

	/**
	 * @name Recursive parsers
	 */
	//@{
	/**
	 * \brief references to parsers, which are used recursively.
	 * \attention this parser reference is used for the pointcut parser, because
	 * the definition uses recursion!
	 * */
	private final Parser.Reference<Node> refPointCutTermParser = Parser.newReference();
	private final Parser.Reference<Node> refPointCutExpressionParser = Parser.newReference();
	private final Parser.Reference<Node> refBinAndParser = Parser.newReference();
	private final Parser.Reference<Node> refBinOrParser = Parser.newReference();
	private final Parser.Reference<Node> refPointCutParser = Parser.newReference();

	//@}

	/**
	 * The Definition of the parser for aspect oriented CoreASM specifications.
	 * 
	 * It consists of the grammar rules (bottom-up)
	 * <ol>
	 * <li>binOpParser</li>
	 * <li>pointCutParser</li>
	 * <li>locatorParser</li>
	 * <li>adviceBlockParser</li> and
	 * <li>aspectBlockParser</li>
	 * </ol>
	 * 
	 * @return parsers as Map<String, GrammarRule>
	 *         <ul>
	 *         <li>String => name of the parser component
	 *         <li>GrammarRule => grammar rule used for parsing the component
	 *         </ul>
	 */
	@Override
	public Map<String, GrammarRule> getParsers() {

		// the id parsers is the return variable for this method
		if (parsers == null) {
			parsers = new HashMap<String, GrammarRule>();

			/**
			 * the following parsers are reused to define the aspect parser:
			 * <ol>
			 * <li>ruleParser</li>
			 * <li>ruleDeclaration</li>
			 * <li>signatureParser</li> and
			 * <li>idParser</li>
			 * </ol>
			 */
			Parser<Node> ruleParser = ((KernelServices) capi
					.getPlugin("Kernel").getPluginInterface()).getRuleParser();

			Parser<Node> ruleDeclaration = ((ParserPlugin) capi
					.getPlugin("Kernel")).getParser("RuleDeclaration");

			Parser<Node> ruleSignature = ((ParserPlugin) capi
					.getPlugin("Kernel")).getParser("RuleSignature");

			Parser<Node> signatureParser = ((ParserPlugin) capi.getPlugin("SignaturePlugin")).getParsers()
					.get("Signature").parser;

			Parser<Node> idParser = ParserTools.getInstance(capi).getIdParser();

			Parser<Node> stringParser = ((ParserPlugin) capi.getPlugin("StringPlugin")).getParser("StringTerm");

			//Parser<Node> resultLocation = ((ParserPlugin)capi.getPlugin("TurboASMPlugin")).getParser("ResultLocation");

			ParserTools pTools = ParserTools.getInstance(capi);

			Parser<Node> pointCutParameterParser = //(String || id) ['as' id]
			Parsers.array(
					Parsers.or(
							stringParser,
							idParser
							),
					Parsers.array(
							pTools.getKeywParser(KW_AS, PLUGIN_NAME),
							idParser
							).optional()
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									PointCutParameterNode node = new PointCutParameterNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode) {
										if (((ASTNode) child).getGrammarRule().equals("ID")) {
											FunctionRuleTermNode fn = new FunctionRuleTermNode(child.getScannerInfo());
											fn.addChild("alpha", child);
											parent.addChild("lambda", fn);
										}
										else
											parent.addChild("lambda", child);
									}
									else
										parent.addChild(child);
								}
							});

			Parser<Node> namedPointcutParser = // 'pointcut' id '(' id (',' id)*  ')' ':' binOrParser
			Parsers.array(
					pTools.getKeywParser(KW_POINTCUT, PLUGIN_NAME),
					idParser,
					Parsers.array(
							pTools.getOprParser("("),
							idParser,
							pTools.star(
									Parsers.array(
											pTools.getOprParser(","),
											idParser
											)
									),
							pTools.getOprParser(")")
							).optional(),
					pTools.getOprParser(":"),
					refBinOrParser.lazy()
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									NamedPointCutDefinitionASTNode node = new NamedPointCutDefinitionASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode) {
										parent.addChild("lambda", child);
									}
									else
										parent.addChild(child);
								}
							});

			/**
			 * Parser for call expression
			 * the keyword call,
			 * the id of the rule_name called (using regex),
			 * optional a boolean value saying if the rule has to return a value
			 */
			Parser<Node> callParser = // 'call' '(' id || string ['as' id] (',' id || string ['as' id] )* ')' ['by' id || string] [('with' || 'without')( 'result' || 'return') ]
			Parsers.array(pTools.getKeywParser(KW_RULECALL, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")"),
					Parsers.array(
							Parsers.or(
									pTools.getKeywParser("with", PLUGIN_NAME),
									pTools.getKeywParser(KW_WITHOUT, PLUGIN_NAME)
									),
							Parsers.or(
									pTools.getKeywParser("result", PLUGIN_NAME),
									pTools.getKeywParser("return", PLUGIN_NAME)
									)
							).optional()
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									CallASTNode node = new CallASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			Parser<Node> getParser = // 'get' '(' id || string ['as' id] (',' id || string ['as' id] )* ')' ['by' id || string] [('with' || 'without')( 'result' || 'return') ]
			Parsers.array(pTools.getKeywParser(KW_GET, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")")
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									GetASTNode node = new GetASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			Parser<Node> setParser = // 'set' '(' id || string ['as' id] (',' id || string ['as' id] )* ')' ['by' id || string] [('with' || 'without')( 'result' || 'return') ]
			Parsers.array(pTools.getKeywParser(KW_SET, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")")
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									SetASTNode node = new SetASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			/* Parser for within expression */
			Parser<Node> withinParser = // 'within' '(' id || string ['as' id] (',' id || string ['as' id] )* ')' ['by' id || string] [('with' || 'without')( 'result' || 'return') ]
			Parsers.array(pTools.getKeywParser(KW_WITHIN, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")"),
					Parsers.array(
							Parsers.or(
									pTools.getKeywParser("with", PLUGIN_NAME),
									pTools.getKeywParser(KW_WITHOUT, PLUGIN_NAME)
									),
							Parsers.or(
									pTools.getKeywParser("result", PLUGIN_NAME),
									pTools.getKeywParser("return", PLUGIN_NAME)
									)
							).optional()
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									WithinASTNode node = new WithinASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			/* Parser for args expression */
			Parser<Node> argsParser = //'args' '(' id || string ['as' id] (',' id || string ['as' id] )* ')'
			Parsers.array(
					pTools.getKeywParser(KW_ARGS, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")")
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									ArgsASTNode node = new ArgsASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									// ((Node) ((Object[]) from[0])[0])
									// .getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			/* Parser for cflow expression */
			Parser<Node> cFlowParser = // cflow(pointCutParser)
			Parsers.array(pTools.getKeywParser(KW_CFLOW, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")"),
					Parsers.array(
							Parsers.or(
									pTools.getKeywParser("with", PLUGIN_NAME),
									pTools.getKeywParser(KW_WITHOUT, PLUGIN_NAME)
									),
							Parsers.or(
									pTools.getKeywParser("result", PLUGIN_NAME),
									pTools.getKeywParser("return", PLUGIN_NAME)
									)
							).optional()
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									CFlowASTNode node = new CFlowASTNode(
											/*
											 * get scanner info from first
											 * element of the complex node call
											 */
											((Node) from[0]).getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			/* Parser for cflow below expression */
			Parser<Node> cFlowBelowParser = // cflowbelow(pointCutParser)
			Parsers.array(pTools.getKeywParser(KW_CFLOWBELOW, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")"),
					Parsers.array(
							Parsers.or(
									pTools.getKeywParser("with", PLUGIN_NAME),
									pTools.getKeywParser(KW_WITHOUT, PLUGIN_NAME)
									),
							Parsers.or(
									pTools.getKeywParser("result", PLUGIN_NAME),
									pTools.getKeywParser("return", PLUGIN_NAME)
									)
							).optional()
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									CFlowBelowASTNode node = new CFlowBelowASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});

			/* Parser for cflow top expression */
			Parser<Node> cFlowTopParser = // cflowtop(pointCutParser)
			Parsers.array(pTools.getKeywParser(KW_CFLOWTOP, PLUGIN_NAME),
					pTools.getOprParser("("),
					pointCutParameterParser,
					pTools.star(
							Parsers.array(
									pTools.getOprParser(","),
									pointCutParameterParser
									)
							),
					pTools.getOprParser(")"),
					Parsers.array(
							Parsers.or(
									pTools.getKeywParser("with", PLUGIN_NAME),
									pTools.getKeywParser(KW_WITHOUT, PLUGIN_NAME)
									),
							Parsers.or(
									pTools.getKeywParser("result", PLUGIN_NAME),
									pTools.getKeywParser("return", PLUGIN_NAME)
									)
							).optional()).map(
					new ParserTools.ArrayParseMap(PLUGIN_NAME) {
						@Override
						public Node map(Object[] from) {
							CFlowTopASTNode node = new CFlowTopASTNode(
									// get scanner info from first
									// element of the complex node call
									((Node) from[0]).getScannerInfo());
							addChildren(node, from);
							return node;
						}

						@Override
						public void addChild(Node parent, Node child) {
							if (child instanceof ASTNode)
								parent.addChild("lambda", child);
							else
								parent.addChild(child);
						}
					});

			/* Parser for not expression */
			Parser<Node> notParser = // not(pointCutParser)
			Parsers.array(pTools.getKeywParser(OP_NOT, PLUGIN_NAME),
					pTools.getOprParser("("), refBinOrParser.lazy(),
					pTools.getOprParser(")")).map(
					new ParserTools.ArrayParseMap(PLUGIN_NAME) {
						@Override
						public Node map(Object[] from) {
							NotASTNode node = new NotASTNode(
									// get scanner info from first
									// element of the complex node call
									((Node) from[0]).getScannerInfo());
							addChildren(node, from);
							return node;
						}

						@Override
						public void addChild(Node parent, Node child) {
							if (child instanceof ASTNode)
								parent.addChild("lambda", child);
							else
								parent.addChild(child);
						}
					});

			/* Parser for named pointcut expressions used as pointcut term */
			Parser<Node> namedPointcutExpressionParser =
					Parsers.array(
							idParser,
							Parsers.array(
									pTools.getOprParser("("),
									idParser,
									pTools.star(
											Parsers.array(
													pTools.getOprParser(","),
													idParser
													)
											),
									pTools.getOprParser(")")
									).optional())
							.map(
									new ParserTools.ArrayParseMap(PLUGIN_NAME) {
										@Override
										public Node map(Object[] from) {
											NamedPointCutASTNode node = new NamedPointCutASTNode(
													// get scanner info from first
													// element of the complex node call
													((Node) from[0]).getScannerInfo());
											addChildren(node, from);
											return node;
										}

										@Override
										public void addChild(Node parent, Node child) {
											if (child instanceof ASTNode) {
												if (((ASTNode) child).getGrammarRule().equals("ID")) {
													FunctionRuleTermNode fn = new FunctionRuleTermNode(child
															.getScannerInfo());
													fn.addChild("alpha", child);
													parent.addChild("lambda", fn);
												}
												else
													parent.addChild("lambda", child);
											}
											else
												parent.addChild(child);
										}
									});

			// pointcut for checking the executing agent at runtime
			Parser<Node> agentPointcutParser =
					Parsers.array(
							pTools.getKeywParser(KW_AGENT, PLUGIN_NAME),
							pTools.getOprParser("("),
							Parsers.or(
									idParser,
									stringParser
									),
							pTools.getOprParser(")")
							).map(
									new ParserTools.ArrayParseMap(PLUGIN_NAME) {
										@Override
										public Node map(Object[] from) {
											AgentPointCutASTNode node = new AgentPointCutASTNode(
													// get scanner info from first
													// element of the complex node call
													((Node) from[0]).getScannerInfo());
											addChildren(node, from);
											return node;
										}

										@Override
										public void addChild(Node parent, Node child) {
											if (child instanceof ASTNode)
												parent.addChild("lambda", child);
											else
												parent.addChild(child);
										}
									});

			/* pointCutExpression parser */
			Parser<Node> pointCutTermParser = Parsers.or(
					// call(id)
					callParser,
					// within(id)
					withinParser,
					// args ((id,)*id)
					argsParser,
					// get(id)
					getParser,
					// set(id)
					setParser,
					// not pointCutParser
					notParser,
					// cflow(pointCutParser)
					cFlowParser,
					// cflowbelow(pointCutParser)
					cFlowBelowParser,
					// cflowtop(pointCutParser)
					cFlowTopParser,
					// namedPointcut
					namedPointcutExpressionParser,
					// agentPointcut
					agentPointcutParser
					);
			refPointCutTermParser.set(pointCutTermParser);

			parsers.put(
					"PointCutTerm",
					new GrammarRule(
							"PointCutTerm",
							"'call(id) | within(id) | args(id*) | bin_not PointCut | cflow(PointCut) | cflowbelow(PointCut) | cflowtop(PointCut) | 'pointcut' id '(' id (',' id)*  ')' ':' BinOr' | 'agent(id | string)'",
							refPointCutTermParser.lazy(), this.getName()));

			//PointCutExpr = '(' BinOr ')' | PointCutTerm
			Parser<Node> pointCutExpressionParser = Parsers.or(
					Parsers.array(
							pTools.getOprParser("("),
							refBinOrParser.lazy(),
							pTools.getOprParser(")")
							),
					Parsers.array(
							refPointCutTermParser.lazy()
							)
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									ExpressionASTNode node = new ExpressionASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});
			refPointCutExpressionParser.set(pointCutExpressionParser);
			parsers.put(
					"PointCutExpression",
					new GrammarRule(
							"PointCutExpression",
							"'(' BinOr ')' | PointCutTerm",
							refPointCutExpressionParser.lazy(), this.getName()));

			//BinAnd =  PointCutExpr ( 'and' BinAnd )*
			Parser<Node> binAndParser = Parsers.array(
					refPointCutExpressionParser.lazy(),
					pTools.star(
							Parsers.array(
									pTools.getOprParser(OP_AND),
									refBinAndParser.lazy()
									)
							)
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									BinAndASTNode node = new BinAndASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});
			refBinAndParser.set(binAndParser);
			parsers.put(
					"BinAndParser",
					new GrammarRule(
							"BinAndParser",
							"PointCutExpr ( 'and' BinAnd )*",
							refBinAndParser.lazy(), this.getName()));

			//BinOr =  BinAnd ( 'or' BinOr )*
			Parser<Node> binOrParser = Parsers.array(
					refBinAndParser.lazy(),
					pTools.star(
							Parsers.array(
									pTools.getOprParser(OP_OR),
									refBinOrParser.lazy()
									)
							)
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
								@Override
								public Node map(Object[] from) {
									BinOrASTNode node = new BinOrASTNode(
											// get scanner info from first
											// element of the complex node call
											((Node) from[0]).getScannerInfo());
									addChildren(node, from);
									return node;
								}

								@Override
								public void addChild(Node parent, Node child) {
									if (child instanceof ASTNode)
										parent.addChild("lambda", child);
									else
										parent.addChild(child);
								}
							});
			refBinOrParser.set(binOrParser);
			parsers.put(
					"BinOrParser",
					new GrammarRule(
							"BinOrParser",
							"BinAnd ( 'or' BinOr )*",
							refBinOrParser.lazy(), this.getName()));

			//PointCut =  BinOr
			Parser<Node> pointCutParser =
					binOrParser;
			refPointCutParser.set(pointCutParser);
			parsers.put(
					"PointCut",
					new GrammarRule(
							"PointCut",
							"BinOr",
							pointCutParser, this.getName()));

			/* advice parser */
			Parser<Node> locatorParser = Parsers.or(
					pTools.getKeywParser(KW_BEFORE, PLUGIN_NAME),
					pTools.getKeywParser(KW_AFTER, PLUGIN_NAME),
					pTools.getKeywParser(KW_AROUND, PLUGIN_NAME));
			parsers.put("locator", new GrammarRule("locator",
					"'before | around | after'", locatorParser, PLUGIN_NAME));

			/*
			 * my Blockrule parser - {\attention redefines standard block rule
			 * parser}
			 * matches only symetric combinations of block start indicator and
			 * block end indicator, i.e. both should be brackets.
			 */
			Parser<Node> blockRuleParser = Parsers.or(
					Parsers.array(pTools.getKeywParser(KW_BEGIN, PLUGIN_NAME),
							pTools.plus(ruleParser),
							//							pTools.plus(Parsers.or(ruleParser, proceed)),
							pTools.getKeywParser(KW_END, PLUGIN_NAME)),
					Parsers.array(pTools.getOprParser("{"),
							pTools.plus(ruleParser),
							//							pTools.plus(Parsers.or(ruleParser, proceed)),
							pTools.getOprParser("}")))
					.map(new ParserTools.ArrayParseMap(PLUGIN_NAME) {
						@Override
						public Node map(Object[] from) {
							ASTNode node = new ASTNode(
									BlockRulePlugin.PLUGIN_NAME,
									ASTNode.RULE_CLASS, "BlockRule", null,
									((Node) from[0]).getScannerInfo());
							addChildren(node, from);
							return node;
						}

						@Override
						public void addChild(Node parent, Node child) {
							if (child instanceof ASTNode)
								parent.addChild("lambda", child);
							else
								parent.addChild(child); // super.addChild(parent,
														// child);
						}
					});
			parsers.put("Rule", new GrammarRule("Rule", "'begin' Rule+ 'end'",
					blockRuleParser, PLUGIN_NAME));

			/*
			 * advice parser
			 */
			Parser<Node> adviceBlockParser = Parsers.array(
					pTools.getKeywParser(KW_ADVICE, PLUGIN_NAME),
					ruleSignature, locatorParser, pTools.getOprParser(OP_COLON),
					pointCutParser, blockRuleParser).map(
					new ParserTools.ArrayParseMap(PLUGIN_NAME) {
						@Override
						public Node map(Object[] from) {
							if (from[1] instanceof ASTNode
									&& ((ASTNode) from[1]).getGrammarClass()
											.equals("Id")) {
								from[1] = new ParserTools.RuleSignatureParseMap();
							}
							AdviceASTNode node = new AdviceASTNode(
									((Node) from[0]).getScannerInfo());
							addChildren(node, from);
							return node;
						}

						@Override
						public void addChild(Node parent, Node child) {
							if (child instanceof ASTNode)
								parent.addChild("lambda", child);
							else
								parent.addChild(child);
						}
					});
			parsers.put(
					KW_ADVICE,
					new GrammarRule(
							KW_ADVICE,
							"'advice' id locator ':' pointcut ('begin' | '{') Rule+ ('end' | ')}'",
							adviceBlockParser, this.getName()));

			/*
			 * aspect main rule
			 */
			Parser<Node> aspectBlockParser = Parsers.array(
					pTools.getKeywParser(KW_ASPECT, this.getName()),
					idParser.optional(),
					Parsers.or(
							// Alternative one - with 'begin' and 'end'
							Parsers.array(
									pTools.getKeywParser(KW_BEGIN,
									this.getName()),
									pTools.star(
										Parsers.or(
											signatureParser,
											ruleDeclaration,
											namedPointcutParser,
											adviceBlockParser
										)
									),
									pTools.getKeywParser(KW_END, this.getName())),
							// Alternative two - with '{' and '}'
							Parsers.array(pTools.getOprParser("{"),
									pTools.star(
											Parsers.or(
												signatureParser,
												ruleDeclaration,
												namedPointcutParser,
												adviceBlockParser
											)
										),
									pTools.getOprParser("}")))).map(
					new ParserTools.ArrayParseMap(PLUGIN_NAME) {
						@Override
						public Node map(Object[] from) {
							AspectASTNode node = new AspectASTNode(
									((Node) from[0]).getScannerInfo());
							addChildren(node, from);
							return node;
						}

						@Override
						public void addChild(Node parent, Node child) {
							if (child instanceof ASTNode)
								parent.addChild("lambda", child);
							else
								parent.addChild(child);
						}
					});
			parsers.put(
					KW_ASPECT,
					new GrammarRule(
							KW_ASPECT,
							"'aspect' [id] ('begin' | '{') signature* ruleDeclaration* pointcut* advice* ('end' | '}')",
							aspectBlockParser, PLUGIN_NAME));

			parsers.put(Kernel.GR_HEADER, new GrammarRule(Kernel.GR_HEADER,
					KW_ASPECT, aspectBlockParser, PLUGIN_NAME));
		}
		return parsers;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.ParserPlugin#getParser(java.lang.String)
	 * @param nonterminal
	 *            name of the nonterminal
	 * @return returns null, because the plugin doen not provide a parser for
	 *         reuse.
	 * 
	 *         handles export requests for partial parsers of this class.
	 */
	@Override
	public Parser<Node> getParser(String nonterminal) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.ParserPlugin#getKeywords()
	 * 
	 * @return array of keywords \link keywords \endlink
	 *         Returns the list of keywords this plugin provides. The returned
	 *         value should not be null.
	 */
	@Override
	public String[] getKeywords() {
		return keywords;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.ParserPlugin#getOperators()
	 * 
	 *      \retval array of operators
	 *      Returns the list of operators this plugin provides. The returned
	 *      value should not be null.
	 */
	@Override
	public String[] getOperators() {
		return operators;
	}

	// Extension point plugin methods
	@Override
	/** {@inheritDoc}
	 *
	 * @see org.coreasm.engine.plugin.ExtensionPointPlugin#getTargetModes()
	 *
	 * @return target modes which invoke the method {@see fireOnModeTransition} to be executed.
	 *
	 *	Returns a map of engine modes to call priorities; upon transition of
	 *	the engine mode to any of these modes, the given plug-in must be
	 *	notified. Zero (0) is the lowest priority and 100 is the highest
	 *	calling priority. The engine will consider this priority when
	 *	calling plug-ins at extension point transitions. All plug-ins with
	 *	the same priority level will be called in a non-deterministic order.
	 *	Default call priority is DEFAULT_PRIORITY.
	 */
	public Map<EngineMode, Integer> getTargetModes() {
		// setting up the registration data for the
		// extension points
		HashMap<EngineMode, Integer> targetModes = new HashMap<EngineMode, Integer>();
		// all EngineModes in alphabetical order
		// targetModes.put(EngineMode.emAggregation, 10);
		// targetModes.put(EngineMode.emIdle, 10);
		// targetModes.put(EngineMode.emInitializingState, 10);
		// targetModes.put(EngineMode.emInitKernel, 10);
		// targetModes.put(EngineMode.emLoadingCatalog, 10);
		// targetModes.put(EngineMode.emLoadingCorePlugins, 10);
		// targetModes.put(EngineMode.emParsingHeader, 10);
		// targetModes.put(EngineMode.emParsingSpec, 10);
		// targetModes.put(EngineMode.emPreparingInitialState, 10);
		// targetModes.put(EngineMode.emRunningAgents, 10);
		// targetModes.put(EngineMode.emSelectingAgents, 10);
		// targetModes.put(EngineMode.emStartingStep, 10);
		// targetModes.put(EngineMode.emStepSucceeded, 10);
		// targetModes.put(EngineMode.emTerminated, 10);
		// targetModes.put(EngineMode.emTerminating, 10);
		// // the failure modes of the engine
		// targetModes.put(EngineMode.emError, 10);
		// targetModes.put(EngineMode.emStepFailed, 10);
		// targetModes.put(EngineMode.emUpdateFailed, 10);

		return targetModes;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.ExtensionPointPlugin#getSourceModes()
	 * 
	 * @return source modes which invoke the method \link fireOnModeTransition
	 *         \endlink to be executed.
	 *         Returns a map of engine modes to call priorities; upon transition
	 *         of
	 *         the engine mode from any of these modes, the given plug-in must
	 *         be
	 *         notified. Zero (0) is the lowest priority and 100 is the highest
	 *         calling priority. The engine will consider this priority when
	 *         calling plug-ins at extension point transitions. All plug-ins
	 *         with
	 *         the same priority level will be called in a non-deterministic
	 *         order.
	 *         Default call priority is DEFAULT_PRIORITY.
	 */
	@Override
	public Map<EngineMode, Integer> getSourceModes() {
		// setting up the registration data for the
		// extension points
		HashMap<EngineMode, Integer> sourceModes = new HashMap<EngineMode, Integer>();
		// all EngineModes in alphabetical order
		// sourceModes.put(EngineMode.emAggregation, 10);
		// sourceModes.put(EngineMode.emIdle, 10);
		//sourceModes.put(EngineMode.emInitializingState, 10);
		// sourceModes.put(EngineMode.emInitKernel, 10);
		// sourceModes.put(EngineMode.emLoadingCatalog, 10);
		// sourceModes.put(EngineMode.emLoadingCorePlugins, 10);
		// sourceModes.put(EngineMode.emParsingHeader, 10);
		sourceModes.put(EngineMode.emParsingSpec, 10);
		sourceModes.put(EngineMode.emPreparingInitialState, 10);
		// sourceModes.put(EngineMode.emRunningAgents, 10);
		// sourceModes.put(EngineMode.emSelectingAgents, 10);
		// sourceModes.put(EngineMode.emStartingStep, 10);
		// sourceModes.put(EngineMode.emStepSucceeded, 10);
		// sourceModes.put(EngineMode.emTerminated, 10);
		// sourceModes.put(EngineMode.emTerminating, 10);
		// // the failure modes of the engine
		// sourceModes.put(EngineMode.emError, 10);
		// sourceModes.put(EngineMode.emStepFailed, 10);
		// sourceModes.put(EngineMode.emUpdateFailed, 10);

		return sourceModes;
	}

	@Override
	/** {@inheritDoc}
	 * @see org.coreasm.engine.plugin.ExtensionPointPlugin#fireOnModeTransition(org.coreasm.engine.CoreASMEngine.EngineMode, org.coreasm.engine.CoreASMEngine.EngineMode)
	 *
	 * @param source	the source mode
	 * @param target	the target mode
	 *
	 * After parsing the specification, the tree is transformed so that the
	 * aspects are woven into the original one.
	 *
	 *
	 \dot
		digraph fireOnModeTransition {
		node [shape=box];
		initialize	[fillcolor=lightblue,style=filled,label="weaver.initialize" ];
		weaveAspectsAfterparsing [fillcolor=lightblue,style=filled,label="weave based on cloned tree" ];
		weaveAspectsForExecution [fillcolor=lightblue,style=filled,label="weave based on original tree"];
		writeProgram [fillcolor=yellow,style=filled,label="write woven program into a file"];
		node [shape=circle];
		start;
		end;
		start -> initialize;
		initialize -> weaveAspectsAfterparsing [label="source == EngineMode.emParsingSpec\n && target == EngineMode.emIdle"];
		initialize -> weaveAspectsForExecution [label="source == EngineMode.emParsingSpec\n && target != EngineMode.emIdle"];
		initialize -> weaveAspectsAfterparsing -> writeProgram;
		writeProgram -> end ;
		weaveAspectsForExecution -> end ;
		}
	\enddot
	 */
	public void fireOnModeTransition(EngineMode source, EngineMode target)
			throws EngineException {

		//Update Capi used in AspectTools
		AspectTools.setCapi(capi);

		LinkedList<Node> list = new LinkedList<Node>();
		AspectTools.createDotGraph(AspectTools.nodes2dot(capi.getParser().getRootNode()), list);

		//AspectTools.writeParseTreeToFile("parseTree of the current program", capi.getParser().getRootNode());
		// initialize weaver with current ControlAPI and start if weaving if initializing was successful
		try {
			if (source == EngineMode.emParsingSpec && target == EngineMode.emIdle)//only parsing
			{
				// clear markers
				info.clearInformation("clear now!");
				// TODO create marker for all aspects and submit them via IInformation to Observers

				//weave with cloned tree to get warnings for current CoreASM specification
				//				if (AspectWeaver.getInstance().initialize(capi,((ASTNode)capi.getParser().getRootNode()))) {
				ASTNode rootnode = (ASTNode) capi.getParser().getRootNode().cloneTree();
				if (AspectWeaver.getInstance().initialize(capi, rootnode)) {
					AspectWeaver.getInstance().weave();
				}
				AspectTools.writeProgramToFile(capi, "after weaving", rootnode, capi.getSpec().getAbsolutePath());
			}
			else if (source == EngineMode.emParsingSpec && target != EngineMode.emIdle)//running the spec
			{
				//weaving if running the engine
				if (AspectWeaver.getInstance().initialize(capi, ((ASTNode) capi.getSpec().getRootNode()))) {
					AspectWeaver.getInstance().weave();
				}
			}
		}
		catch (CoreASMError e) {
			AspectWeaver.getInstance().reset();
			capi.error(e);
		}
		catch (Exception e) {
			AspectWeaver.getInstance().reset();
			e.printStackTrace();
		}
	}

	/**
	 * create a marker and send the information to plugins that are registered
	 * at information dispatcher
	 * 
	 * @param capi
	 *            used to get the current spec
	 * @param functionRuleTermNode
	 *            the node to which the information is related
	 * @param binding
	 *            the binding information about the given functionRuleTermNode
	 */
	public static void createMarker(ControlAPI capi, ASTNode functionRuleTermNode, Binding binding) {
		Map<String, String> data = new HashMap<String, String>();
		
		Specification spec = capi.getSpec();
		CharacterPosition charPos = functionRuleTermNode.getScannerInfo().getPos(capi.getParser().getPositionMap());
		
		data.put("file", spec.getAbsolutePath());
		data.put("advice", binding.getPointcutASTNode().getAdvice().getRealName());
		data.put("advicePos", "" + binding.getPointcutASTNode().getAdvice().getScannerInfo().charPosition);
		data.put("aspect", binding.getPointcutASTNode().getAspect().getName());
		data.put("aspectPos", "" + binding.getPointcutASTNode().getAspect().getScannerInfo().charPosition);
		data.put("column", "" + charPos.column);
		data.put("length", "" + functionRuleTermNode.getFirst().getToken().length());
		data.put("name", binding.toString());	
		data.put("function", functionRuleTermNode.unparseTree());
		data.put("functionPos", "" + functionRuleTermNode.getScannerInfo().charPosition);
		data.put("line", "" + spec.getLine(charPos.line).line);
		
		ASTNode parentRule = functionRuleTermNode.getParent();
		while (parentRule != null && !Kernel.GR_RULEDECLARATION.equals(parentRule.getGrammarRule()))
			parentRule = parentRule.getParent();
		data.put("rulePos", "" + parentRule.getScannerInfo().charPosition);
		data.put("ruleName", parentRule.getFirst().getFirst().getToken());

		info.createInformation("create now!", VerbosityLevel.COMMUNICATION, data);
	}

	/**
	 * @name methods implementing Vocabulary Extender
	 */
	//@{
	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getFunctions()
	 * 
	 *      \retval map of functions if extending the vocabulary with functions,
	 *      initialize and return
	 *      corresponding function elements.
	 *      \retval null otherwise
	 */
	@Override
	public Map<String, FunctionElement> getFunctions() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getFunctions();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getUniverses()
	 * 
	 *      \retval map of universes if extending the vocabulary with universe,
	 *      initialize and return corresponding universe elements.
	 *      \retval null otherwise
	 */
	@Override
	public Map<String, UniverseElement> getUniverses() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getUniverses();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getRules()
	 * 
	 *      \retval rule elements if extending the vocabulary with rules,
	 *      initialize and return
	 *      corresponding rule elements
	 *      \retval null otherwise
	 * 
	 *      \attention This method uses the class \link ExcerptOfSignaturePlugin
	 *      \endlink
	 *      (which is a limited, changed subset of the SignaturePlugin) create
	 *      the rules declared in aspects. Moreover, all adviceRules are stored
	 *      in a hashMap to ease the weaving process.
	 */
	@Override
	public Map<String, RuleElement> getRules() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getRules();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getBackgrounds()
	 * 
	 *      \retval map of backgrounds if extending the vocabulary with
	 *      background, initialize and return corresponding background elements.
	 *      \retval null otherwise
	 */
	@Override
	public Map<String, BackgroundElement> getBackgrounds() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getBackgrounds();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getFunctionNames()
	 * 
	 *      \retval set of names of all functions that are provided by this
	 *      plugin.
	 *      \attention The returned value should NOT be null. Plug-ins should
	 *      return an empty set if they are not providing any function. Hint:
	 *      use Collections.emptySet().
	 */
	@Override
	public Set<String> getFunctionNames() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getFunctionNames();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getUniverseNames()
	 * 
	 *      \retval set of names of all universes that are provided by this
	 *      plugin.
	 *      \attention The returned value should NOT be null. Plug-ins should
	 *      return an empty set if they are not providing any universe. Hint:
	 *      use Collections.emptySet().
	 */
	@Override
	public Set<String> getUniverseNames() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getUniverseNames();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getBackgroundNames()
	 * 
	 *      \retval set of names of the backgrounds that are provided by this
	 *      plugin.
	 * 
	 *      \attention The returned value should NOT be null. Plug-ins should
	 *      return an empty set if they are not providing any background. Hint:
	 *      use Collections.emptySet().
	 */
	@Override
	public Set<String> getBackgroundNames() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getBackgroundNames();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getRuleNames()
	 * 
	 *      \retval set of names of all rule provided be this plugin.
	 *      \attention The returned value should NOT be null. Plug-ins should
	 *      return an empty set if they are not providing any background. Hint:
	 *      use Collections.emptySet().
	 */
	@Override
	public Set<String> getRuleNames() {
		ExcerptOfSignaturePlugin esp = new ExcerptOfSignaturePlugin();
		esp.setControlAPI(capi);
		return esp.getRuleNames();
	}
	//@}
}
