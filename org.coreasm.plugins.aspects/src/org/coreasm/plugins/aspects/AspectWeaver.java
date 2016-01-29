/**
 * AoASM CoreASM Plugin
 * 
 * The weaver integrates the aspects in terms of new nodes into the AST of the
 * current CoreASM program. First, the weaver has to be initialized with the
 * current ControlAPI capi and afterwards the weaving can be started.
 * 
 * @author Marcel Dausend
 */
package org.coreasm.plugins.aspects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jparsec.Parser;
import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMWarning;
import org.coreasm.engine.absstorage.RuleElement;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.kernel.Kernel;
import org.coreasm.engine.parser.ParserTools;
import org.coreasm.engine.plugin.ParserPlugin;
import org.coreasm.engine.plugins.chooserule.ChooseRuleNode;
import org.coreasm.engine.plugins.conditionalrule.ConditionalRuleNode;
import org.coreasm.engine.plugins.forallrule.ForallRuleNode;
import org.coreasm.engine.plugins.letrule.LetRuleNode;
import org.coreasm.engine.plugins.turboasm.LocalRuleNode;
import org.coreasm.engine.plugins.turboasm.ReturnRuleNode;
import org.coreasm.engine.plugins.turboasm.SeqRuleNode;
import org.coreasm.plugins.aspects.errorhandling.AspectException;
import org.coreasm.plugins.aspects.errorhandling.BindingException;
import org.coreasm.plugins.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.Binding;
import org.coreasm.plugins.aspects.pointcutmatching.NamedPointCutASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.NamedPointCutDefinitionASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.PointCutParameterNode;
import org.coreasm.plugins.aspects.utils.AspectTools;

public class AspectWeaver {

	/** singletonWeaver hold the singleton instance of this class */
	private static AspectWeaver singletonWeaver = null;

	/**
	 * stores the current value of the ControlAPI capi and ASTNode rootnode
	 * which is changed during initialization()
	 * 
	 * @see {initialize(ControlAPI capi, ASTNode rootnode)}
	 */
	///@{
	private ControlAPI capi;
	private ASTNode rootnode;
	///@}

	/**
	 * collections which are used to ease access to astnodes (sorted by grammar
	 * and advice rules)
	 */
	///@{
	private static HashMap<String, LinkedList<ASTNode>> astNodes = new HashMap<String, LinkedList<ASTNode>>();
	private final HashMap<String, RuleElement> adviceRules = new HashMap<String, RuleElement>();
	///@}

	/**
	 * status of the initialization of the current instance of the weaver,
	 * initialized with false
	 */
	private boolean initialize;

	/** name for rules which are introduced by orchestration */
	///@{
	public static final String MATCHING_RULE_INSIDE_CALLSTACK = "matchingRuleCallsInsideCallstack";
	public static final String MATCHING_SIGNATURE_INSIDE_CALLSTACK = "matchingParameterSignatureInsideCallstack";

	///@}

	/** private constructor used by singleton pattern */
	private AspectWeaver() {
		this.initialize = false;
	}

	/**
	 * implements singleton pattern for AspectWeaver
	 * 
	 * @return singleton instance of class AspectWeaver
	 */
	public static AspectWeaver getInstance() {
		if (singletonWeaver == null)
			singletonWeaver = new AspectWeaver();
		return singletonWeaver;
	}

	/**
	 * returns a hashmap of astnodes of the current specification
	 * 
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
	 * @param capi
	 *            current control api used for parsing and/or execution
	 * @param rootnode
	 *            current specification is given by the current rootnode and its
	 *            children
	 * @return if the specification is ready for weaving true is returned
	 *         otherwise false
	 */
	public boolean initialize(ControlAPI capi, ASTNode rootnode) {

		if (!initialize && capi != null && rootnode != null) {
			//set capi and rootnode
			this.capi = capi;
			this.setRootnode(rootnode);

			// collect all ASTNodes from aspects from the capi
			astNodes = new HashMap<String, LinkedList<ASTNode>>();
			AspectTools.collectASTNodesByGrammar(astNodes, rootnode);

			//continue initialization
			if (astNodes.get("FunctionRuleTerm") != null
					&& astNodes.get(AdviceASTNode.NODE_TYPE) != null) {
				this.initialize = true;
				return true;
			}
			else {
				return false;
			}
		}
		return false;
	}

