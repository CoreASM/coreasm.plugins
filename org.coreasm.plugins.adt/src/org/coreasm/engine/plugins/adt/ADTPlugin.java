package org.coreasm.engine.plugins.adt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.engine.EngineError;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.absstorage.BackgroundElement;
import org.coreasm.engine.absstorage.FunctionElement;
import org.coreasm.engine.absstorage.RuleElement;
import org.coreasm.engine.absstorage.UniverseElement;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter;
import org.coreasm.engine.interpreter.InterpreterException;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.KernelServices;
import org.coreasm.engine.parser.GrammarRule;
import org.coreasm.engine.parser.ParseMap;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.plugin.InterpreterPlugin;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugin.Plugin;
import org.coreasm.engine.plugin.VocabularyExtender;

public class ADTPlugin extends Plugin implements ParserPlugin, VocabularyExtender, InterpreterPlugin{

	public static final VersionInfo verInfo = new VersionInfo(0, 1, 1, "alpha");
	
	public static final String PLUGIN_NAME = ADTPlugin.class.getSimpleName();
	
	private HashMap<String,FunctionElement> functions;
    private HashMap<String,UniverseElement> universes;
    private HashMap<String,BackgroundElement> backgrounds;
    private HashMap<String,RuleElement> rules;
    
    private Map<String, GrammarRule> parsers = null;
	
    private final String[] keywords = {"datatype", "match", "on"};
	private final String[] operators = {"(", ",", ")", "=", "|", ".", ":", "->", "_"};
	
	
	@Override
	public void initialize() {
	// do nothing
	}
	
	/*
     * @see org.coreasm.engine.plugin.ParserPlugin#getParser(java.lang.String)
     */
    public Parser<Node> getParser(String nonterminal) {
    	return null;
    }
    
	public String[] getKeywords() {
		return keywords;
	}

