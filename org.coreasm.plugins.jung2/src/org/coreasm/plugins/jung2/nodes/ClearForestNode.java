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
public class ClearForestNode extends ASTNode {

	public ClearForestNode(ScannerInfo info) {
		super(Jung2Plugin.getPluginName(), ASTNode.RULE_CLASS, "ClearForestNode", null, info);
	}

	public ClearForestNode(ClearForestNode node) {
		super(node);
	}
}
