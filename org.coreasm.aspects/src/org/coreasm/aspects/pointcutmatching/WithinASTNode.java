/**
 *
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.aspects.errorhandling.MatchingError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

import java.util.LinkedList;
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
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, WithinASTNode.NODE_TYPE, null, scannerInfo);
	}


	@Override
	public PointCutMatchingResult matches(ASTNode compareToNode) throws MatchingError {

		// do not consider macro call rule within advice blocks (first if)
		if (compareToNode.getParent().getParent() instanceof AdviceASTNode)
			return new PointCutMatchingResult(false, new LinkedList<ArgsASTNode>());

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
		return new PointCutMatchingResult(
				rulename.matches(pointCutToken),
				new LinkedList<ArgsASTNode>()
				);
	}

	@Override
	public String generateExpressionString() {
		// static condition which has been already checked
		return "true";
	}

}
