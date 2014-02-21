/**
 *
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Marcel Dausend
 *
 */
public class WithinASTNode extends PointCutASTNode {

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = WithinASTNode.class.getSimpleName();

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public WithinASTNode(WithinASTNode self){
		super(self);
	}

	/**
	 *
	 * @param scannerInfo
	 */
	public WithinASTNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, WithinASTNode.NODE_TYPE, null, scannerInfo);
	}


	@Override
	public Binding matches(ASTNode compareToNode) throws MatchingError {

		// do not consider macro call rule within advice blocks (first if)
		if (compareToNode.getParent().getParent() instanceof AdviceASTNode)
			return new Binding(compareToNode, this);

		// compareToNode has to be directly defined in a rule
		// which name matches the pointCutToken
		ASTNode node = compareToNode;
		//step up until node points to the declaration
		while(!(node.getGrammarRule().equals("RuleDeclaration"))){
			node=node.getParent();
		}
		//the node is a rule declaration with the following name
		String rulename = node.getFirst().getFirst().getToken();
		String pointCutToken = this.getFirstASTNode().getToken();

		//throw an Exception if the pattern syntax is irregular
		try {
			Pattern.compile(pointCutToken);
		}catch (PatternSyntaxException e){
			throw new MatchingError(pointCutToken, this, e.getMessage(), e.getCause());
		}
        if ( rulename.matches(pointCutToken) )
            return new Binding(compareToNode, this, new HashMap<String, ASTNode>());
        else
            return new Binding(compareToNode, this);
	}

	@Override
	public String generateExpressionString() {
		// static condition which has been already checked
		return "true";
	}

}