	public String[] getOperators() {
		return operators;
	}
	
	
	public Map<String, GrammarRule> getParsers() {
		if (parsers == null) {
			parsers = new HashMap<String, GrammarRule>();
			//Kernel kernelPlugin = (Kernel)capi.getPlugin("Kernel");
			//KernelServices kernel = (KernelServices)kernelPlugin.getPluginInterface();

			//Parser<Node> ruleADTParser = kernel.getRuleADTParser(); 

			ParserTools pTools = ParserTools.getInstance(capi);
			Parser<Node> idParser = pTools.getIdParser();			
			
			//TODO insert other Parser
			
			// ADTDefinition : 'datatype' ID ('(' ID (',' ID)* ')') '=' ID ('(' ID (':' ID)? (',' ID (':' ID)? )* ')') ( '\n' '|' ID ('(' ID (':' ID)? (',' ID (':' ID)?)* ')') )*
			Parser<Node> datatypeParser = Parsers.array(
					new Parser[] {
							pTools.getKeywParser("datatype", PLUGIN_NAME),
							idParser,
							pTools.seq(
								pTools.getOprParser("("),
								pTools.csplus(idParser),
								pTools.getOprParser(")")).optional(),
							pTools.getOprParser("="),
							idParser,
							pTools.seq(
								pTools.getOprParser("("),
								pTools.csplus(
										idParser,
										pTools.seq(
												pTools.getOprParser(":"),
												idParser
										).optional()
								),
								pTools.getOprParser(")")).optional(),
							pTools.csplus(
									pTools.seq(
										pTools.getOprParser("\n"),
										pTools.getOprParser("|"),
										idParser,
										pTools.getOprParser("("),
										pTools.csplus(
												idParser,
												pTools.seq(
														pTools.getOprParser(":"),
														idParser
												).optional()
										),
										pTools.getOprParser(")")
									)
							)
							
					}).map(
					new ParserTools.ArrayParseMap(PLUGIN_NAME) {
						public Node map(Object[] vals) {
							Node node = new DatatypeNode(((Node)vals[0]).getScannerInfo());
							addChildren(node, vals);
							return node;
						}
						
					});
			parsers.put("DatatypeDefinition", new GrammarRule("DatatypeDefinition", 
					"'datatype' ID ('(' ID (',' ID)* ')') '=' ID ('(' ID (':' ID)? (',' ID (':' ID)? )* ')') ( '\n' '|' ID ('(' ID (':' ID)? (',' ID (':' ID)?)* ')') )**", datatypeParser, PLUGIN_NAME));
			
			
			// SelektorDefinition : (ID '.' ID) | ( ID '(' ID ')' )
			Parser<Node> selektorParser = Parsers.or(
					Parsers.array(
							new Parser[] {
									idParser,
									pTools.getOprParser("."),
									idParser
							}).map(
									new ParserTools.ArrayParseMap(PLUGIN_NAME) {

										public Node map(Object[] vals) {
											Node node = new SelektorNode(((Node)vals[0]).getScannerInfo());
											addChildren(node, vals);
											return node;
										}
						
									}
					),
					Parsers.array(
							new Parser[] {
									idParser,
									pTools.getOprParser("("),
									idParser,
									pTools.getOprParser(")")
							}).map(
									new ParserTools.ArrayParseMap(PLUGIN_NAME) {

										public Node map(Object[] vals) {
											Node node = new SelektorNode(((Node)vals[0]).getScannerInfo());
											addChildren(node, vals);
											return node;
										}
						
									}
							)
					
			);
			parsers.put("SelektorDefinition", new GrammarRule("SelektorDefinition", 
					"(ID '.' ID) | ( ID '(' ID ')' )", selektorParser, PLUGIN_NAME));

			// PatternMatchDefinition : 'match' '(' ID ')' 'on' (( '\n' '|' ID )+ '->' ID)+)
			Parser<Node> patternMatchParser = Parsers.array(
					new Parser[] {
							pTools.getKeywParser("match", PLUGIN_NAME),
							pTools.getOprParser("("),
							idParser,
							pTools.getOprParser(")"),
							pTools.getKeywParser("on", PLUGIN_NAME),
							pTools.csplus(
									pTools.seq(
											pTools.getOprParser("\n"),
											pTools.seq(
														pTools.getOprParser("|"),
														idParser
											),
											pTools.getOprParser("->"),
											idParser
									)
							)
					}).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
									public Node map(Object[] vals) {
										Node node = new PatternMatchNode(((Node)vals[0]).getScannerInfo());
										addChildren(node, vals);
										return node;
									}
						
							}
					);
			parsers.put("PatternMatchDefinition", new GrammarRule("PatternMatchDefinition", 
				"'match' '(' ID ')' 'on' (( '\n' '|' ID )+ '->' ID)+)", patternMatchParser, PLUGIN_NAME));

			
			// ADT : (DatatypeDefinition|SelektorDefinition|PatternMatchDefinition)*
			Parser<Node> adtParser = Parsers.array(
					new Parser[] {
						Parsers.or(
								datatypeParser,
								selektorParser,
								patternMatchParser)
						}).map(
						new ParserTools.ArrayParseMap(PLUGIN_NAME) {

							public Node map(Object[] vals) {
								Node node = new ASTNode(
					        			ADTPlugin.PLUGIN_NAME,
					        			ASTNode.DECLARATION_CLASS,
					        			"ADT",
					        			null,
					        			((Node)vals[0]).getScannerInfo());
								addChildren(node, vals);
								return node;
							}});
			parsers.put("ADT", new GrammarRule("ADT", 
					"((DatatypeDefinition|SelektorDefinition|PatternMatchDefinition))*", adtParser, PLUGIN_NAME));
			
			parsers.put("Header", new GrammarRule("Header", "ADT", adtParser, PLUGIN_NAME));
			
		}
		return parsers;
	}
	
	
	public VersionInfo getVersionInfo() {
		return verInfo;
	}

	@Override
	public ASTNode interpret(Interpreter arg0, ASTNode arg1) throws InterpreterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Parser<? extends Object>> getLexers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getBackgroundNames() {
		return getBackgrounds().keySet();
	}

	@Override
	public Map<String, BackgroundElement> getBackgrounds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getFunctionNames() {
		return getFunctions().keySet();
	}

	@Override
	public Map<String, FunctionElement> getFunctions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getRuleNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, RuleElement> getRules() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getUniverseNames() {
		return getUniverses().keySet();
	}

	@Override
	public Map<String, UniverseElement> getUniverses() {
		// TODO Auto-generated method stub
		return null;
	}
	
}