package org.coreasm.plugins.universalcontrol;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * A node representing the Universal Control Construct
 * @author Michael Stegmaier
 *
 */
@SuppressWarnings("serial")
public class UniversalControlNode extends ASTNode {
	public UniversalControlNode(ScannerInfo info) {
		super(UniversalControlPlugin.PLUGIN_NAME, ASTNode.RULE_CLASS, "UniversalControlRule", null, info);
	}

	public UniversalControlNode(UniversalControlNode node) {
		super(node);
	}

	public Node getRepetitionNode() {
		Node prevChild = null;
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_FOREVER.equals(child.getToken()))
				return null;
			if (UniversalControlPlugin.KEYWORD_ONCE.equals(child.getToken()))
				return child;
			if (UniversalControlPlugin.KEYWORD_TIMES.equals(child.getToken()))
				return prevChild;
			prevChild = child;
		}
		return null;
	}

	public ASTNode getResetCondition() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_RESETTING.equals(child.getToken()))
				return (ASTNode)child.getNextCSTNode().getNextCSTNode();
		}
		return null;
	}
	
	public boolean isStepwise() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_SEQUENTIALY.equals(child.getToken()))
				return false;
			if (UniversalControlPlugin.KEYWORD_PARALLELY.equals(child.getToken()))
				return false;
			if (UniversalControlPlugin.KEYWORD_STEPWISE.equals(child.getToken()))
				return true;
		}
		return false;
	}

	public boolean isSequential() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_SEQUENTIALY.equals(child.getToken()))
				return true;
			if (UniversalControlPlugin.KEYWORD_PARALLELY.equals(child.getToken()))
				return false;
			if (UniversalControlPlugin.KEYWORD_STEPWISE.equals(child.getToken()))
				return false;
		}
		return false;
	}

	public ASTNode getCondition() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_IF.equals(child.getToken()) || UniversalControlPlugin.KEYWORD_WHILE.equals(child.getToken()))
				return (ASTNode)child.getNextCSTNode();
		}
		return null;
	}

	public boolean shouldLoop() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_IF.equals(child.getToken()) || UniversalControlPlugin.KEYWORD_WHILE.equals(child.getToken()))
				return UniversalControlPlugin.KEYWORD_WHILE.equals(child.getToken());
		}
		return false;
	}

	public boolean isVariableSelection() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_VARIABLE.equals(child.getToken()) || UniversalControlPlugin.KEYWORD_FIXED.equals(child.getToken()))
				return UniversalControlPlugin.KEYWORD_VARIABLE.equals(child.getToken());
		}
		return false;
	}

	public String getSelectionKeyword() {
		for (Node child = getFirstCSTNode(); child != null; child = child.getNextCSTNode()) {
			if (UniversalControlPlugin.KEYWORD_WHOLE.equals(child.getToken()) || UniversalControlPlugin.KEYWORD_ANY.equals(child.getToken()) || UniversalControlPlugin.KEYWORD_SINGLE.equals(child.getToken()))
				return child.getToken();
		}
		return UniversalControlPlugin.KEYWORD_WHOLE;
	}

	public ASTNode getRuleBlock() {
		ASTNode ruleBlock = getFirst();
		while (ruleBlock == getCondition() || ruleBlock == getResetCondition())
			ruleBlock = ruleBlock.getNext();
		return ruleBlock;
	}
}
