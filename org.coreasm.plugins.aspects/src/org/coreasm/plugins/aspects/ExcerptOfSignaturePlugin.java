/**
/**
 *
 */
package org.coreasm.plugins.aspects;

import org.coreasm.engine.ControlAPI;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.EngineError;
import org.coreasm.engine.absstorage.*;
import org.coreasm.engine.absstorage.FunctionElement.FunctionClass;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.Interpreter;
import org.coreasm.engine.interpreter.InterpreterException;
import org.coreasm.engine.plugins.signature.*;
import org.coreasm.plugins.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.AspectASTNode;

import java.util.*;

/**
 * @author marcel
 * 
 */
public class ExcerptOfSignaturePlugin {

	/**
	 * storage for signatures declared within aspect constructs
	 */
	// @{
	private HashMap<String, FunctionElement> functions;
	private HashMap<String, UniverseElement> universes;
	private HashMap<String, BackgroundElement> backgrounds;
	private HashMap<String, RuleElement> rules;
	// @}

	/** Control API of engine instance this plugin is associated with */
	private ControlAPI capi;

	/**
	 * Sets the Control API of the instance of the engine which this plugin is
	 * associated with.
	 * 
	 * @param capi
	 *            The <code>ControlAPI</code> of this instance of the engine.
	 */
	public void setControlAPI(ControlAPI capi) {
		this.capi = capi;
	}

	/**
	 * this method has been changed in two points: 1) only signatures inside
	 * aspect nodes are considered. 2) rule declarations inside aspect nodes are
	 * taken into account, too.
	 */
	private void processSignatures() {
		// Don't do anything if the spec is not parsed yet
		if (capi.getSpec().getRootNode() == null)
			return;

		// processingSignatures = true;
		if (functions == null) {
			functions = new HashMap<String, FunctionElement>();
			// functionClass = new HashMap<String,FunctionClass>();
		}
		if (universes == null) {
			universes = new HashMap<String, UniverseElement>();
		}
		if (backgrounds == null) {
			backgrounds = new HashMap<String, BackgroundElement>();
		}
		if (rules == null) {
			rules = new HashMap<String, RuleElement>();
		}

		ASTNode node = capi.getParser().getRootNode().getFirst();

		Interpreter interpreter = capi.getInterpreter()
				.getInterpreterInstance();

		while (node != null) {
			if (node instanceof AspectASTNode) {

				for (ASTNode childOfAspectNode : node.getAbstractChildNodes()) {

					if (childOfAspectNode.getGrammarRule().equals("Signature")) {
						ASTNode currentSignature = childOfAspectNode.getFirst();

						while (currentSignature != null) {
							if (currentSignature instanceof EnumerationNode) {
								createEnumeration(currentSignature, interpreter);
							} else if (currentSignature instanceof FunctionNode) {
								createFunction(currentSignature, interpreter);
							} else if (currentSignature instanceof UniverseNode) {
								createUniverse(currentSignature, interpreter);
							} else if (currentSignature instanceof DerivedFunctionNode) {
								createDerivedFunction(currentSignature,
										interpreter);
							}
							currentSignature = currentSignature.getNext();
						}
					}// extend the signature processing by rule declarations
					else if (childOfAspectNode.getGrammarRule().equals(
							"RuleDeclaration")) {
						createRuleElement(childOfAspectNode, interpreter);
					} else if (childOfAspectNode instanceof AdviceASTNode) {
						AdviceASTNode adviceNode = (AdviceASTNode) childOfAspectNode;
						// set Token! getToken is overwritten so that the
						// original token can be received using the method
						// getOriginalToken
						createRuleElement(adviceNode, interpreter);
					}

				}
			}
			node = node.getNext();
		}
	}

