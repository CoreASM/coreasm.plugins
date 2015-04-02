package org.coreasm.plugins.jung2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Parsers;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.VersionInfo;
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
import org.coreasm.engine.plugins.string.StringElement;
import org.coreasm.plugins.jung2.nodes.ClearForestNode;
import org.coreasm.plugins.jung2.nodes.EdgeNode;
import org.coreasm.plugins.jung2.nodes.CollapseVertexNode;
import org.coreasm.plugins.jung2.nodes.ExpandVertexNode;
import org.coreasm.plugins.jung2.nodes.RemoveEdgeNode;
import org.coreasm.plugins.jung2.nodes.RemoveVertexNode;
import org.coreasm.plugins.jung2.nodes.VertexNode;



public class Jung2Plugin extends Plugin implements ParserPlugin, InterpreterPlugin {

	private static final String PLUGIN_NAME = Jung2Plugin.class.getSimpleName();
	static final VersionInfo VERSION_INFO = new VersionInfo(0, 0, 2, "alpha");

	public static final String KEYWORD_VERTEX= "vertex";
	public static final String KEYWORD_EDGE = "edge";
	public static final String KEYWORD_REMOVE = "remove";
	public static final String KEYWORD_DESCENDANTS = "descendants";
	public static final String KEYWORD_CLEAR = "clear";
	public static final String KEYWORD_FOREST = "forest";
	public static final String KEYWORD_COLLAPSE = "collapse";
	public static final String KEYWORD_EXPAND = "expand";
	public static final String KEYWORD_TOOLTIP = "tooltip";




	public static final String OPERATOR_PARENT_TO_CHILD = "->";

	private static final String[] KEYWORDS = new String[] { KEYWORD_VERTEX, KEYWORD_EDGE, KEYWORD_REMOVE,
			KEYWORD_DESCENDANTS, KEYWORD_CLEAR, KEYWORD_FOREST, KEYWORD_COLLAPSE, KEYWORD_EXPAND, KEYWORD_TOOLTIP };
	private static final String[] OPERATORS = new String[] { OPERATOR_PARENT_TO_CHILD };

	private HashMap<String, GrammarRule> parsers;

	private GraphControler controller;
	
	public VersionInfo getVersionInfo() {
		return VERSION_INFO;
	}
	
