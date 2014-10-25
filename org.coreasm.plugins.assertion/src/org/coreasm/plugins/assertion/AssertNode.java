package org.coreasm.plugins.assertion;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * A node representing an assertion rule
 * @author Michael Stegmaier
 *
 */
@SuppressWarnings("serial")
public class AssertNode extends ASTNode {
	public AssertNode(ScannerInfo info) {
		super(AssertionPlugin.PLUGIN_NAME, ASTNode.RULE_CLASS, "AssertRule", null, info);
	}

	public AssertNode(AssertNode node) {
		super(node);
	}

	public ASTNode getTerm() {
		return getFirst();
	}

	public ASTNode getMessageTerm() {
		return getTerm().getNext();
	}
}
