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
public class VertexNode extends ASTNode {

	public VertexNode(ScannerInfo info) {
		super(Jung2Plugin.getPluginName(), ASTNode.RULE_CLASS, "VertexNode", null, info);
	}

	public VertexNode(VertexNode node) {
		super(node);
	}

	public String getParentId() {
		return this.getFirst().getToken();

	}

	public String getChildId(){
		return this.getFirst().getNext().getToken();
	}

	public String getTooltip() {
		return this.getFirst().getNext().getNext().getToken();
	}
}