	/**
	 * create an enumeration from the given signature
	 * 
	 * @param currentSignature
	 * @param interpreter
	 */
	private void createEnumeration(ASTNode currentSignature,
			Interpreter interpreter) {
		EnumerationNode enumerationNode = (EnumerationNode) currentSignature;
		List<EnumerationElement> members = enumerationNode.getMembers();
		String enumName = enumerationNode.getName();
		EnumerationBackgroundElement background = new EnumerationBackgroundElement(
				members);

		for (EnumerationElement e : members) {
			MapFunction f = new MapFunction();
			try {
				f.setValue(ElementList.NO_ARGUMENT, e);
			} catch (UnmodifiableFunctionException e1) {
				capi.error("Cannot modify unmodifiable function.",
						enumerationNode, interpreter);
			}
			f.setFClass(FunctionClass.fcStatic);
			addFunction(e.getName(), f, enumerationNode, interpreter);
			// e.setBackground(enumName); \todo Funktionieren Enums?!
			// setBackground ist private!!
		}

		addBackground(enumName, background, enumerationNode, interpreter);
	}

	/**
	 * create a function from the given signature, add it to the interpreters's
	 * function, and initialize the function with its initial value if existing
	 * 
	 * @param currentSignature
	 * @param interpreter
	 */
	private void createFunction(ASTNode currentSignature,
			Interpreter interpreter) {
		FunctionNode functionNode = (FunctionNode) currentSignature;
		MapFunction function = null;

		if (functionNode.getName()
				.equals(AbstractStorage.PROGRAM_FUNCTION_NAME)) {
			/**
			 * \todo (from SignaturePlugin): check signature for correct
			 * signature of program function
			 */
			function = (MapFunction) capi.getStorage().getFunction(
					AbstractStorage.PROGRAM_FUNCTION_NAME);
		} else {
			function = new MapFunction();
		}

		Signature signature = new Signature();
		signature.setDomain(functionNode.getDomain());
		signature.setRange(functionNode.getRange());
		function.setSignature(signature);

		if (!functionNode.getName().equals(
				AbstractStorage.PROGRAM_FUNCTION_NAME)) {
			addFunction(functionNode.getName(), function, functionNode,
					interpreter);
		}

		if (functionNode.getInitNode() != null) {
			try {
				interpreter.interpret(functionNode.getInitNode(),
						interpreter.getSelf());
			} catch (InterpreterException e) {
				e.printStackTrace();
			}

			Element initValue = functionNode.getInitNode().getValue();

			if (functionNode.getDomain().size() == 0) {
				try {
					function.setValue(ElementList.NO_ARGUMENT, initValue);
				} catch (UnmodifiableFunctionException e) {
					throw new EngineError(
							"Cannot set initial value for unmodifiable function "
									+ functionNode.getName());
				}
			} else {
				if (initValue instanceof FunctionElement) {
					FunctionElement map = (FunctionElement) initValue;
					int dSize = functionNode.getDomain().size();
					for (Location l : map.getLocations(functionNode.getName())) {
						if (l.args.size() == dSize) {
							try {
								function.setValue(l.args, map.getValue(l.args));
							} catch (UnmodifiableFunctionException e) {
								throw new EngineError(
										"Cannot set initial value for unmodifiable function "
												+ functionNode.getName());
							}
						} else {
							if (l.args.size() == 1
									&& l.args.get(0) instanceof Enumerable
									&& ((Enumerable) l.args.get(0)).enumerate()
											.size() == dSize) {
								try {
									function.setValue(
											ElementList
													.create(((Enumerable) l.args
															.get(0))
															.enumerate()), map
													.getValue(l.args));
								} catch (UnmodifiableFunctionException e) {
									throw new EngineError(
											"Cannot set initial value for unmodifiable function "
													+ functionNode.getName());
								}
							} else
								throw new EngineError(
										"Initial value of function "
												+ functionNode.getName()
												+ " does not match the function signature.");
						}

					}
				}
			}
		}

		function.setFClass(functionNode.getFunctionClass());
	}

