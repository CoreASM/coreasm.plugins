package org.coreasm.plugins.jung2.nodes;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.plugins.jung2.Jung2Plugin;

/**
 * A node representing an vertex add operation on a tree
 * @author Marcel Dasuend
 *
 */
@SuppressWarnings("serial")
public class CollapseVertexNode extends ASTNode {

	public CollapseVertexNode(ScannerInfo info) {
		super(Jung2Plugin.getPluginName(), ASTNode.RULE_CLASS, "FoldVertextNode", null, info);
	}

	public CollapseVertexNode(CollapseVertexNode node) {
		super(node);
	}

	public ASTNode getVertexIdNode() {
		return this.getFirst();
	}
}