	public static String getPluginName() {
		return PLUGIN_NAME;
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
			ParserTools pTools = ParserTools.getInstance(capi);

			KernelServices kernel = (KernelServices)capi.getPlugin("Kernel").getPluginInterface();
			Parser<Node> termParser = kernel.getTermParser();

			Parser<Node> createVertex = Parsers.array(
				pTools.getKeywParser(KEYWORD_VERTEX, getPluginName()),
				termParser,
				termParser,
					Parsers.array(
							pTools.getKeywParser(KEYWORD_TOOLTIP, getPluginName()),
							termParser).optional()
			).map(
			new ArrayParseMap(getPluginName()) {
				public Node map(Object[] vals) {
					Node node = new VertexNode(((Node)vals[0]).getScannerInfo());
					addChildren(node, vals);
					return node;
				}
			});
			parsers.put("VertexRule", new GrammarRule("VertexNode",
					"'vertex' StringTerm StringTerm (tooltip StringTerm)?", createVertex,
					getPluginName()));

			Parser<Node> createEdge = Parsers.array(
					pTools.getKeywParser(KEYWORD_EDGE, getPluginName()),
					termParser,
					pTools.getOprParser(OPERATOR_PARENT_TO_CHILD),
					termParser,
					Parsers.array(
							pTools.getKeywParser(KEYWORD_TOOLTIP, getPluginName()),
							termParser).optional()
				).map(
				new ArrayParseMap(getPluginName()) {
					public Node map(Object[] vals) {
						Node node = new EdgeNode(((Node)vals[0]).getScannerInfo());
						addChildren(node, vals);
						return node;
					}
				});
			parsers.put("EdgeRule", new GrammarRule("EdgeNode",
					"'edge' StringTerm '->' StringTerm (tooltip StringTerm)?", createEdge, getPluginName()));

				Parser<Node> removeVertex = Parsers.array(
						pTools.getKeywParser(KEYWORD_REMOVE, getPluginName()),
						pTools.getKeywParser(KEYWORD_VERTEX, getPluginName()),
						pTools.getKeywParser(KEYWORD_DESCENDANTS, getPluginName()).optional(),
						termParser
					).map(
					new ArrayParseMap(getPluginName()) {
						public Node map(Object[] vals) {
							Node node = new RemoveVertexNode(((Node)vals[0]).getScannerInfo());
							addChildren(node, vals);
							return node;
						}
					});
					parsers.put("RemoveVertexRule", new GrammarRule("RemoveVertexNode", "'remove' 'vertex' 'descendants'? StringTerm '->' StringTerm", removeVertex, getPluginName()));

					Parser<Node> removeEdge = Parsers.array(
							pTools.getKeywParser(KEYWORD_REMOVE, getPluginName()),
							pTools.getKeywParser(KEYWORD_EDGE, getPluginName()),
							pTools.getKeywParser(KEYWORD_DESCENDANTS, getPluginName()).optional(),
							termParser,
							pTools.getOprParser(OPERATOR_PARENT_TO_CHILD),
							termParser
						).map(
						new ArrayParseMap(getPluginName()) {
							public Node map(Object[] vals) {
								Node node = new RemoveEdgeNode(((Node)vals[0]).getScannerInfo());
								addChildren(node, vals);
								return node;
							}
						});
						parsers.put("RemoveEdgeNode", new GrammarRule("RemoveEdgeNode", "'remove' 'edge''descendants'? StringTerm '->' StringTerm", removeEdge, getPluginName()));

						Parser<Node> clearForest = Parsers.array(
								pTools.getKeywParser(KEYWORD_CLEAR, getPluginName()),
								pTools.getKeywParser(KEYWORD_FOREST, getPluginName())
							).map(
							new ArrayParseMap(getPluginName()) {
								public Node map(Object[] vals) {
									Node node = new ClearForestNode(((Node)vals[0]).getScannerInfo());
									addChildren(node, vals);
									return node;
								}
							});
							parsers.put("ClearForestNode", new GrammarRule("ClearForestNode", "'clear' 'forest'", clearForest, getPluginName()));

			Parser<Node> collapseVertex = Parsers.array(
					pTools.getKeywParser(KEYWORD_COLLAPSE, getPluginName()),
					pTools.getKeywParser(KEYWORD_VERTEX, getPluginName()),
					termParser
					).map(
							new ArrayParseMap(getPluginName()) {
								public Node map(Object[] vals) {
									Node node = new CollapseVertexNode(((Node) vals[0]).getScannerInfo());
									addChildren(node, vals);
									return node;
								}
							});
			parsers.put("CollapseVertexRule", new GrammarRule("CollapseVertexRule", "'collapse' 'vertex' StringTerm",
					collapseVertex,
					getPluginName()));

			Parser<Node> expandVertex = Parsers.array(
					pTools.getKeywParser(KEYWORD_EXPAND, getPluginName()),
					pTools.getKeywParser(KEYWORD_VERTEX, getPluginName()),
					termParser
					).map(
							new ArrayParseMap(getPluginName()) {
								public Node map(Object[] vals) {
									Node node = new ExpandVertexNode(((Node) vals[0]).getScannerInfo());
									addChildren(node, vals);
									return node;
								}
							});
			parsers.put("ExpandVertexRule", new GrammarRule("ExpandVertexRule", "'expand' 'vertex' StringTerm",
					expandVertex,
					getPluginName()));

			Parser<Node> overallPluginParser = Parsers.or(createVertex, createEdge, removeVertex, removeEdge,
					clearForest, collapseVertex, expandVertex);
			parsers.put(
					"Rule",
					new GrammarRule(
							"VertexOrEdge",
							"VertexNode | EdgeNode | RemoveVertexRule | RemoveEdgeRule | CollapseVertexRule | ExpandVertexRule",
							overallPluginParser, getPluginName()));
		}
		return parsers;
	}

	@Override
	public ASTNode interpret(Interpreter interpreter, ASTNode pos)
			throws InterpreterException {
		if (controller == null)
			controller = new GraphControler();

		if (pos instanceof VertexNode) {
			StringElement id, label, tooltip = null;
			VertexNode vertex = (VertexNode)pos;
			if (!vertex.getFirst().isEvaluated())
				return vertex.getFirst();
			if (!(vertex.getFirst().getValue() instanceof StringElement))
				throw new CoreASMError("The first value of the create vertex operation must be a StringElement but was " + vertex.getFirst().getValue() + ".", vertex.getFirst());
			id = (StringElement)vertex.getFirst().getValue();
			if (!vertex.getFirst().getNextASTNode().isEvaluated())
				return vertex.getFirst().getNextASTNode();
			if (!(vertex.getFirst().getNextASTNode().getValue() instanceof StringElement))
				throw new CoreASMError("The second value of the create vertex operation must be a StringElement but was " + vertex.getFirst().getNext().getValue() + ".", vertex.getFirst().getNext());
			label = (StringElement)vertex.getFirst().getNext().getValue();
			ASTNode tooltipNode = vertex.getFirst().getNext().getNext();
			if (tooltipNode != null) {
				if (!(tooltipNode.isEvaluated()))
					return tooltipNode;
				if (!(tooltipNode.getValue() instanceof StringElement))
					throw new CoreASMError("The tooltip value must be a StringElement but was " +
							tooltipNode.getValue() +
							".", vertex.getFirst().getNext());
				tooltip = (StringElement) tooltipNode.getValue();
				controller.getModel().addVertex(id.getValue(), label.getValue(), tooltip.getValue());
			}else {
				controller.getModel().addVertex(id.getValue(), label.getValue());
			}
			capi.getInterpreter().getInterpreterInstance().clearTree(vertex);
		}else if(pos instanceof EdgeNode){
			StringElement parentId, childId = null;
			EdgeNode edge = (EdgeNode)pos;
			if (!edge.getFirst().isEvaluated())
				return edge.getFirst();
			if (!(edge.getFirst().getValue() instanceof StringElement))
				throw new CoreASMError("The first value of the create edge operation must be a StringElement but was " + edge.getFirst().getValue() + ".", edge.getFirst());
			parentId = (StringElement)edge.getFirst().getValue();
			if (!edge.getFirst().getNextASTNode().isEvaluated())
				return edge.getFirst().getNextASTNode();
			if (!(edge.getFirst().getNextASTNode().getValue() instanceof StringElement))
				throw new CoreASMError("The second value of the create edge operation must be a StringElement but was " + edge.getFirst().getNext().getValue() + ".", edge.getFirst().getNext());
			childId = (StringElement)edge.getFirst().getNext().getValue();
			controller.getModel().addEdge(parentId.getValue(), childId.getValue());
			capi.getInterpreter().getInterpreterInstance().clearTree(edge);
		}else if(pos instanceof RemoveVertexNode){
			StringElement id = null;
			RemoveVertexNode vertex = (RemoveVertexNode)pos;
			if (!vertex.getFirst().isEvaluated())
				return vertex.getFirst();
			if (!(vertex.getFirst().getValue() instanceof StringElement))
				throw new CoreASMError("The value of the remove vertex operation must be a StringElement but was " + vertex.getFirst().getValue() + ".", vertex.getFirst());
			id = (StringElement)vertex.getFirst().getValue();
			controller.getModel().removeVertex(id.getValue(), vertex.descendants());
			capi.getInterpreter().getInterpreterInstance().clearTree(vertex);
		}else if(pos instanceof RemoveEdgeNode){
			StringElement parentId, childId = null;
			RemoveEdgeNode edge = (RemoveEdgeNode)pos;
			if (!edge.getFirst().isEvaluated())
				return edge.getFirst();
			if (!(edge.getFirst().getValue() instanceof StringElement))
				throw new CoreASMError("The first value of the create edge operation must be a StringElement but was " + edge.getFirst().getValue() + ".", edge.getFirst());
			parentId = (StringElement)edge.getFirst().getValue();
			if (!edge.getFirst().getNextASTNode().isEvaluated())
				return edge.getFirst().getNextASTNode();
			if (!(edge.getFirst().getNextASTNode().getValue() instanceof StringElement))
				throw new CoreASMError("The second value of the create edge operation must be a StringElement but was " + edge.getFirst().getNext().getValue() + ".", edge.getFirst().getNext());
			childId = (StringElement)edge.getFirst().getNext().getValue();
			controller.getModel().removeEdge(parentId.getValue(), childId.getValue(), edge.descendants());
			capi.getInterpreter().getInterpreterInstance().clearTree(edge);
		}else if(pos instanceof ClearForestNode){
			ClearForestNode cf = (ClearForestNode)pos;
			controller.getModel().clearForest();
			capi.getInterpreter().getInterpreterInstance().clearTree(cf);
		}
		else if (pos instanceof CollapseVertexNode) {
			CollapseVertexNode fvn = (CollapseVertexNode) pos;
			if (!fvn.getVertexIdNode().isEvaluated())
				return fvn.getVertexIdNode();
			if (!(fvn.getVertexIdNode().getValue() instanceof StringElement))
				throw new CoreASMError("The value of the collapse vertex operation must be a StringElement but was " +
						fvn.getVertexIdNode().getValue() + ".", fvn.getVertexIdNode());
			String vertexId = ((StringElement) fvn.getVertexIdNode().getValue()).getValue();
			Vertex vertex = controller.getModel().getVertex(vertexId);
			if (vertex != null)
				controller.getModel().getViewer().foldVertex(vertex);
			capi.getInterpreter().getInterpreterInstance().clearTree(fvn);

		}
		else if (pos instanceof ExpandVertexNode) {
			ExpandVertexNode fvn = (ExpandVertexNode) pos;
			if (!fvn.getVertexIdNode().isEvaluated())
				return fvn.getVertexIdNode();
			if (!(fvn.getVertexIdNode().getValue() instanceof StringElement))
				throw new CoreASMError("The value of the expand vertex operation must be a StringElement but was " +
						fvn.getVertexIdNode().getValue() + ".", fvn.getVertexIdNode());
			String vertexId = ((StringElement) fvn.getVertexIdNode().getValue()).getValue();
			Vertex vertex = controller.getModel().getVertex(vertexId);
			if (vertex != null)
				controller.getModel().getViewer().expandVertex(vertex);
			capi.getInterpreter().getInterpreterInstance().clearTree(fvn);
		}
		pos.setNode(null, new UpdateMultiset(), null);
		return pos;
	}

}