	/**
	 * create a universe from the given signature and add it to the
	 * interpreter's functions
	 * 
	 * @param currentSignature
	 * @param interpreter
	 */
	private void createUniverse(ASTNode currentSignature,
			Interpreter interpreter) {
		UniverseNode universeNode = (UniverseNode) currentSignature;
		UniverseElement u = new UniverseElement();

		addUniverse(universeNode.getName(), u, currentSignature, interpreter);

		ASTNode member = universeNode.getFirst().getNext();

		while (member != null) {
			Element e = new EnumerationElement(member.getToken());
			u.member(e, true);
			MapFunction f = new MapFunction();

			try {
				f.setValue(ElementList.NO_ARGUMENT, e);
			} catch (UnmodifiableFunctionException e1) {
				capi.error("Cannot modify unmodifiable function.",
						universeNode, interpreter);
			}

			f.setFClass(FunctionClass.fcStatic);
			addFunction(member.getToken(), f, universeNode, interpreter);
			member = member.getNext();
		}
	}

	/**
	 * create a rule from the given rule declaration and add it to the
	 * interpreter's rules
	 * 
	 * @param currentRuleDeclaration
	 * @param interpreter
	 */
	void createRuleElement(ASTNode currentRuleDeclaration,
			Interpreter interpreter) {
		// get name (ID) node of rule
		final ASTNode idNode;
		final String ruleName;

		// The a generic rule name is used instead of the user defined one.
		idNode = currentRuleDeclaration.getFirst().getFirst();
		if (idNode != null) {
			ruleName = idNode.getToken();

			// create structure for all parameters
			ArrayList<String> params = new ArrayList<String>();
			// an advice cannot have parameters so that idNode is set to 'null'
			// above

			ASTNode currentParams = idNode.getNext();
			// while there are parameters to add to the list
			while (currentParams != null) {
				// add parameters to the list
				if (currentParams instanceof FunctionRuleTermNode)
					params.add(currentParams.getFirst().getToken());
				else
					params.add(currentParams.getToken());

				// get next parameter
				currentParams = currentParams.getNext();
			}

			// get root node of rule body
			ASTNode bodyNode;
			// in case of advice the body node is the last node of the advice
			if (currentRuleDeclaration instanceof AdviceASTNode) {
				bodyNode = currentRuleDeclaration.getFirst();
				while (!bodyNode.getGrammarRule().equals("BlockRule"))
					bodyNode = bodyNode.getNext();
			} else {
				bodyNode = currentRuleDeclaration.getFirst().getNext();
			}
			// create a copy of the body
			bodyNode = (ASTNode) capi.getInterpreter().copyTree(bodyNode);

			// add rule element
			RuleElement newRuleElement = new RuleElement(
					currentRuleDeclaration, ruleName, params, bodyNode);
			addRule(ruleName, newRuleElement, currentRuleDeclaration,
					interpreter);
		} else {
			throw new CoreASMError("Rule for the ASTNode "
					+ currentRuleDeclaration.toString()
					+ " could not be created!", currentRuleDeclaration);
		}
	}

	/**
	 * create a derived function from the given signature and add it to the
	 * interpreter's functions
	 * 
	 * @param currentSignature
	 * @param interpreter
	 */
	private void createDerivedFunction(ASTNode currentSignature,
			Interpreter interpreter) {
		DerivedFunctionNode derivedFuncNode = (DerivedFunctionNode) currentSignature;

		ASTNode exprNode = derivedFuncNode.getExpressionNode();
		ASTNode idNode = derivedFuncNode.getNameSignatureNode().getFirst();

		// create structure for all parameters
		ArrayList<String> params = new ArrayList<String>();
		ASTNode currentParams = idNode.getNext();
		// while there are parameters to add to the list
		while (currentParams != null) {
			// add parameters to the list
			params.add(currentParams.getToken());
			// get next parameter
			currentParams = currentParams.getNext();
		}

		DerivedFunctionElement func = new DerivedFunctionElement(capi, params,
				exprNode);

		addFunction(idNode.getToken(), func, currentSignature, interpreter);
	}

