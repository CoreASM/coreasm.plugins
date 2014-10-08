package org.coreasm.plugins.assertion;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

@SuppressWarnings("serial")
public class InvariantNode extends ASTNode {
	public InvariantNode(ScannerInfo info) {
		super(AssertionPlugin.PLUGIN_NAME, ASTNode.DECLARATION_CLASS, "InvariantDeclaration", null, info);
	}
	
	public InvariantNode(AssertNode node) {
		super(node);
	}

	public ASTNode getTerm() {
		return getFirst();
	}
}
