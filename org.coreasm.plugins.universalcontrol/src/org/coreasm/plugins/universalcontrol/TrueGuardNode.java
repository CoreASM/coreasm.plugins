package org.coreasm.plugins.universalcontrol;

import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;
import org.coreasm.engine.interpreter.ScannerInfo;

@SuppressWarnings("serial")
public class TrueGuardNode extends ASTNode {

	public TrueGuardNode(Node parent) {
		super(
				UniversalControlPlugin.PLUGIN_NAME,
				ASTNode.EXPRESSION_CLASS,
				"",
				null,
				ScannerInfo.NO_INFO);
    	parent.addChild(this);
    	this.setParent(parent);
    }

}