	/**
	 * statically check the specification for errors
	 * 
	 * @throws AspectException
	 */
	private void PerformStaticChecks() throws AspectException{

		/**
		 * check if all named pointcut parameters are used on the righthandside
		 */
		LinkedList<ASTNode> namedPointCutDefinitions = astNodes.get("NamedPointCutDefinitionASTNode");
		if (namedPointCutDefinitions != null) {
			for (ASTNode nptcdef : astNodes.get("NamedPointCutDefinitionASTNode")) {
				NamedPointCutDefinitionASTNode definition = (NamedPointCutDefinitionASTNode) nptcdef;
				List<ASTNode> parameters;
				if (!((parameters = definition.requiredParametersContained()).isEmpty()))
					for (ASTNode param : parameters) {
						throw new CoreASMError(definition.getName() + " requires parameters"
								+ parameters.toString(), param);
					}
				for (PointCutParameterNode unboundParameter : definition.getUnboundPointCutParameters())
					AspectTools.getCapi().warning(new CoreASMWarning(AoASMPlugin.PLUGIN_NAME, "The pointcut parameter " + unboundParameter.getName() + " in named pointcut " + definition.getName() + " is unbound!", unboundParameter.getFuntionRuleTermNode()));
			}
		}
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
			LinkedList<ASTNode> nPtcList = AspectWeaver.getInstance().getAstNodes()
					.get(NamedPointCutDefinitionASTNode.NODE_TYPE);
			if (nPtcList != null)
				for (ASTNode nptc : nPtcList) {
					if (nptc instanceof NamedPointCutDefinitionASTNode) {
						/*
						 * the set substituted keeps track of the named
						 * pointcuts that have already
						 * taken into account during the substitution.
						 * If a named pointcut would be taken into account
						 * twice,
						 * the definition of named pointcuts is cyclic!
						 */
						HashSet<String> substituted = new HashSet<String>();
						NamedPointCutDefinitionASTNode nptcdef = (NamedPointCutDefinitionASTNode) nptc;
						substituted.add(nptcdef.getName());
						//perform the substitution recursively
						substituteNamedPointcuts(nptcdef.getPointCut(), substituted);
						nptcdef.removeFromTree();
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
			}
			catch (CoreASMError e) {
				capi.error(e);
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
				LinkedList<ASTNode> parallelNodes = new LinkedList<ASTNode>();

				//each advice which had been matched to the current candidate
				//requires weaving
				for (AdviceASTNode advice : weavingCandidates.get(candidate)) {

					//if there exist matching arg expressions in the pointcut of the advice...
					/**
					 * \note LinkedList<ArgsASTNode> argsList holds the list of
					 * valid bindings for the given advice and candidate
					 */
					Binding binding = advice.getBinding(candidate);
					AdviceASTNode boundAdvice;
					/**
					 * \note Binding binding holds the first binding from the
					 * valid bindings, and thereby determines the precedence of
					 * bindings in pointcuts from left to right
					 */
					//...clone the current node and insert the binding depending for the current candidate
					boundAdvice = advice.cloneWithBinding(binding);

					// create adviceMacroCallRules and add each to its
					// collection
					if (boundAdvice.getLocator().equals("before")) {
						beforeNodes.add(AspectTools.create(
								AspectTools.MACROCALLRULE, boundAdvice));
					}
					else if (boundAdvice.getLocator().equals("after")) {
						afterNodes.add(AspectTools.create(
								AspectTools.MACROCALLRULE, boundAdvice));
					}
					else if (boundAdvice.getLocator().equals("around")) {
						aroundNodes.add(AspectTools.create(
								AspectTools.MACROCALLRULE, boundAdvice));
					}
					else if (boundAdvice.getLocator().equals("parallel")) {
						parallelNodes.add(AspectTools.create(
								AspectTools.MACROCALLRULE, boundAdvice));
					}
					else {
					}
				}

				// insert nodes into a new seqblock
				// which will replace the current MacroCallRule
				SeqRuleNode rootNodeOfSeqBlockSequence = AspectTools
						.create(AspectTools.SEQBLOCKRULE,
								candidate.getScannerInfo());// new
															// SeqBlockRuleNode(candidate.getScannerInfo());
				SeqRuleNode seqBlockNode = rootNodeOfSeqBlockSequence;

				// get the parent of candidate and remove the candidate from its
				// parent but store its insertion reference for the replacement
				// action
				// TODO complete this list according pointcut constructs
				ASTNode insertionContext = candidate;
				while (insertionContext != null
						&& !(insertionContext.getParent() instanceof ConditionalRuleNode)
						&& !(insertionContext.getParent() instanceof SeqRuleNode)
						&& !(insertionContext.getParent() instanceof LetRuleNode)
						&& !(insertionContext.getParent() instanceof ReturnRuleNode)
						&& !(insertionContext.getParent() instanceof LocalRuleNode)
						&& !(insertionContext.getParent() instanceof ChooseRuleNode)
						&& !(insertionContext.getParent() instanceof ForallRuleNode)
						&& !(insertionContext.getParent() instanceof FunctionRuleTermNode)
						&& !("BlockRule".equals(insertionContext.getParent().getGrammarRule())))
					insertionContext = insertionContext.getParent();

				ASTNode parentOfInsertionContext = insertionContext.getParent();
				String nodeName = AspectTools.getNodeName(insertionContext);
				Node insertionReference = insertionContext.removeFromTree();

				// change macroCallRule if there is exactly one around advice,
				// else insert nodes into seqblockrule
				/*if (aroundNodes.size() == 1 && beforeNodes.isEmpty()
						&& afterNodes.isEmpty()) {
					if (insertionReference != null)
						AspectTools.addChildAfter(parentOfInsertionContext, insertionReference,
								nodeName,
								aroundNodes.getFirst());
					else
						AspectTools.addChild(parentOfInsertionContext, aroundNodes.getFirst());
				}
				else {*/
					// add before nodes
					if (!beforeNodes.isEmpty()) {
						ASTNode beforeRuleBlock = AspectTools.create(
								AspectTools.BLOCKRULE, beforeNodes);
						seqBlockNode = AspectTools.insert(seqBlockNode,
								beforeRuleBlock);
					}
					ASTNode middleRuleBLock = AspectTools
							.create(AspectTools.BLOCKRULE,
									candidate.getScannerInfo());
					// if weather around nodes nor parallel nodes exist, insert the candidate here
					if (aroundNodes.isEmpty() && parallelNodes.isEmpty()) {
						seqBlockNode = AspectTools.insert(seqBlockNode,
								insertionContext);
					}
					//add around and parallel nodes to middleBlockRule node
					else {
						// add around nodes
						if (!aroundNodes.isEmpty()){
							ASTNode aroundRuleBlock = AspectTools.create(
									AspectTools.BLOCKRULE, aroundNodes);
							middleRuleBLock = AspectTools.insert(middleRuleBLock,
									aroundRuleBlock);
						}else { //insert candidate if no around advice exists
							middleRuleBLock = AspectTools.insert(middleRuleBLock,
									insertionContext);
						}
						// add parallel nodes to middle block
						if (!parallelNodes.isEmpty()) {
							ASTNode parallelRuleBlock = AspectTools.create(
									AspectTools.BLOCKRULE, parallelNodes);
							middleRuleBLock = AspectTools.insert(middleRuleBLock,
									parallelRuleBlock);
						}
						//close middle block
						AspectTools.close(middleRuleBLock, null);
						//add middle block to seqblock
						seqBlockNode = AspectTools.insert(seqBlockNode,
								middleRuleBLock);
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
					AspectTools.addChildAfter(parentOfInsertionContext, insertionReference,
							nodeName,
							rootNodeOfSeqBlockSequence);
				//} end else
			}
			/**
			 * \todo remove non matching advices and shift all definitions to
			 * the main context of the program @see orchestrate(ASTNode node)
			 */
		}
		// prepare next weaving (new initialization required)
		reset();
	}

	/**
	 * replace named pointcut expressions with the righthand side of this named
	 * pointcut definition
	 * 
	 * @param astnode
	 *            node that eventually has to be replaced
	 * @param substituted
	 *            list of substituted named pointcuts (keep track of to avoid
	 *            cyclic substitutions)
	 * @throws BindingException
	 *             thrown if cyclic substitution or named pointcut is not
	 *             defined
	 */
	private static void substituteNamedPointcuts(ASTNode astnode, HashSet<String> substituted) throws BindingException {
		if (astnode instanceof NamedPointCutASTNode) {
			NamedPointCutASTNode nptc = ((NamedPointCutASTNode) astnode);
			if (substituted.contains(nptc.getName()))
				throw new CoreASMError("cycle in pointcut defintion!", astnode);
			//exchange
			LinkedList<ASTNode> nptcdefs = AspectWeaver.getInstance().getAstNodes()
					.get(NamedPointCutDefinitionASTNode.NODE_TYPE);
			boolean notDefined = true;
			if (nptcdefs != null)
				for (ASTNode nptcdef : nptcdefs) {
					if (nptcdef instanceof NamedPointCutDefinitionASTNode) {
						NamedPointCutDefinitionASTNode definition = (NamedPointCutDefinitionASTNode) nptcdef;
						if (definition.isDefinitionOf(nptc)) {
							ASTNode parent = nptc.getParent();
							Node positionToInsert = nptc.removeFromTree();
							Node pointcut = cloneWithBinding(definition, nptc);
							//pointcut.setParent(parent);//TODO add surrounding round brackets
							AspectTools.addChildAfter(parent, positionToInsert, definition.getName(), pointcut);
							substituted.add(definition.getName());
							substituteNamedPointcuts(parent, substituted);
							notDefined = false;
							break;
						}
					}
				}
			if (notDefined)
				throw new CoreASMError("Named pointcut " + nptc.unparseTree() + " is not defined.", nptc);
		}
		else
			for (ASTNode child : astnode.getAbstractChildNodes())
				substituteNamedPointcuts(child, substituted);
	}

	/**
	 * replace variables in pointcut expressions which will replace a
	 * namedpointcut with the corresponding ids used in this namedpointcut
	 * 
	 * @param def
	 *            definition form which the pointcut that is used to replace the
	 *            namedpointcut is extracted
	 * @param nptc
	 *            named pointcut that has to be replaced
	 * @return pointcut from def with exchanged variables according to the here
	 *         constructed binding
	 * @throws AspectException
	 */
	private static Node cloneWithBinding(NamedPointCutDefinitionASTNode def, NamedPointCutASTNode nptc)
			throws BindingException {
		//binding between variables from def as key and nptc variables as value
		HashMap<String, FunctionRuleTermNode> binding = new HashMap<String, FunctionRuleTermNode>();
		for (int i = 0; i < def.getPointCutParameters().size(); i++) {
			FunctionRuleTermNode value = binding.put(def.getPointCutParameters().get(i).getToken(), nptc
					.getPointCutParameters().get(i));
			if (value != null && !value.getName().equals(nptc.getPointCutParameters().get(i).getToken()))
				throw new BindingException("Binding inconsistent!", nptc.getPointCutParameters().get(i));
		}
		ASTNode pointcut = (ASTNode) def.getPointCut().cloneTree();
		//replacement of the variables within pointcut according to the binding
		cloneWithBinding(pointcut, binding);

		return pointcut;
	}

	/**
	 * replace id nodes in pointcut parameternodes with ASTNodes from the given
	 * binding if their name is a key of that binding
	 * 
	 * @param node
	 * @param binding
	 */
	private static void cloneWithBinding(ASTNode node, HashMap<String, FunctionRuleTermNode> binding) {
		if (node instanceof PointCutParameterNode) {
			PointCutParameterNode param = (PointCutParameterNode) node;
			if (param.getFuntionRuleTermNode() != null && binding.containsKey(param.getName())) {
				String paramName = param.getName();
				FunctionRuleTermNode fnNode = param.getFuntionRuleTermNode();
				Node insertionReference = fnNode.removeFromTree();
				AspectTools.addChildAfter(param, insertionReference, "beta", binding.get(paramName));
			}
		}
		for (ASTNode child : node.getAbstractChildNodes())
			cloneWithBinding(child, binding);
	}

	/**
	 * inserts recursively a runtime condition to every advice rule inside its
	 * rule block
	 * 
	 * @param node
	 */
	private void orchestrate(ASTNode node) {

		//@formatter:off
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
		//@formatter:on
		if (node instanceof AdviceASTNode) {
			AdviceASTNode advice = (AdviceASTNode) node;

			//cleanup bindings by removing bindings with null created by dynamic joinpoint constructs like cflow
			List<ASTNode> params = advice.removeDynamicBindingParameters();

			//introduce letConstruct that inserts cflow bindings into advice rule blocks
			ASTNode letConstruct = null;
			String letExpression = advice.getPointCut().getLetExpression();
			if (!letExpression.isEmpty()) {
				String letDeclaration = "";
				String cflowBindings = advice.getPointCut().getCflowBindings();
				letDeclaration = "let\n"
						+ letExpression
						+ " in\n"
						+ " let _cflow_bindings_ = " + cflowBindings + " in\n"
						+ "if "
						+ "_cflow_bindings_ != undef then\n";
				if (!params.isEmpty()){
					letDeclaration += "let\n";
					Iterator<ASTNode> it = params.iterator();
					while (it.hasNext()) {
						ASTNode astNode = it.next();
						String name = astNode.getToken();
						letDeclaration += name + " = GetBindingValue( "
								+ "\"" + name + "\", _cflow_bindings_ )";
						if (it.hasNext())
							letDeclaration += ", ";
					}
					letDeclaration += "in\n";
				}
				letDeclaration += "skip";
				// get condition parser to parse the condition
				Parser<Node> letParser = ((ParserPlugin) capi
						.getPlugin("LetRulePlugin")).getParsers().get(
						"Rule").parser;// using
				ParserTools parserTools = ParserTools.getInstance(capi);
				Parser<Node> parser = letParser.from(
						parserTools.getTokenizer(), parserTools.getIgnored());
				letConstruct = (ASTNode) parser.parse(letDeclaration);
			}

			// get condition parser to parse the condition
			Parser<Node> conditionParser = ((ParserPlugin) capi
					.getPlugin("ConditionalRulePlugin")).getParsers().get(
					"Rule").parser;// using

			//a) add runtime condition of pointcuts to advice body
			// 1) create condition ASTNode with runtime expressions from
			// pointcuts
			ASTNode conditionASTNode = null;
			String condition = advice.getPointCut().getCondition();
			if (condition.isEmpty())
				condition = "true";
			String ifThenConstruct = "if (" + condition + ") then skip";

			// FunctionSignature
			ParserTools parserTools = ParserTools.getInstance(capi);
			Parser<Node> parser = conditionParser.from(
					parserTools.getTokenizer(), parserTools.getIgnored());
			// condition node (is an ASTNode)
			conditionASTNode = (ASTNode) parser.parse(ifThenConstruct);

			//ASTNode to be inserted into the rule definition created from based on the current advice
			ASTNode insertOrchestration = letConstruct;

			//if let is not null, i.e. there are cflow bindings, then insert conditional into let construct
			if (insertOrchestration != null) {
				//find skip rule to be replaced with conditional
				ASTNode lastChild = insertOrchestration;
				while (lastChild != null && !lastChild.getAbstractChildNodes().isEmpty()) {
					lastChild = lastChild.getAbstractChildNodes().get(lastChild.getAbstractChildNodes().size() - 1);
				}
				lastChild.replaceWith(conditionASTNode);
			}
			else
				//no cflow bindings exist
				insertOrchestration = conditionASTNode;

			ASTNode skip = conditionASTNode.getAbstractChildNodes().get(1);
			ASTNode ruleBlock = advice.getRuleBlock();
			// 2) replace ruleBlock in advice by let construct that includes the condition rule
			ruleBlock.replaceWith(insertOrchestration);
			// 3) replace skip in condition rule by ruleBlock
			skip.replaceWith(ruleBlock);
			// 4) replace advice definition by corresponding rule declaration
			advice.replaceWith(advice.makeRuleDeclaration());
		}
		else {
			for (ASTNode child : node.getAbstractChildNodes()) {
				orchestrate(child);
			}
		}
	}

	/**
	 * implement call stack into all rule bodies:
	 * 0) declare the function callStack(agent) and initialize it (for every
	 * agent?!)
	 * 1) define statements for the a seqblock which embeds the original
	 * ruleblock
	 * a) add rule signature of every rule to the call stack of the agent self
	 * at runtime (first statement of the seqblock)
	 * b) remove rule signature from the call stack of agent self (last
	 * statement of the seqblock)
	 * c) embed original rule block into a new seqblock
	 * 2) define auxiliary rules for the runtime checks of cflow and args
	 * conditions
	 */
	private void orchestrate() {

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
				Node.DEFAULT_NAME,
				functionSignatureDeclarationNode);

		//step 1
		LinkedList<ASTNode> ruleDeclarations = getRuleDefinitions(this.getRootnode());
		String signatureList, updateRuleStart, updateRuleEnd;
		ASTNode updateASTNodeStart, ruleBody, updateASTNodeEnd, parent;
		Node insertionReference;
		for (ASTNode ruleDeclarationASTNode : ruleDeclarations) {
			signatureList = AspectTools.getRuleSignatureAsCoreASMList(ruleDeclarationASTNode);
			updateRuleStart = "if callStack(self) = undef then callStack(self) := [" + signatureList + "]"
					+ " else callStack(self) := cons(" + signatureList + ", callStack(self))";//condition for initialization case callStack(self)=undef
			updateRuleEnd = "callStack(self) := tail(callStack(self))";

			//set default rule respectively block as scope for the orchestration
			ruleBody = ruleDeclarationASTNode.getFirst().getNext();

			//if the ruleBody is either a local rule or a return rule,
			//reassign ruleBody with the rule or block its child
			//until this block is no local rule or return rule
			while (ruleBody.getGrammarRule().equals(AspectTools.LOCALRULE) ||
					ruleBody.getGrammarRule().equals(AspectTools.RETURNRULE)) {
				if ( ruleBody instanceof LocalRuleNode)
					ruleBody = ((LocalRuleNode)ruleBody).getRuleNode();
				else ruleBody = ((ReturnRuleNode)ruleBody).getRuleNode();
			}

			//store insertion reference and parent before potentially packing this ruleBody into a par block
			parent = ruleBody.getParent();
			String nodeName = AspectTools.getNodeName(ruleBody);
			insertionReference = ruleBody.removeFromTree();

			//if the rule body is not a block rule then pack it into a new par-block
			//that is necessary, because otherwise weaving would generate unrelated seqblocks within a rule's body
			ASTNode parBlock;
			if (!ruleBody.getGrammarRule().equals(AspectTools.SEQBLOCKRULE) ||
					!ruleBody.getGrammarRule().equals(AspectTools.BLOCKRULE))
			{
				LinkedList<ASTNode> ruleBodyList = new LinkedList<ASTNode>();
				ruleBodyList.add(ruleBody);
				parBlock = AspectTools.create(AspectTools.BLOCKRULE, ruleBodyList);
			}
			else
				parBlock = ruleBody;

			//initiate conditional rule parser
			//condition ensures that every callStack is initialized with the first element as new list if undef
			Parser<Node> conditionalRuleParser = ((ParserPlugin) capi.getPlugin("ConditionalRulePlugin")).getParsers()
					.get("Rule").parser;
			parser = conditionalRuleParser.from(parserTools.getTokenizer(), parserTools.getIgnored());

			//update rule elements (are of type ASTNodes)
			updateASTNodeStart = (ASTNode) parser.parse(updateRuleStart);
			updateASTNodeStart.setScannerInfo(parBlock);

			//initiate update rule parser
			Parser<Node> updateRuleParser = ((ParserPlugin) capi
					.getPlugin("Kernel")).getParsers().get(
					"UpdateRule").parser;
			parser = updateRuleParser.from(
					parserTools.getTokenizer(),
					parserTools.getIgnored());

			//update rule elements (are of type ASTNodes)
			updateASTNodeEnd = (ASTNode) parser.parse(updateRuleEnd);
			updateASTNodeEnd.setScannerInfo(parBlock);

			//create new seqblock
			LinkedList<ASTNode> seqblockList = new LinkedList<ASTNode>();
			seqblockList.add(updateASTNodeStart);
			seqblockList.add(parBlock);
			seqblockList.add(updateASTNodeEnd);
			ASTNode seqBlockASTNode = AspectTools.create(AspectTools.SEQBLOCKRULE, seqblockList);

			AspectTools.addChildAfter(parent, insertionReference, nodeName, seqBlockASTNode);
		}

		//step 2
		//insert auxilliary functions for cflow support
		insertCflowAuxilliaryFunctions();
	}

