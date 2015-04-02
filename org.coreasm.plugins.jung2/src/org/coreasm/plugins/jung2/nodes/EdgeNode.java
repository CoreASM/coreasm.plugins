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
public class EdgeNode extends ASTNode {

	public EdgeNode(ScannerInfo info) {
		super(Jung2Plugin.getPluginName(), ASTNode.RULE_CLASS, "EdgeNode", null, info);
	}

	public EdgeNode(EdgeNode node) {
		super(node);
	}

	public String getLabel() {
		return this.getFirst().getNext().getToken();
	}

	public String getId(){
		return this.getFirst().getToken();
	}

	public String getTooltip() {
		return this.getFirst().getNext().getNext().getToken();
	}
}
