package org.coreasm.plugins.assertion;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * A node representing the declaration of an invariant
 * @author Michael Stegmaier
 *
 */
@SuppressWarnings("serial")
public class InvariantNode extends ASTNode {
	public InvariantNode(ScannerInfo info) {
		super(AssertionPlugin.PLUGIN_NAME, ASTNode.DECLARATION_CLASS, "InvariantDeclaration", null, info);
	}
	
	public InvariantNode(InvariantNode node) {
		super(node);
	}

	public ASTNode getTerm() {
		return getFirst();
	}

	public ASTNode getMessageTerm() {
		return getTerm().getNext();
	}
}
