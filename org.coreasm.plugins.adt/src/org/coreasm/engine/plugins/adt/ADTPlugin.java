package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.engine.CoreASMError;
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
    
    private boolean processingDefinitions = false;
	
    private final String[] keywords = {"datatype", "match", "on", "This"};
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
			Kernel kernelPlugin = (Kernel)capi.getPlugin("Kernel");
			KernelServices kernel = (KernelServices)kernelPlugin.getPluginInterface();

			//Parser<Node> ruleADTParser = kernel.getRuleADTParser(); 

			ParserTools pTools = ParserTools.getInstance(capi);
			Parser<Node> idParser = pTools.getIdParser();			
			
			// Pattern : ID ( '(' pattern , (',' pattern)* ')' )?, there has to be a wildcard or a variable in the 5th recursive stage
			Parser<Node> patternParser = Parsers.or(
												pTools.getOprParser("_"),
												idParser
										);
			
			for(int i =0; i<5; i++){
			
			patternParser = Parsers.or(
					Parsers.array(
							new Parser[] {
									pTools.getOprParser("_")
							}
					),
					Parsers.array(
							new Parser[] {
									idParser,
									pTools.seq(
											pTools.getOprParser("("),
											pTools.csplus(patternParser),
											pTools.getOprParser(")")
									).optional()
							}
					)).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
									public Node map(Object[] vals) {
										Node node = new PatternNode(((Node)vals[0]).getScannerInfo());
										addChildren(node, vals);
										return node;
									}
						
							}
					);
			parsers.put("PatternDeclaration", new GrammarRule("PatternDeclaration", 
				"ID ( '(' pattern , (',' pattern)* ')' )?", patternParser, PLUGIN_NAME));
			
			}
			
			// typeconstructor : ID ( '(' ID ( ',' ID )* ')' )?
			Parser<Node> typeconstructorParser = Parsers.array(
					new Parser[] {
							idParser,
							pTools.seq(
									pTools.getOprParser("("),
									pTools.csplus(idParser),
									pTools.getOprParser(")")
							).optional()
					}).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
									public Node map(Object[] vals) {
											Node node = new TypeconstructorNode(((Node)vals[0]).getScannerInfo());
											addChildren(node, vals);
											return node;
									}
							
							}
					);
					parsers.put("Typeconstructor", new GrammarRule("Typeconstructor", 
					"ID ( '(' ID ( ',' ID )* ')' )?", typeconstructorParser, PLUGIN_NAME));
			
			// parameter : ID ( ':' ('This' | ID | typeConstructor) )?
						Parser<Node> parameterParser = Parsers.array(
								new Parser[] {
										idParser,
										pTools.seq(
												pTools.getOprParser(":"),
												Parsers.or(
														pTools.getKeywParser("This", PLUGIN_NAME),
														typeconstructorParser,
														idParser
												)
										).optional()
								}).map(
										new ParserTools.ArrayParseMap(PLUGIN_NAME) {
												public Node map(Object[] vals) {
														Node node = new ParameterNode(((Node)vals[0]).getScannerInfo());
														addChildren(node, vals);
														return node;
												}
										
										}
								);
								parsers.put("Parameter", new GrammarRule("Parameter", 
								"ID ( ':' ('This' | ID | typeConstructor) )?", parameterParser, PLUGIN_NAME));
			
			// dataconstructor : ID ( '(' Parameter (',' Parameter)* ')' )?
			Parser<Node> dataconstructorParser = Parsers.array(
					new Parser[] {
							idParser,
							pTools.seq(
									pTools.getOprParser("("),
									pTools.csplus(
											parameterParser
									),
									pTools.getOprParser(")")
							).optional()
					}).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
									public Node map(Object[] vals) {
											Node node = new DataconstructorNode(((Node)vals[0]).getScannerInfo());
											addChildren(node, vals);
											return node;
									}
							
							}
					);
					parsers.put("Dataconstructor", new GrammarRule("Dataconstructor", 
					"ID ( '(' ID ( ':' ID) ? ( ',' ID ( ':' ID )? )* ')' )?", dataconstructorParser, PLUGIN_NAME));
			
					
			// ADTDefinition : 'datatype' typeconstructor) '=' dataconstructor ( '|' dataconstructor )*
			Parser<Node> datatypeParser = Parsers.array(
					new Parser[] {
							pTools.getKeywParser("datatype", PLUGIN_NAME),
							typeconstructorParser,
							pTools.getOprParser("="),
							dataconstructorParser,
							pTools.star(
									pTools.seq(
										pTools.getOprParser("|"),
										dataconstructorParser
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
					"'datatype' ID ('(' ID (',' ID)* ')') '=' ID ('(' ID (':' ID)? (',' ID (':' ID)? )* ')') ( '|' ID ('(' ID (':' ID)? (',' ID (':' ID)?)* ')') )**", datatypeParser, PLUGIN_NAME));
			
			
			// SelektorDefinition : ID '.' ID
			Parser<Node> selektorParser = Parsers.array(
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
			);
					/*, Other Selektor-Parser,  replaced by FunctionRuleTerm
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
					)*/
					
			parsers.put("SelektorDefinition", new GrammarRule("SelektorDefinition", 
					"ID '.' ID", selektorParser, PLUGIN_NAME));

			// PatternMatchDefinition : 'match' '(' ID ')' 'on' '(' ( '|' pattern )+ '->' functionRuleTerm)+ ')'
			Parser<Node> patternMatchParser = Parsers.array(
					new Parser[] {
							pTools.getKeywParser("match", PLUGIN_NAME),
							pTools.getOprParser("("),
							idParser,
							pTools.getOprParser(")"),
							pTools.getKeywParser("on", PLUGIN_NAME),
							pTools.getOprParser("("),
							pTools.plus(
									pTools.seq(
											pTools.plus(
														pTools.seq(
																pTools.getOprParser("|"),
																patternParser
														)
											),
											pTools.getOprParser("->"),
											kernel.getFunctionRuleTermParser()
									)
							),
							pTools.getOprParser(")")
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
				"'match' '(' ID ')' 'on' '(' ( '|' ID )+ '->' ID)+ ')'", patternMatchParser, PLUGIN_NAME));


			
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
					"(DatatypeDefinition|SelektorDefinition|PatternMatchDefinition|Pattern)", adtParser, PLUGIN_NAME));
			
			parsers.put("Header", new GrammarRule("Header", "ADT", adtParser, PLUGIN_NAME));
			
		}
		return parsers;
	}
	
	
	public VersionInfo getVersionInfo() {
		return verInfo;
	}

