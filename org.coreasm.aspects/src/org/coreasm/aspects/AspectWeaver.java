/**
 * AoASM CoreASM Plugin
 * 
 * The weaver integrates the aspects in terms of new nodes into the AST of the
 * current CoreASM program. First, the weaver has to be initialized with the
 * current ControlAPI capi and afterwards the weaving can be started.
 * 
 * @author Marcel Dausend
 */
package org.coreasm.aspects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.codehaus.jparsec.Parser;
import org.coreasm.aspects.errorhandling.AspectException;
import org.coreasm.aspects.errorhandling.BindingException;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.aspects.pointcutmatching.Binding;
import org.coreasm.aspects.pointcutmatching.NamedPointCutASTNode;
import org.coreasm.aspects.pointcutmatching.NamedPointCutDefinitionASTNode;
import org.coreasm.aspects.pointcutmatching.PointCutParameterNode;
import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.absstorage.RuleElement;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugins.letrule.LetRuleNode;
import org.coreasm.engine.plugins.turboasm.LocalRuleNode;
import org.coreasm.engine.plugins.turboasm.ReturnRuleNode;
import org.coreasm.engine.plugins.turboasm.SeqBlockRuleNode;

public class AspectWeaver {

	/**singletonWeaver hold the singleton instance of this class */
	private static AspectWeaver singletonWeaver = null;

	/** stores the current value of the ControlAPI capi and ASTNode rootnode which is changed during initialization()
	 * @see {initialize(ControlAPI capi, ASTNode rootnode)} */
	///@{
	private ControlAPI capi;
	private ASTNode rootnode;
	///@}

	/** collections which are used to ease access to astnodes (sorted by grammar and advice rules) */
	///@{
	private static HashMap<String, LinkedList<ASTNode>> astNodes = new HashMap<String, LinkedList<ASTNode>>();
	private HashMap<String, RuleElement> adviceRules = new HashMap<String, RuleElement>();
	///@}

	/** status of the initialization of the current instance of the weaver, initialized with false*/
	private boolean initialize;

	/** name for rules which are introduced by orchestration */
	///@{
	public static final String MATCHING_RULE_INSIDE_CALLSTACK ="matchingRuleCallsInsideCallstack";
	public static final String MATCHING_SIGNATURE_INSIDE_CALLSTACK = "matchingParameterSignatureInsideCallstack";
	///@}

	/** private constructor used by singleton pattern*/
	private AspectWeaver() {
		this.initialize = false;
	}

	/**
	 * implements singleton pattern for AspectWeaver
	 * @return singleton instance of class AspectWeaver
	 */
	public static AspectWeaver getInstance(){
		if ( singletonWeaver == null )
			singletonWeaver = new AspectWeaver();
		return singletonWeaver;
	}

	/**
	 * returns a hashmap of astnodes of the current specification
	 * @return hashmap of astnodes @see astNodes
	 */
	public HashMap<String, LinkedList<ASTNode>> getAstNodes() {
		return astNodes;
	}