	/**
	 * add an entry to the field of universes
	 * 
	 * @param name
	 * @param universe
	 * @param node
	 * @param interpreter
	 */
	private void addUniverse(String name, UniverseElement universe,
			ASTNode node, Interpreter interpreter) {
		if (checkNameUniqueness(name, "universe", node))
			universes.put(name, universe);
	}

	/**
	 * add an entry to the field of backgrounds
	 * 
	 * @param name
	 * @param background
	 * @param node
	 * @param interpreter
	 */
	private void addBackground(String name, BackgroundElement background,
			ASTNode node, Interpreter interpreter) {
		if (checkNameUniqueness(name, "background", node))
			backgrounds.put(name, background);
	}

	/**
	 * add an entry to the field of functions
	 * 
	 * @param name
	 * @param function
	 * @param node
	 * @param interpreter
	 */
	private void addFunction(String name, FunctionElement function,
			ASTNode node, Interpreter interpreter) {
		if (checkNameUniqueness(name, "function", node))
			functions.put(name, function);
	}

	/**
	 * add an entry to the field of rules
	 * 
	 * @param name
	 * @param rule
	 * @param node
	 * @param interpreter
	 */
	private void addRule(String name, RuleElement rule, ASTNode node,
			Interpreter interpreter) {
		if (checkNameUniqueness(name, "derived function", node))
			rules.put(name, rule);
	}

	/**
	 * check if the give name is unique and not contained in any of the checked
	 * fields
	 * 
	 * @param name
	 *            name to be checked if it is globally unique
	 * @param type
	 *            given type of the name
	 * @param node
	 *            given node
	 * @return returns true if the name is unique, else a new exception is
	 *         thrown
	 */
	private boolean checkNameUniqueness(String name, String type, ASTNode node) {
		boolean result = true;
		if (rules.containsKey(name)) {
			throw new CoreASMError("Cannot add " + type + " '" + name + "'."
					+ " A derived function with the same name already exists.",
					node);
		}
		if (functions.containsKey(name)) {
			throw new CoreASMError("Cannot add " + type + " '" + name + "'."
					+ " A function with the same name already exists.", node);
		}
		if (universes.containsKey(name)) {
			throw new CoreASMError("Cannot add " + type + " '" + name + "'."
					+ " A universe with the same name already exists.", node);
		}
		if (backgrounds.containsKey(name)) {
			throw new CoreASMError("Cannot add " + type + " '" + name + "'."
					+ " A background with the same name already exists.", node);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getFunctions()
	 */
	public Map<String, FunctionElement> getFunctions() {
		if (functions == null) {
			processSignatures();
		}
		return functions;
	}

	/**
	 * returns an empty set of rule names
	 * @return empty set
	 */
	public Set<String> getRuleNames() {
		return Collections.emptySet();
	}

	/**
	 * returns a map of rule elements returned by processSignatures()
	 * @return map of rules
	 */
	public Map<String, RuleElement> getRules() {
		if (rules == null)
			processSignatures();
		return rules;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getUniverses()
	 */
	public Map<String, UniverseElement> getUniverses() {
		if (universes == null) {
			processSignatures();
		}
		return universes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.coreasm.engine.plugin.VocabularyExtender#getBackgrounds()
	 */
	public Map<String, BackgroundElement> getBackgrounds() {
		if (backgrounds == null) {
			processSignatures();
		}
		return backgrounds;
	}

	/**
	 * returns a set of background names
	 * @return background names
	 */
	public Set<String> getBackgroundNames() {
		return getBackgrounds().keySet();
	}

	/**
	 * returns a set of function names
	 * @return function names
	 */
	public Set<String> getFunctionNames() {
		return getFunctions().keySet();
	}

	/**
	 * returns a set of universe names
	 * @return universe names
	 */
	public Set<String> getUniverseNames() {
		return getUniverses().keySet();
	}

}
