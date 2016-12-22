package org.coreasm.engine.plugins.adt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.absstorage.BackgroundElement;
import org.coreasm.engine.absstorage.Element;
import org.coreasm.engine.absstorage.FunctionElement;
import org.coreasm.engine.absstorage.RuleElement;
import org.coreasm.engine.absstorage.UniverseElement;
import org.coreasm.engine.absstorage.UpdateMultiset;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Interpreter;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.kernel.KernelServices;
import org.coreasm.engine.parser.GrammarRule;
import org.coreasm.engine.parser.OperatorRule;
import org.coreasm.engine.parser.OperatorRule.OpType;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.plugin.InterpreterPlugin;
import org.coreasm.engine.plugin.OperatorProvider;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugin.Plugin;
import org.coreasm.engine.plugin.VocabularyExtender;

public class ADTPlugin extends Plugin implements ParserPlugin, VocabularyExtender, InterpreterPlugin, OperatorProvider {

	public static final VersionInfo verInfo = new VersionInfo(0, 1, 1, "alpha");
	
	public static final String PLUGIN_NAME = ADTPlugin.class.getSimpleName();
	
	private HashMap<String,FunctionElement> functions;
    private HashMap<String,UniverseElement> universes;
    private HashMap<String,BackgroundElement> backgrounds;
    private HashMap<String,RuleElement> rules;
    
    private Map<String, GrammarRule> parsers = null;
    
    @SuppressWarnings("unused")
	private boolean processingDefinitions = false;
    private final String SELEKTOR_OP = ".";
	
    private final String[] keywords = {"datatype", "match", "on"};
	private final String[] operators = {"(", ",", ")", "=", "|", SELEKTOR_OP, ":", "->"};
	
	
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
			Parser<Node> termParser  = kernel.getTermParser();
			
			// Pattern : ID ( '(' Pattern (',' Pattern)* ')' )? | '_', there has to be a wildcard or a variable in the 5th recursive stage
			//The wildcard is treated as id, because the operator isn't accessible
			Parser<Node> patternParser = idParser;
			
			for(int i =0; i<64; i++){
			
			patternParser = Parsers.array(
					new Parser[] {
							idParser,
							pTools.seq(
									pTools.getOprParser("("),
									pTools.csplus(patternParser),
									pTools.getOprParser(")")
							).optional()
					}
					).map(
							new ParserTools.ArrayParseMap(PLUGIN_NAME) {
									public Node map(Object[] vals) {
										Node node = new PatternNode(((Node)vals[0]).getScannerInfo());
										addChildren(node, vals);
										return node;
									}
						
							}
					);
			parsers.put("PatternDeclaration", new GrammarRule("PatternDeclaration", 
				"ID ( '(' pattern , (',' pattern)* ')' )? | '_'", patternParser, PLUGIN_NAME));
			
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
			
			// parameter : (ID | typeconstructor) ( ':' (ID | typeconstructor) )?
			// the ID-Parser is included in the typeconstrutorParser
			Parser<Node> parameterParser = Parsers.array(
								new Parser[] {
										typeconstructorParser,
										pTools.seq(
												pTools.getOprParser(":"),
												typeconstructorParser
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
								"(ID | typeconstructor) ( ':' ( ID | typeconstructor) )? ", parameterParser, PLUGIN_NAME));
			
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
					"ID ( '(' Parameter (',' Parameter)* ')' )?", dataconstructorParser, PLUGIN_NAME));
			
					
			// Datatype : 'datatype' typeconstructor '=' dataconstructor ( '|' dataconstructor )*
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
			
			/* Replaced by Operator
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
				, Other Selektor-Parser,  replaced by FunctionRuleTerm
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
					
			parsers.put("SelektorDefinition", new GrammarRule("SelektorDefinition", 
					"ID '.' ID", selektorParser, PLUGIN_NAME));
			*/