	/**
	 * The weaver is initialized by setting the current ControlAPI capi,
	 * collecting all relevant ASTNodes from the AST, and setting initialize to
	 * true if their are any relevant MacroCallRules which have aspects and the
	 * specification has no problems in respect to weaving.
	 *
	 * @param capi	current control api used for parsing and/or execution
	 * @param rootnode	current specification is given by the current rootnode and its children
	 * @return if the specification is ready for weaving true is returned otherwise false
	 */
	public boolean initialize(ControlAPI capi, ASTNode rootnode) {

		if (!initialize && capi != null && rootnode != null) {
			//set capi and rootnode
			this.capi = capi;
			this.setRootnode(rootnode);

			// collect all ASTNodes from aspects from the capi
			astNodes = AspectTools.collectASTNodesByGrammar(rootnode);

			//continue initialization
			if (astNodes.get("MacroCallRule") != null
					&& astNodes.get(AdviceASTNode.NODE_TYPE) != null) {
				this.initialize = true;
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * statically check the specification for errors
	 * @throws AspectException
	 */
	private void PerformStaticChecks() throws AspectException{
//		//searching for errors of proceed nodes
//		if ( astNodes.get(AdviceASTNode.NODE_TYPE) != null ) // \todo modularity plugin nicht zum reinen Parsen verfügbar
//		for (ASTNode node : astNodes.get(AdviceASTNode.NODE_TYPE))
//			if(node instanceof AdviceASTNode)
//			{
//				LinkedList<ProceedASTNode> pn = (LinkedList<ProceedASTNode>) AspectTools.getChildrenOfType(node, ProceedASTNode.class);
//				//more than one proceed
//				if (pn.size() > 1 )
//					capi.error(new CoreASMError("more than one proceed node in advice node "+AspectTools.constructName(node), null, null, null, node));
//				//proceed without around
//				if (!pn.isEmpty() && node.getChildNodes().get(2).getToken().equals("around"))
//					capi.error(new CoreASMError("proceed can only be used if the advice "+AspectTools.constructName(node)+" has the locator \"around\"", null, null, null, node));
//			}
	}

	//@formatter:off
	/**
	 * Transform the AST after the initialization process has been completed.
	 * Afterwards, the reset method is called to prepare the next weaving
	 * process.
	 *
	 \dot
		digraph weave {
		colorscheme = pastel19;
		node [colorscheme = pastel19, fillcolor=2,style=filled, shape=box];
		binding;
		before;
		around;
		after;
		pointcutMatching;
		orchestrate;
		reset;

		subgraph cluster_0 {
			style=filled;
			color=7;
			node [style=filled,color=white];
			binding -> before [label = "1."];
			before -> binding;
			binding -> around [label = "2."];
			around -> binding;
			binding -> after [label = "3."];
			label = "binding and insertion of advice block";
		}
		node [colorscheme = pastel19, fillcolor=1,style=filled, shape=box];
		MatchingError

		node [shape=oval, style=default, color=default]
	 	start -> pointcutMatching [label="initialize == true"];
	 	pointcutMatching -> MatchingError [label="pointcut matching caused error"];
	 	pointcutMatching -> orchestrate;
	 	orchestrate -> binding [label="for each weaving candidate"];
	 	after -> reset;
	 	start -> reset [label="initialize == false"];
	 	reset -> end;
	 	}
	 \enddot
	 * @throws Throwable 
	 */
	//@formatter:on

	/**
	 * 
	 * @throws AspectException
	 * @throws Throwable
	 */
	public void weave() throws AspectException {

		//HashMap with ASTNodes of the original program as key and a list of all advices having pointcuts matching this node
		HashMap<ASTNode, LinkedList<AdviceASTNode>> weavingCandidates = new HashMap<ASTNode, LinkedList<AdviceASTNode>>();

		/**
		 * try to match the pointcuts
		 */
		if (this.initialize) {
			
			//check the specification statically for errors
			PerformStaticChecks();
			
			///@{
			/** Preprocessing **/
			//substitute all named pointcuts by referenced pointcut expressions (transitively)
			LinkedList<ASTNode> nPtcList = AspectWeaver.getInstance().getAstNodes().get(NamedPointCutDefinitionASTNode.NODE_TYPE);
			if (nPtcList != null)
				for (ASTNode nptc : nPtcList) {
					if (nptc instanceof NamedPointCutDefinitionASTNode) {
						/*
						 * the set substituted keeps track of the named
						 * pointcuts that have already
						 * taken into account for during the substitution.
						 * If a named pointcut would be taken into account
						 * twice,
						 * the definition of named pointcuts is cyclic!
						 */
						HashSet<String> substituted = new HashSet<String>();
						NamedPointCutDefinitionASTNode nptcdef = (NamedPointCutDefinitionASTNode) nptc;
						substituted.add(nptcdef.getName());
						//perfomr the substutution recursively
						substituteNamedPointcuts(nptcdef.getPointCut(), substituted);
					}
				}
			
			//substitute the named pointcuts used inside an advices
			//there can be no cyclic definitions if the substitution of the named pointcuts (above) was successful
			LinkedList<ASTNode> adviceAstNode = AspectWeaver.getInstance().getAstNodes().get(AdviceASTNode.NODE_TYPE);
			if (adviceAstNode != null)
				for (ASTNode advDef : adviceAstNode) {
					if (advDef instanceof AdviceASTNode) {
						substituteNamedPointcuts(((AdviceASTNode) advDef).getPointCut(), new HashSet<String>());
					}
				}
			///@}
			
			/** pointcut matching **/
			try {
				weavingCandidates = pointCutMatching(getAstNodes());
			}catch (MatchingError e){
				e.generateCapiError(capi);
			}
			catch (Exception e) {
				capi.error(e);
			}

			//insert orchestration into AST
			/** \bug functions used as call stack parameter will cause an error!!! */
			orchestrate();

			// Weaving candidate by candidate
			for (ASTNode candidate : weavingCandidates.keySet()) {

				// collections of advice MacroCallRules by advice type
				LinkedList<ASTNode> beforeNodes = new LinkedList<ASTNode>();
				LinkedList<ASTNode> aroundNodes = new LinkedList<ASTNode>();
				LinkedList<ASTNode> afterNodes = new LinkedList<ASTNode>();

				//each advice which had been matched to the current candidate
				//requires weaving
				for (AdviceASTNode advice : weavingCandidates.get(candidate)) {

					//if there exist matching arg expressions in the pointcut of the advice...
					/**\note LinkedList<ArgsASTNode> argsList holds the list of valid bindings for the given advice and candidate*/
					Binding binding = advice.getBinding(candidate);
					AdviceASTNode boundAdvice;
					/** \note Binding binding holds the first binding from the valid bindings, and thereby determines the precedence of bindings in pointcuts from left to right */
					//...clone the current node and insert the binding depending for the current candidate
					boundAdvice = advice.cloneWithBinding(binding);

					// create adviceMacroCallRules and add each to its
					// collection
					if (boundAdvice.getLocator().equals("before")) {
						beforeNodes.add(AspectTools.create(
							AspectTools.MACROCALLRULE, boundAdvice));
						//break;
					}else if (boundAdvice.getLocator().equals("after")) {
							afterNodes.add(AspectTools.create(
									AspectTools.MACROCALLRULE, boundAdvice));
							//break;
					}else if (boundAdvice.getLocator().equals("around")) {
							aroundNodes.add(AspectTools.create(
									AspectTools.MACROCALLRULE, boundAdvice));
							//break;
					}else{
							//break;
					}
				}

				// insert nodes into a new seqblock
				// which will replace the current MacroCallRule
				SeqBlockRuleNode rootNodeOfSeqBlockSequence = AspectTools
						.create(AspectTools.SEQBLOCKRULE,
								candidate.getScannerInfo());// new
															// SeqBlockRuleNode(candidate.getScannerInfo());
				SeqBlockRuleNode seqBlockNode = rootNodeOfSeqBlockSequence;

				// get the parent of candidate and remove the candidate from its
				// parent but store its insertion reference for the replacement
				// action
				// TODO complete this list according pointcut constructs
				ASTNode insertionContext = candidate;
				while (insertionContext != null
						&& !(insertionContext.getParent() instanceof SeqBlockRuleNode)
						&& !(insertionContext.getParent() instanceof LetRuleNode)
						&& !(insertionContext.getParent() instanceof ReturnRuleNode)
						&& !(insertionContext.getParent() instanceof LocalRuleNode)
						&& !("BlockRule".equals(insertionContext.getParent().getGrammarRule())))
					insertionContext = insertionContext.getParent();
				
				ASTNode parentOfInsertionContext = insertionContext.getParent();
				Node insertionReference = insertionContext.removeFromTree();

				// change macroCallRule if there is exactly one around advice,
				// else insert nodes into seqblockrule
				if (aroundNodes.size() == 1 && beforeNodes.isEmpty()
						&& afterNodes.isEmpty()) {
					if (insertionReference != null)
						AspectTools.addChildAfter(parentOfInsertionContext, insertionReference,
								aroundNodes.getFirst().getToken(),
								aroundNodes.getFirst());
					else
						AspectTools.addChild(parentOfInsertionContext, aroundNodes.getFirst());
				} else {
					// add before nodes
					if (!beforeNodes.isEmpty()) {
						ASTNode beforeRuleBlock = AspectTools.create(
								AspectTools.BLOCKRULE, beforeNodes);
						seqBlockNode = AspectTools.insert(seqBlockNode,
								beforeRuleBlock);
					}
					// add around nodes if existing or insert candidate again
					if (aroundNodes.isEmpty()) {
						seqBlockNode = AspectTools.insert(seqBlockNode,
								candidate);
					} else {
						ASTNode aroundRuleBlock = AspectTools.create(
								AspectTools.BLOCKRULE, aroundNodes);
						seqBlockNode = AspectTools.insert(seqBlockNode,
								aroundRuleBlock);
					}
					// add after nodes
					if (!afterNodes.isEmpty()) {
						ASTNode afterRuleBlock = AspectTools.create(
								AspectTools.BLOCKRULE, afterNodes);
						seqBlockNode = AspectTools.insert(seqBlockNode,
								afterRuleBlock);
					}

					// insert new seqblock at position of the original
					// macroCallRule
					AspectTools.close(seqBlockNode, null);

					// insert new seqblock at original position of macrocallrule
					// candidate
					// the position is indicated by its previous sibling node or
					// null, if it is the first child.
					if (insertionReference != null)
						AspectTools.addChildAfter(parentOfInsertionContext, insertionReference,
								rootNodeOfSeqBlockSequence.getToken(),
								rootNodeOfSeqBlockSequence);
					else
						AspectTools.addChild(parentOfInsertionContext, rootNodeOfSeqBlockSequence);
				}
			}
			/** \todo remove non matching advices and shift all definitions to the main context of the program @see orchestrate(ASTNode node) */
		}
		// prepare next weaving (new initialization required)
		reset();
	}

	private static void substituteNamedPointcuts(ASTNode astnode, HashSet<String> substituted) throws BindingException {
		if (astnode instanceof NamedPointCutASTNode) {
			NamedPointCutASTNode nptc = ((NamedPointCutASTNode) astnode);
			if (substituted.contains(nptc.getName()))
				throw new CoreASMError("cycle in pointcut defintion!", astnode);
			//exchange
			LinkedList<ASTNode> nptcdefs = AspectWeaver.getInstance().getAstNodes().get(NamedPointCutDefinitionASTNode.NODE_TYPE);
			for(ASTNode nptcdef : nptcdefs){
				if (nptcdef instanceof NamedPointCutDefinitionASTNode) {
					NamedPointCutDefinitionASTNode definition = (NamedPointCutDefinitionASTNode) nptcdef;
					if ( definition.isDefinitionOf(nptc) ){
						ASTNode parent = nptc.getParent();
						Node positionToInsert = nptc.removeFromTree();
						Node pointcut = cloneWithBinding(definition, nptc);
						//pointcut.setParent(parent);//TODO add surrounding round brackets
						AspectTools.addChildAfter(parent, positionToInsert, definition.getName(), pointcut);
						substituted.add(definition.getName());
						substituteNamedPointcuts(parent, substituted);
					}
				}
			}
		} else
			for(ASTNode child : astnode.getAbstractChildNodes())
				substituteNamedPointcuts(child, substituted);
	}

	/**
	 * replace variables in pointcut expressions which will replace a namedpointcut with the corresponding ids used in this namedpointcut
	 *
	 * @param def 	definition form which the pointcut that is used to replace the namedpointcut is extracted
	 * @param nptc	named pointcut that has to be replaced
	 * @return	pointcut from def with exchanged variables according to the here constructed binding
	 * @throws Exception
	 */
	private static Node cloneWithBinding(NamedPointCutDefinitionASTNode def, NamedPointCutASTNode nptc)
			throws BindingException {
		//binding between variables from def as key and nptc variables as value
		HashMap<String, FunctionRuleTermNode> binding = new HashMap<String, FunctionRuleTermNode>();
		for(int i = 0; i < def.getPointCutParameters().size(); i++){
			FunctionRuleTermNode value = binding.put(def.getPointCutParameters().get(i).getToken(), nptc.getPointCutParameters().get(i));
			if (value != null && ! value.getName().equals(nptc.getPointCutParameters().get(i).getToken()))
				throw new BindingException("Binding inconsistent!", nptc.getPointCutParameters().get(i),
						value == null ? new NullPointerException("value is null!") : new Exception(
								"Binding inconsistent!"));
		}
		ASTNode pointcut = (ASTNode)def.getPointCut().cloneTree();
		//replacement of the variables within pointcut according to the binding
		cloneWithBinding(pointcut, binding);
		return pointcut;
	}

	/**
	 * replace id nodes in pointcut parameternodes with ASTNodes from the given binding if their name is a key of that binding
	 * @param node
	 * @param binding
	 */
	private static void cloneWithBinding(ASTNode node, HashMap<String, FunctionRuleTermNode> binding){
		if (node instanceof PointCutParameterNode) {
			PointCutParameterNode param = (PointCutParameterNode)node;
			if (param.getFuntionRuleTermNode() != null && binding.containsKey(param.getName())){
				String paramName =  param.getName();
				FunctionRuleTermNode fnNode = param.getFuntionRuleTermNode();
				Node insertionReference = fnNode.removeFromTree();
				AspectTools.addChildAfter(param, insertionReference, "beta",binding.get(paramName));
			}
		}
		for(ASTNode child : node.getAbstractChildNodes())
			cloneWithBinding(child, binding);
	}

	/**
	 * inserts recursively a runtime condition to every advice rule inside its rule block
	 * @param node
	 */
	private void orchestrate(ASTNode node){

		/**
		 * modify advice nodes:
		 * a) add runtime condition of pointcuts to advice body
		 * b) convert advice into rule definition
		 *
		 \dot
		 	digraph orchestrate {
		 		colorscheme = pastel19;
				start;
				end;
				node [colorscheme = pastel19, fillcolor=2,style=filled, shape=box];
				task1 [label="insert runtime condition from ptc"];
				task2 [label="convert advice into rule defintion"];
				node [colorscheme = pastel19, fillcolor=5,style=filled, shape=box];

				start -> task1 -> task2 -> end;
		 	}
		 \enddot
		 */
		if (node instanceof AdviceASTNode) {
			AdviceASTNode advice = (AdviceASTNode)node;

			//a) add runtime condition of pointcuts to advice body
			// 1) create condition ASTNode with runtime expressions from
			// pointcuts
			ASTNode conditionASTNode = null;
			String condition = advice.getPointCut().generateExpressionString();
			String ifThenConstruct = "if (" + condition + ") then skip";

			// get condition parser to parse the condition
			Parser<Node> conditionParser = ((ParserPlugin) capi
					.getPlugin("ConditionalRulePlugin")).getParsers().get(
					"Rule").parser;// using
			// FunctionSignature
			ParserTools parserTools = ParserTools.getInstance(capi);
			Parser<Node> parser = conditionParser.from(
					parserTools.getTokenizer(), parserTools.getIgnored());
			// condition node (is an ASTNode)
			conditionASTNode = (ASTNode) parser.parse(ifThenConstruct);

			if (AoASMPlugin.isDebugMode())
				AspectTools.writeParseTreeToFile(advice.getFirst().getFirst().getToken()+"_condition.dot", conditionASTNode);

			// 2) remove skip rule from new condition
			ASTNode skip = conditionASTNode.getAbstractChildNodes().get(1);
			ASTNode skipParent = skip.getParent();
			Node skipInsertionReference = skip.removeFromTree();

			// 3) remove rule body from advice node
			ASTNode ruleBody;
			Node ruleParent, ruleInsertionReference;

			ruleBody = advice.getPointCut().getNextASTNode();
			ruleParent = ruleBody.getParent();
			ruleInsertionReference = ruleBody.removeFromTree();

			// 4) add rule body to condition at old position of skip
			conditionASTNode.setScannerInfo(ruleBody);
			AspectTools.addChildAfter(skipParent, skipInsertionReference,
					ruleBody.getToken(), ruleBody);//TODO name should be as used for block of if condition

			// 5) create new block rule for condition to satisfy the create method
			LinkedList<ASTNode> conditionList = new LinkedList<ASTNode>();
			conditionList.add(conditionASTNode);
			ASTNode conditionBlock = AspectTools.create(AspectTools.BLOCKRULE,
					conditionList);
			// 6) add condition to old position of the rule body
			AspectTools.addChildAfter(ruleParent, ruleInsertionReference,
					conditionBlock.getToken(), conditionBlock);
			if(AoASMPlugin.isDebugMode())
				AspectTools.writeParseTreeToFile(advice.getFirst().getFirst().getToken()+".dot", advice);
			/// covert advice node into rule declaration

			// 1) save insertion reference
				ruleParent = advice.getParent();
				ruleInsertionReference = advice.removeFromTree();//TODO wrong parent - named pointcut instead of advice
			// 2) transform advice into rule definition
				ASTNode ruleDefinition = getRuleDefinitionFromAdvice(advice);
			// 3) add ruleDefinition to Program
				ruleDefinition.setParent(ruleParent);
				AspectTools.addChildAfter(ruleParent, ruleInsertionReference, ruleDefinition.getToken(), ruleDefinition);

		}else{
			for (ASTNode child : node.getAbstractChildNodes()) {
				orchestrate(child);
			}
		}
	}

	/**
	 * transform the given advice ASTNode into a rule declaration ASTNode
	 *
	 * @param advice given for transformation into a rule declaration
	 * @return rule declaration ASTNode
	 *
	 */
	private ASTNode getRuleDefinitionFromAdvice(AdviceASTNode advice){

		//create components for the rule declaration node
		ASTNode ruleDeclaration;
			//see method RuleDeclarationParseMap in class ParserTools
			ruleDeclaration = new ASTNode(
					null,
					ASTNode.DECLARATION_CLASS,
					Kernel.GR_RULEDECLARATION,
					null,
					advice.getScannerInfo()
					);
			Node ruleKeyword = new Node(
					null,
					"rule",
					advice.getScannerInfo(),
					Node.KEYWORD_NODE
				);
			//the signature of the rule declaration has to be the one from the advice
			ASTNode ruleSignature = advice.getFirst();
			Node equal = new Node(
					"Kernel",
					"=",
					//get ScannerInfo from locator node
					advice.getChildNodes().get(2).getScannerInfo(),
					Node.OPERATOR_NODE
					);
			//the body of the rule declaration is the body of the advice
			ASTNode body = advice.getAbstractChildNodes().get(advice.getAbstractChildNodes().size()-1);

			//compose the components of the rule declaration node
			AspectTools.addChild(ruleDeclaration, ruleKeyword);
			AspectTools.addChild(ruleDeclaration, ruleSignature);
			AspectTools.addChild(ruleDeclaration, equal);
			AspectTools.addChild(ruleDeclaration, body);

			return ruleDeclaration;
	}

	/**
	 * implement call stack into all rule bodies:
	 * 0) declare the function callStack(agent) and initialize it (for every agent?!)
	 * 1) define statements for the a seqblock which embeds the original ruleblock
	 * 	a) add rule signature of every rule to the call stack of the agent self at runtime (first statement of the seqblock)
	 * 	b) remove rule signature from the call stack of agent self (last statement of the seqblock)
	 * 	c) embed original rule block into a new seqblock
	 * 2) define auxiliary rules for the runtime checks of cflow and args conditions
	 */
	private void orchestrate(){

		//insert runtime conditions at the beginning of each advice block rule
		orchestrate(this.getRootnode());

		//step 0
		//get functionSignatureRuleParser to parse the function declaration
		//function callStack : Agents -> LIST initially []
		Parser<Node> functionSignatureRuleParser = ((ParserPlugin) capi
				.getPlugin("SignaturePlugin")).getParsers()
				.get("Signature").parser;// using
		// FunctionSignature
		ParserTools parserTools = ParserTools
				.getInstance(capi);
		Parser<Node> parser = functionSignatureRuleParser
				.from(parserTools.getTokenizer(),
						parserTools.getIgnored());
		Node functionSignatureDeclarationNode = parser
				.parse("function callStack : Agents -> LIST");

		// add new function definition as first child to the
		// root of the parse tree
			ASTNode root = this.getRootnode();
			AspectTools.addChildAfter(root,
			root.getFirst(),
			functionSignatureDeclarationNode.getToken(),
			functionSignatureDeclarationNode);

		if (AoASMPlugin.isDebugMode())
			AspectTools.writeParseTreeToFile("callStack.dot", functionSignatureDeclarationNode);

		//step 1
		LinkedList<ASTNode> ruleDeclarations = getRuleDefinitions(this.getRootnode());
		String sigantureList, updateRuleStart, updateRuleEnd;
		ASTNode updateASTNodeStart, ruleBody, updateASTNodeEnd, parent;
		Node insertionReference;
		for (ASTNode ruleDeclarationASTNode : ruleDeclarations) {
			sigantureList = getRuleSignatureAsCoreASMList(ruleDeclarationASTNode);
			updateRuleStart = "if callStack(self) = undef then callStack(self) := "+sigantureList+" else callStack(self) := cons("+sigantureList+", callStack(self))";//condition for initialization case callStack(self)=undef
			updateRuleEnd = "callStack(self) := tail(callStack(self))";

			//set default rule respectively block as scope for the orchestration
			ruleBody = ruleDeclarationASTNode.getFirst().getNext();

			//if the ruleBody is either a local rule or a return rule,
			//reassign ruleBody with the rule or block its child
			//until this block is no local rule or return rule
			while(ruleBody.getGrammarRule().equals(AspectTools.LOCALRULE) ||
					ruleBody.getGrammarRule().equals(AspectTools.RETURNRULE)){
				ruleBody = ruleBody.getAbstractChildNodes().get(1);
			}

			//store insertion reference and parent before potentially packing this ruleBody into a par block
			parent = ruleBody.getParent();
			insertionReference = ruleBody.removeFromTree();

			//if the rule body is not a block rule then pack it into a new par-block
			//that is necessary, because otherwise weaving would generate unrelated seqblocks within a rule's body
			ASTNode parBlock;
			if ( !ruleBody.getGrammarRule().equals(AspectTools.SEQBLOCKRULE) ||
					!ruleBody.getGrammarRule().equals(AspectTools.BLOCKRULE) )
			{
				LinkedList<ASTNode> ruleBodyList = new LinkedList<ASTNode>();
				ruleBodyList.add(ruleBody);
				parBlock = AspectTools.create(AspectTools.BLOCKRULE, ruleBodyList);
			}
			else
				parBlock = ruleBody;

			//initiate conditional rule parser
			//condition ensures that every callStack is initialized with the first element as new list if undef
			Parser<Node> conditionalRuleParser = ((ParserPlugin)capi.getPlugin("ConditionalRulePlugin")).getParsers().get("Rule").parser;
			parser = conditionalRuleParser.from(parserTools.getTokenizer(),parserTools.getIgnored());

			//update rule elements (are of type ASTNodes)
			updateASTNodeStart = (ASTNode)parser.parse(updateRuleStart);
			updateASTNodeStart.setScannerInfo(parBlock);

			//initiate update rule parser
			Parser<Node> updateRuleParser = ((ParserPlugin) capi
					.getPlugin("Kernel")).getParsers().get(
					"UpdateRule").parser;
			parser = updateRuleParser.from(
					parserTools.getTokenizer(),
					parserTools.getIgnored());

			//update rule elements (are of type ASTNodes)
			updateASTNodeEnd = (ASTNode)parser.parse(updateRuleEnd);
			updateASTNodeEnd.setScannerInfo(parBlock);

			//create new seqblock
			LinkedList<ASTNode> seqblockList = new LinkedList<ASTNode>();
			seqblockList.add(updateASTNodeStart);
			seqblockList.add(parBlock);
			seqblockList.add(updateASTNodeEnd);
			ASTNode seqBlockASTNode =AspectTools.create(AspectTools.SEQBLOCKRULE, seqblockList);

			AspectTools.addChildAfter(parent, insertionReference, seqBlockASTNode.getToken(), seqBlockASTNode);

			if(AoASMPlugin.isDebugMode())
				AspectTools.writeParseTreeToFile(parent.getFirst().getFirst().getToken()+".dot", parent);
		}

		//step 2
		//@formatter:off
		String ruleCallCheck =
				//"//returns a set of rule signatures from the callStack matching the given ruleSignature\n"+
				"rule "+ MATCHING_RULE_INSIDE_CALLSTACK+"(ruleSignature) =\n" +
				"	return res in par\n" +
				"		//just looking for a rulename (i.e. call(x))\n" +
				"		if(head(ruleSignature)!={} and tail(ruleSignature)={}) then\n" +
				"			res := {signature | signature in callStack(self) with matches(head(signature),head(ruleSignature))}\n" +
				"			//looking for a rule signature i.e. call(x,[p1,...,pn])\n" +
				"		else if ( tail(ruleSignature) != {} ) then\n" +
				"			res := { signature | signature in callStack(self) with signature = ruleSignature }\n" +
				"		else res := {}\n"+
				"	endpar";

		
		String argsCheck =
				"//returns a set of rule signatures from the callStack matching the given argument list\n"
						+ "rule "+ MATCHING_SIGNATURE_INSIDE_CALLSTACK + "(listOfArguments) =\n"
						+ "	return res in par\n"
						+ "		res := { signature | signature in callStack(self) with tail(signature) = listOfArguments }\n"
						+ "	endpar";
		//@formatter:on

		Parser<Node> ruleDeclarationParser = ((ParserPlugin) capi
				.getPlugin("Kernel")).getParser("RuleDeclaration");// using
		parserTools = ParserTools
				.getInstance(capi);
		parser = ruleDeclarationParser
				.from(parserTools.getTokenizer(),
						parserTools.getIgnored());
		Node ruleCallASTNode = parser
				.parse(ruleCallCheck);
		Node argsCheckASTNode = parser
				.parse(argsCheck);

		//ASTNode rootNode = TestEngineDriver.getRootNodeFromSpecification(ruleCallCheck + "\n" + argsCheck);

		//ASTNode ruleCallASTNode = AspectTools.findRuleDeclaration(rootNode, MATCHING_RULE_INSIDE_CALLSTACK);
		String dot = AspectTools.nodes2dot(ruleCallASTNode);
		AspectTools.createDotGraph(dot, new LinkedList<Node>());

		//ASTNode argsCheckASTNode = AspectTools.findRuleDeclaration(rootNode, MATCHING_SIGNATURE_INSIDE_CALLSTACK);
		dot = AspectTools.nodes2dot(argsCheckASTNode);
		AspectTools.createDotGraph(dot, new LinkedList<Node>());
		//reset capi to previous one
		AspectTools.setCapi(capi);

		// add new rule definitions as first children to the
		// root of the parse tree
		root = this.getRootnode();
		AspectTools.addChildAfter(root, capi.getParser().getRootNode().getFirst(),
				Node.DEFAULT_NAME, ruleCallASTNode);

		AspectTools.addChildAfter(root, capi.getParser().getRootNode().getFirst(),
				Node.DEFAULT_NAME, argsCheckASTNode);
	}

	/**
	 * collects all rule definitions of the given specification to ease orchestration
	 *
	 * @param node  should be the root node of the specification
	 * @return list of rule declaration ASTNodes
	 */
	private LinkedList<ASTNode> getRuleDefinitions(ASTNode node){
		LinkedList<ASTNode> ruleDeclarations = new LinkedList<ASTNode>();
		if (node.getGrammarRule().equals(Kernel.GR_RULEDECLARATION))
			ruleDeclarations.add(node);
		else if (!node.getAbstractChildNodes().isEmpty())
			for (ASTNode astNode : node.getAbstractChildNodes()) {
				ruleDeclarations.addAll(getRuleDefinitions(astNode));
			}
		return ruleDeclarations;
	}

	/**
	 * return a rule or function signature as coreasm string
	 *
	 * @param astNode should be a rule definition or function rule term
	 * @return  string in coreasm syntax representing the signature of the given node
	 */
	private String getRuleSignatureAsCoreASMList(ASTNode astNode){
 		String ruleSignatureAsCoreASMList="";
		ASTNode node;
		if (astNode.getGrammarRule().equals(Kernel.GR_RULEDECLARATION) || astNode instanceof FunctionRuleTermNode) {
			if (astNode.getGrammarRule().equals(Kernel.GR_RULEDECLARATION))
			{
				ruleSignatureAsCoreASMList+="[";
			}
				//signature node
			node=astNode.getFirst();
			ASTNode signatureElement;
			//add name of rule/function surrounded by quotes
			ruleSignatureAsCoreASMList += "\""+node.getFirst().getToken()+"\"";
			//add parameters
			for (int i = 1; i < node.getAbstractChildNodes().size();i++) {
				signatureElement=node.getAbstractChildNodes().get(i);
				ruleSignatureAsCoreASMList+= ", ";
				ruleSignatureAsCoreASMList += getRuleSignatureAsCoreASMList(signatureElement);
				//if its not the last signature element, insert a colon, too.
			}

			if (astNode.getGrammarRule().equals(Kernel.GR_RULEDECLARATION))
			{
				ruleSignatureAsCoreASMList+="]";
			}
		}else //ID, NUMBER, BOOLEAN, StringTerm, BooleanTerm, KernelTerms
		{
			ruleSignatureAsCoreASMList = astNode.getToken();
		}
		return ruleSignatureAsCoreASMList;
	}

	/**
	 * This method returns a hash-map of with weaving candidates and their
	 * advices for all macrocallrules which are not used in blocks inside any
	 * advice.
	 *
	 * @param astNodes
	 * @return
	 * @throws Exception
	 */
	private HashMap<ASTNode, LinkedList<AdviceASTNode>> pointCutMatching(HashMap<String, LinkedList<ASTNode>> astNodes) throws Exception {
		HashMap<ASTNode, LinkedList<AdviceASTNode>> weavingCandidates = new HashMap<ASTNode, LinkedList<AdviceASTNode>>();
		LinkedList<ASTNode> candidates = new LinkedList<ASTNode>();
		for (ASTNode functionRuleTerm : astNodes.get("FunctionRuleTerm")) {
			if (!insideAdviceRule(functionRuleTerm))
				for (ASTNode advice : astNodes
						.get(AdviceASTNode.NODE_TYPE)) {
					// if an advice matches the current astnode
					// it is added to the hashmap of candidates for weaving
					Binding binding = ((AdviceASTNode) advice).matches(functionRuleTerm);
					if (binding.exists() && !binding.getBinding().isEmpty()) {
						//create marker	
						AoASMPlugin.createMarker(capi, functionRuleTerm, binding);
						if (weavingCandidates.get(functionRuleTerm) == null) {
							LinkedList<AdviceASTNode> newAdviceList = new LinkedList<AdviceASTNode>();
							newAdviceList.add((AdviceASTNode) advice);
							weavingCandidates.put(functionRuleTerm, newAdviceList);
						} else {
							LinkedList<AdviceASTNode> oldAdviceList = weavingCandidates
									.get(functionRuleTerm);
							oldAdviceList.add((AdviceASTNode) advice);
							weavingCandidates.put(functionRuleTerm, oldAdviceList);
						}
					}
				}
		}
		return weavingCandidates;
	}

	/**
	 * if the given macro call is defined inside an advice block then true is
	 * returned
	 *
	 * @param macroCall
	 * @return
	 */
	private boolean insideAdviceRule(ASTNode macroCall) {
		if (macroCall.getGrammarRule().equals("RuleDeclaration"))
			return false;
		else if (macroCall instanceof AdviceASTNode)
			return true;
		else if (macroCall.getParent() != null)
			return insideAdviceRule(macroCall.getParent());
		return false;
	}

	/**
	 * Add an adviceRule to <code>adviceRules</code>
	 *
	 * @param name
	 *            name of the adviceRule
	 * @param adviceRule
	 *            aspectOfThe adviceRule
	 */
	public void addAdviceRuleElement(String name, RuleElement adviceRule) {
		if (!adviceRules.containsKey(name))
			adviceRules.put(name, adviceRule);
		else
			throw new CoreASMError("AdviceRule with " + name
					+ " already exsists! " + adviceRule.toString());
	}

	/**
	 * resets the weaving environment for the next weaving
	 */
	void reset() {
		astNodes.clear();
		AdviceASTNode.resetAdviceCounter();
		adviceRules.clear();
		this.capi = null;
		initialize = false;
	}

	/**
	 * @return the rootnode
	 */
	public ASTNode getRootnode() {
		return rootnode;
	}

	/**
	 * @param rootnode the rootnode to set
	 */
	public void setRootnode(ASTNode rootnode) {
		this.rootnode = rootnode;
	}
	/**
	 * 
	 * @return currently used control api
	 */
	public ControlAPI getControlAPI(){
		return capi;
	}

}