	private void insertCflowAuxilliaryFunctions() {
		//insert derived function definitions for cflow bindings
		List<String> auxilliaryDefintions = new ArrayList<String>();

		auxilliaryDefintions
				.add("derived isConsistent(binding) = forall t1 in binding , t2 in binding holds last(t1) = last(t2) implies head(t1) = head(t2)");
		auxilliaryDefintions
				.add("derived callStackMatches(callSignature) = if callStack(self) = undef then undef else head(filter([CreateBinding(c, callSignature) | c in reverse(callStack(self)) with CreateBinding(c, callSignature) != undef ], @isConsistent))");
		auxilliaryDefintions
				.add("derived Concat(l1, l2) = return res in if l2 = undef then res := l1 else if l1 = undef then res := l2 else res := l1 + l2");
		auxilliaryDefintions.add(
				"derived CreateBinding(sig, ruleSignature) =\n" + "	return binding in\n" +
						"		let pattern = map(ruleSignature, @ExtractPattern ) in\n" +
						"			if |sig| = |pattern| then\n" +
						"				//toString to convert all parameters to Strings\n" +
						"				if forall c in zipwith(map(sig,@toString), pattern, @matches) holds c then\n" +
						"					binding := IdsIntoList(sig, ruleSignature)\n" +
						"				else binding := undef\n" + "			else binding := undef");

		auxilliaryDefintions.add(
				"derived ExtractPattern(list) ="
						+ "	return singleElementList in"
						+ "		if LIST(list) then"
						+ "			singleElementList := toString(head(list))"
						+ "		else"
						+ "			singleElementList := toString(list)");
		auxilliaryDefintions
				.add("derived IdsIntoList(sig, patternsig) = filter(zip(sig, map(patternsig, @ExtractId)), @LastNotUndef)");
		auxilliaryDefintions.add("derived ExtractId(list) ="
						+ "	return id in"
						+ "		if LIST(list) then"
						+ "			id := last(list)"
						+ "		else"
						+ "			id := undef");
		auxilliaryDefintions.add("derived LastNotUndef(tuple) = last(tuple) != undef");

		auxilliaryDefintions
				.add("derived GetBindings(bindingVar, matchingResult) = map(filter(zip(matchingResult, replicate(bindingVar, |matchingResult|)), @NameEquals), @head)");
		auxilliaryDefintions.add("derived NameEquals(elem) = last(elem) = last(head(elem))");
		auxilliaryDefintions.add("derived GetValues(bindinglist) = map(bindinglist, @head)");
		auxilliaryDefintions.add(
				"derived AndBinding(list1, list2) =\n"
						+ "	return res in local list in\n"
						+ "seq\n"
						+ "if list1 = undef or list2 = undef then list := undef\n"
						+ "else list := list1 + list2\n"
						+ "next"
						+ "		if list = undef then"
						+ "			res := undef"
						+ "		else if"
						+ "			forall id in GetIds(list) holds"
						+ "			AllEqual(GetValues(GetBindings(id, list)))"
						+ "		then"
						+ "			res := list"
						+ "		else"
						+ "			res := undef");
		auxilliaryDefintions.add(
				"derived OrBinding(list1, list2) =\n"
						+ "	return res in local list in\n"
						+ "seq\n"
						+ "if list1 = undef and list2 = undef then list := undef\n"
						+ "else if list1 = undef then list := list2\n"
						+ "else if list2 = undef then list := list1\n"
						+ "else list := list1 + list2\n"
						+ "next\n"
						+ "		if list = undef then\n"
						+ "			res := undef\n"
						+ "		else\n"
						+ "			//create a list by using a set comprehension as workaround\n"
						+ "			res := toList({ x is [ GetBindingValue(id, list), id] | id in GetIds(list) })");
		auxilliaryDefintions
				.add("derived NotBinding(list) = return res in if list = undef then res := [] else res := undef");
		auxilliaryDefintions.add("derived GetIds(bindinglist) = map(bindinglist, @last)");
		auxilliaryDefintions.add("derived AllEqual(list) = forall element in tail(list) holds element = head(list)");

		auxilliaryDefintions
				.add("derived GetBindingValue(bindingVar, matchingResult) = head(GetValues(GetBindings(bindingVar, matchingResult)))");
		Parser<Node> signatureParser = ((ParserPlugin) capi.getPlugin("SignaturePlugin"))
				//.getParsers().get("DerivedFunctionDeclaration").parser;
				.getParsers().get("Signature").parser;
		ParserTools parserTools = ParserTools
				.getInstance(capi);
		Parser<Node> parser = signatureParser
				.from(parserTools.getTokenizer(),
						parserTools.getIgnored());
		for (String definition : auxilliaryDefintions) {
			Node def = parser.parse(definition);
			ASTNode root = this.getRootnode();
			AspectTools.addChildAfter(root, root.getFirst(), Node.DEFAULT_NAME, def);
		}
	}