			// PatternMatchDefinition : 'match' '(' ID ')' 'on' '(' ( ( '|' Pattern )+ '->' Term)+ ')'
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
											termParser
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
			//Define this parser as Termparser
			parsers.put("BasicTerm", new GrammarRule("PatternMatchDefinition", 
				"'match' '(' ID ')' 'on' '(' ( '|' ID )+ '->' ID)+ ')'", patternMatchParser, PLUGIN_NAME));


			
			// ADT : (DatatypeDefinition|SelektorDefinition|PatternMatchDefinition)*
			//The SelektorParser is replaced by SelektorFunction and SelektorOperatorRule
			Parser<Node> adtParser = Parsers.array(
					new Parser[] {
						Parsers.or(
								datatypeParser,
								//selektorParser,
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

	@SuppressWarnings("unused")
	public ASTNode interpret(Interpreter interpreter, ASTNode pos) {
		
		ASTNode nextPos = pos;
		String x = pos.getToken();
		String gClass = pos.getGrammarClass();
		
		
		if (pos instanceof PatternMatchNode){
			PatternMatchNode pmNode = (PatternMatchNode) pos;
			
			/*if(!pmNode.getValueNode().isEvaluated()){
				return pmNode.getValueNode();
			
			}*/
			
			//get the InterpreterInstance to read and change the environment
			Interpreter iInstance = capi.getInterpreter().getInterpreterInstance();
			
			//get the Datatype-value, which should be pattern-matched
			String valueName = pmNode.getVariableName(); 

			DatatypeElement value = (DatatypeElement) iInstance.getEnv(valueName); 
			
			//try to bind the value to each Pattern. If it fits, call the next given function. 
			//If nothing fits, throw an error 
			boolean matchFound = false;
			HashMap<String, Element> bindings = new HashMap<String, Element>();
			
			//The resultNode is the TermParserNode of the corresponding Pattern
			ASTNode resultNode = null;
			
			
			for (PatternNode pNode : pmNode.getPatternNodes()){
				
				//create DatatypeElement for the pattern
				DatatypeElement pattern = (DatatypeElement) createDatatypeElement(pNode);
				
				if(matchPattern(value, pattern, bindings)){
					matchFound = true;
					resultNode = pmNode.getResult(pNode);
					break;
				}else{
					bindings.clear();
				}
				
			}
			
			//
			if(matchFound){
				//check if the result is already interpreted, otherwise interpret it and then return to this node
				if(!resultNode.isEvaluated())
					return resultNode;
				
				
				// add all bindings to the environment, existing variables will be shadowed not replaced
				for(Entry<String, Element> entry : bindings.entrySet()){
					iInstance.addEnv(entry.getKey(), entry.getValue());
				}
				
				// "return" the value of the patternMatch-Term to the higher expression
				pos.setNode(null, new UpdateMultiset(), resultNode.getValue());
				
				// remove all bindings in the environment, shadowed variables will be restored
				// the environment will be reset how it was before the patternMatching
				for(String entry : bindings.keySet()){
					iInstance.removeEnv(entry);
				}
			
			}else{
				throw new CoreASMError("Cannot match the value " + pmNode.getVariableName() + " to a pattern." + 
            		"Try to use a Default-Pattern with a wildcard.", pmNode);
			}
		}
		
		return nextPos;
	}


	private Element createDatatypeElement(PatternNode pNode){
		
		//check if it's a wildcard
		if(pNode.isWildcard()){
			return DatatypeElement.wildcard();
		}else if(functions.get(pNode.getName()) == null  && (!pNode.hasSubPatterns())){
			return DatatypeElement.variable(pNode.getName());
		//if it is a Dataconstructor, there is a functionElement in the function-HashMap
		}else if(functions.containsKey(pNode.getName())){
			//get the dataconstructorName
			String dcName = pNode.getName();
			
			//the datatype-Name is taken from the corresponding FunctionElement
			String dtName = ((DataconstructorFunction)functions.get(dcName)).DATATYPE_NAME;
			
			//put all parameter into an ArrayList, look out for further dataconstructors
			ArrayList<Element> parameter = new ArrayList<Element>();
			
			for(PatternNode childNode : pNode.getSubPattern()){
				if (childNode instanceof PatternNode){
					parameter.add(createDatatypeElement(childNode));
				}
			}
			
			return new DatatypeElement(dtName, dcName, parameter);
		}
		
		return Element.UNDEF;
		
	}

	private void processDefinitions(){
		
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
            
            // Add the wildcard as a constant Datatype-background
            backgrounds.put("_", DataconstructorBackgroundElement.wildcard());
        }
        if (rules == null) {
            rules = new HashMap<String,RuleElement>();
        }

        
        ASTNode node = capi.getParser().getRootNode().getFirst();
        
    	Interpreter interpreter = capi.getInterpreter().getInterpreterInstance(); 
		
		while (node != null) {
            if ((node.getGrammarRule() != null) && node.getGrammarRule().equals("ADT")) {
                ASTNode currentSignature = node.getFirst();
                
                // create BackgroundElements and FunctionsElements for each datatype
            	while (currentSignature != null) {
                    if (currentSignature instanceof DatatypeNode) {
                       createDatatypeBackground((DatatypeNode)currentSignature, interpreter);
                    }            
                    
                    currentSignature = currentSignature.getNext();
                }
            }
            
            node = node.getNext();            
        }        
	}

	private void createDatatypeBackground(DatatypeNode dtNode, Interpreter interpreter) {
		
		//check, if datatypename is unique, is done later, to ensure neither a dataconstructor nor a selector has got the same name
		String datatypename = dtNode.getDatatypeName();
		String typeconstructor = dtNode.getTypeconstructorName();
		ArrayList<DataconstructorBackgroundElement> dataConstructors = new ArrayList<DataconstructorBackgroundElement>();
		
		
		// build a DatatypeBackgroundElement for the new algebraic datatype and its dataconstructors and a SelektorFunctionElement for each Selektor
		// check if the name of  one of the dataconstructors or selektors isn't unique in the hole specification
		
		//get all dataconstructors and their parameters
		for(DataconstructorNode dcNode : dtNode.getDataconstructorNodes()){
			
			//checkNameUniqueness is done later, if there is a selector-function with this name
			String dcName = dcNode.getName();
			
			//List for each parameter
			ArrayList<String> parameters = new ArrayList<String>();
			
			int place = 0; //place of each parameter
			
			for(ParameterNode pNode : dcNode.getParameterNodes()){		
				
				//check Parametertype
				String type = pNode.getType();
				
				//Check if type is "This", then replace it by the Typeconstructor
				if("This".equals(type))
					type = typeconstructor;
				
				parameters.add(type);
				
				//check if selektor - if defined - is unique
				//add it to functions
				String selektor = pNode.getSelektor();
				
				if(selektor!=null){
					if(checkNameUniqueness(selektor, "function", dtNode, interpreter))
						functions.put(selektor, new SelektorFunction(datatypename, dcName, place));	
				}
				
				place++;
				
			}
			
			//build DataconstructorBackgrondElement for the datatype
			dataConstructors.add(new DataconstructorBackgroundElement(dcName, datatypename, parameters));
			
			//build DataconstructorFunctions for the functions
			checkNameUniqueness(dcName, "backgrond", dtNode, interpreter);
			DataconstructorFunction dcFunction = new DataconstructorFunction(datatypename, dcName, parameters);
			functions.put(dcName, dcFunction);
			
		}
		
		
		//build DatatypeBackgroundElement and put it to the backgrounds
		checkNameUniqueness(datatypename, "backgrond", dtNode, interpreter);
		DatatypeBackgroundElement dbElement = new DatatypeBackgroundElement(datatypename, dataConstructors);
		backgrounds.put(datatypename, dbElement);
		
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
    
    /*
	 * checks the validity of the arguments.
	 */
	protected boolean checkArguments(int count, List<? extends Element> args) {

		if (args.size() != count)
			return false;
		else 
			for (int i=0; i < count; i++)
				if ( ! (args.get(i) instanceof Element)) {
					return false;
				}
		
		return true;
	}
	
	private boolean matchPattern(DatatypeElement value, DatatypeElement pattern, HashMap<String, Element> bindings){
		
		//Wildcard always match the pattern
		if(pattern.isWildcard()){
			return true;
		}
		
		//Variable alwas match the pattern and creates a new binding
		if(pattern.isVariable()){
			bindings.put(pattern.getVariableName(), value);
			return true;
		}
		
		//typecheck, it is of the same type and dataconstructor
		if((!value.getDatatype().equals(pattern.getDatatype())) || (!value.getDataconstructor().equals(pattern.getDataconstructor()))){
			return false;
		}
		
		
		//check, if all parameters match recursive
		for(int i = 0; i < pattern.getParameter().size(); i++){
			
			Element valueParameter = value.getParameter(i);
			Element patternParameter = pattern.getParameter(i);
			
			//check if it is a DatatypeElement and call the matching-Algorithm recursively. If it fails, this matching fails
			if(patternParameter instanceof DatatypeElement && valueParameter instanceof DatatypeElement){
				matchPattern((DatatypeElement) valueParameter, (DatatypeElement) patternParameter, bindings);
			
			//If Variable isn't a Datatype, check Pattern for Variable or Wildcard
			//If Pattern is a DatatypeElement (not Wildcard or Variable) and the value not, return false
			}else if(patternParameter instanceof DatatypeElement){
				DatatypeElement patternElement = (DatatypeElement) patternParameter;
				
				if(patternElement.isWildcard()){
				}else if(patternElement.isVariable()){
					bindings.put(patternElement.getVariableName(), valueParameter);
				}else{
					return false;
				}
				
			}else{
			//else check if they're the same
				if(! patternParameter.equals(valueParameter))
					return false;
				
			}
		}
		
		return true;
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
	

	@Override
	public Collection<OperatorRule> getOperatorRules() {

		ArrayList<OperatorRule> opRules = new ArrayList<OperatorRule>();
		
		opRules.add(new OperatorRule(SELEKTOR_OP , OpType.INFIX_LEFT, 800, PLUGIN_NAME));
		
		return opRules;
	}
	
	public Element interpretOperatorNode(Interpreter interpreter, ASTNode opNode){
		
		String x = opNode.getToken();
		String gClass = opNode.getGrammarClass();

		// if class of operator is binary
		if (gClass.equals(ASTNode.BINARY_OPERATOR_CLASS)) {

			// get operand nodes
			ASTNode alpha = opNode.getFirst();
			ASTNode beta = alpha.getNext();
			
			// get operand values, first is an Element, the second the selector-String
			Element l = alpha.getValue();
			String r = beta.getFirst().getToken();
			
			//check if the first element is a datatypeElement, otherwise it's not correct
			if(l instanceof DatatypeElement){
				
				//check if it's the selector-Operator
				if(x.equals(SELEKTOR_OP)){
					
					//get the selector-Function and put the Element into it as return-value
					SelektorFunction selector  = (SelektorFunction) functions.get(r);
				
					ArrayList<Element> arg = new ArrayList<Element>();
					arg.add(l);
				
					return selector.getValue(arg);
				}
				
			}
		}
		
		return Element.UNDEF;
	}
	
}