public ASTNode interpret(Interpreter interpreter, ASTNode pos) {
		
		ASTNode nextPos = pos;
		String x = pos.getToken();
		String gClass = pos.getGrammarClass();
        
		//TODO implement
		if(pos instanceof DatatypeNode){
			DatatypeNode dtNode = (DatatypeNode) pos;
			
			//check if datatypename is unique, otherwise throw an error
			String datatypename = dtNode.getFirst().getToken();
			String typeconstructor = dtNode.getTypeconstructorName();
			
			// build a DatatypeBackgroundElement for the new algebraic datatype and its dataconstructors and a SelektorFunctionElement for each Selektor
			// if the name of the datatype, one of the dataconstructors or selektors isn't unique in the hole specification, throw an error
			
			//get all dataconstructors and their parameters
			for(DataconstructorNode dcNode : dtNode.getDataconstructorNodes()){
				
				//check it datatype name is unique - otherwise throw an error
				String dcName = dcNode.getName();
				
				for(ParameterNode pNode : dcNode.getParameterNodes()){
					
					//check Parametertype
					String type = pNode.getType();
					
					//Check if type is "This", then replace it by the Typeconstructor
					
					//check if selektor - if defined - is unique otherwise throw an error
					//add it to Functions or throw an error
					String selektor = pNode.getSelektor();
					if(selektor!=null){
						
					}
					
					
				}
				
				//build Dataconstructor
				
			}
			
			//build DatatypeBackgroundElement
			
		}
		
		else if (pos instanceof SelektorNode){
			SelektorNode sNode = (SelektorNode) pos;
			
			//call SelektorFunctionElement, if it's defined, otherwise throw an error
		}
		
		else if (pos instanceof PatternMatchNode){
			PatternMatchNode pmNode = (PatternMatchNode) pos;
			
			
			//get the Datatype-value from the AbstractStorage, which should be pattern-matched
			
			
			
			//try to bind the value to each Pattern. If it fits, call the next given function. 
			//If nothing fits, throw an error 
		}
		
		return nextPos;
	}

	private void processDefinitions(){
		//TODO implement
		
		// Don't do anything if the spec is not parsed yet
    	if (capi.getSpec().getRootNode() == null)
    		return;
    	
        processingDefinitions = true;
        if (functions == null) {        
            functions = new HashMap<String,FunctionElement>();
            //functionClass = new HashMap<String,FunctionClass>();
        }
        if (universes == null) {
            universes = new HashMap<String,UniverseElement>();            
        }
        if (backgrounds == null) {
            backgrounds = new HashMap<String,BackgroundElement>();
        }
        if (rules == null) {
            rules = new HashMap<String,RuleElement>();
        }

        
        ASTNode node = capi.getParser().getRootNode().getFirst();
        
    	Interpreter interpreter = capi.getInterpreter().getInterpreterInstance(); 
		
		while (node != null) {
            if ((node.getGrammarRule() != null) && node.getGrammarRule().equals("ADT")) {
                ASTNode currentSignature = node.getFirst();
                

            	while (currentSignature != null) {
                    //if (currentSignature instanceof EnumerationNode) {
                    //    createEnumeration(currentSignature, interpreter);
                   // }

                    
                    
                    currentSignature = currentSignature.getNext();
                }
            }
            
            node = node.getNext();            
        }        
	}

	@Override
	public Set<Parser<? extends Object>> getLexers() {
		return Collections.emptySet();
	}

	@Override
	public Map<String, BackgroundElement> getBackgrounds() {
		if(backgrounds == null){
			processDefinitions();
		}
		
		return backgrounds;
	}

	@Override
	public Map<String, FunctionElement> getFunctions() {
		if(functions == null){
			processDefinitions();
		}
		
		return functions;
	}


	@Override
	public Map<String, RuleElement> getRules() {
		if(rules == null){
			processDefinitions();
		}
		return rules;
	}


	@Override
	public Map<String, UniverseElement> getUniverses() {
		if(universes == null){
			processDefinitions();
		}
		return universes;
	}
	
	@Override
	public Set<String> getBackgroundNames() {
		return getBackgrounds().keySet();
	}
	
	@Override
	public Set<String> getFunctionNames() {
		return getFunctions().keySet();
	}
	
	@Override
	public Set<String> getRuleNames() {
		return getRules().keySet();
	}
	
	@Override
	public Set<String> getUniverseNames() {
		return getUniverses().keySet();
	}
	
	
	/* (non-Javadoc)
	 * @see org.coreasm.engine.plugins.signature.SignaturePlugin#checkNameUniqueness
	 */
    private boolean checkNameUniqueness(String name, String type, ASTNode node, Interpreter interpreter) {
    	boolean result = true;
        if (rules.containsKey(name)) {
        	throw new CoreASMError("Cannot add " + type + " '" + name + "'." + 
            		" A derived function with the same name already exists.", node);
        }
        if (functions.containsKey(name)) {
//            capi.error("Cannot add " + type + " '" + name + "'." + 
//            		" A function with the same name already exists.", node, interpreter);
//            result = false;
        	throw new CoreASMError("Cannot add " + type + " '" + name + "'." + 
            		" A function with the same name already exists.", node);
        }
        if (universes.containsKey(name)) {
        	throw new CoreASMError("Cannot add " + type + " '" + name + "'." + 
            		" A universe with the same name already exists.", node);
        }
        if (backgrounds.containsKey(name)) {
        	throw new CoreASMError("Cannot add " + type + " '" + name + "'." + 
            		" A background with the same name already exists.", node);
        }
        return result;
    }
	
}