	/**
	 * collects all rule definitions of the given specification to ease
	 * orchestration
	 * 
	 * @param node
	 *            should be the root node of the specification
	 * @return list of rule declaration ASTNodes
	 */
	private LinkedList<ASTNode> getRuleDefinitions(ASTNode node) {
		LinkedList<ASTNode> ruleDeclarations = new LinkedList<ASTNode>();
		if (node.getGrammarRule().equals(Kernel.GR_RULEDECLARATION))
			ruleDeclarations.add(node);
		else if (!node.getAbstractChildNodes().isEmpty())
			for (ASTNode astNode : node.getAbstractChildNodes()) {
				ruleDeclarations.addAll(getRuleDefinitions(astNode));
			}
		return ruleDeclarations;
	}
	
	public LinkedList<ASTNode> getRuleDefinitions() {
		return getRuleDefinitions(getRootnode());
	}

	/**
	 * This method returns a hash-map of with weaving candidates and their
	 * advices for all macrocallrules which are not used in blocks inside any
	 * advice.
	 * 
	 * @param astNodes
	 * @return
	 * @throws AspectException
	 */
	private HashMap<ASTNode, LinkedList<AdviceASTNode>> pointCutMatching(HashMap<String, LinkedList<ASTNode>> astNodes)
			throws AspectException {
		HashMap<ASTNode, LinkedList<AdviceASTNode>> weavingCandidates = new HashMap<ASTNode, LinkedList<AdviceASTNode>>();
		for (ASTNode functionRuleTerm : astNodes.get("FunctionRuleTerm")) {
			if (!insideAdviceRule(functionRuleTerm))
				for (ASTNode advice : astNodes
						.get(AdviceASTNode.NODE_TYPE)) {
					// if an advice matches the current astnode
					// it is added to the hashmap of candidates for weaving
					Binding binding = ((AdviceASTNode) advice).matches(functionRuleTerm);
					if (binding.exists()) {
						//create marker	
						AoASMPlugin.createMarker(capi, functionRuleTerm, binding);
						if (weavingCandidates.get(functionRuleTerm) == null) {
							LinkedList<AdviceASTNode> newAdviceList = new LinkedList<AdviceASTNode>();
							newAdviceList.add((AdviceASTNode) advice);
							weavingCandidates.put(functionRuleTerm, newAdviceList);
						}
						else {
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
	 * @param rootnode
	 *            the rootnode to set
	 */
	public void setRootnode(ASTNode rootnode) {
		this.rootnode = rootnode;
	}

	/**
	 * 
	 * @return currently used control api
	 */
	public ControlAPI getControlAPI() {
		return capi;
	}

}
