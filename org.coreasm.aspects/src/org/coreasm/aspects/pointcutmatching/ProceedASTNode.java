/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import java.util.LinkedList;
import java.util.List;

import org.coreasm.aspects.AspectTools;
import org.coreasm.aspects.errorhandling.ProceedError;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.kernel.MacroCallRuleNode;

/**
 * @author marcel
 *
 */
public class ProceedASTNode extends MacroCallRuleNode{

	private static final long serialVersionUID = 1L;
	private static final String NODE_TYPE = ProceedASTNode.class.getSimpleName();	
	
	/**
	 * needed for duplicate
	 * 
	 * @param self
	 */
	public ProceedASTNode(ProceedASTNode self) {
		super(self);
	}
	
	/**
	 * contructor used to create a new ProocedASTNode from a given cloneTree of a proceed FunctionRuleTermNode contained in an advice's body.
	 * @param proceedNode
	 */
	public ProceedASTNode(MacroCallRuleNode proceedNode) {
		super(proceedNode);
	}
	
	public ProceedASTNode(FunctionRuleTermNode proceedNode) {
		super(proceedNode.getScannerInfo());
		AspectTools.addChild(this, proceedNode);
	}
	
	public ProceedASTNode(ScannerInfo scannerInfo){
		super(scannerInfo);
	}

	public List<ASTNode> getArguments(){
		return ((FunctionRuleTermNode)this.getFirstASTNode()).getArguments();
	}
	
	public String getToken(){
		return ((FunctionRuleTermNode)this.getFirstASTNode()).getFirstASTNode().getToken();
	}

	@SuppressWarnings("unchecked")
	public void checkProceedCondition() throws ProceedError {
		AdviceASTNode adv = AspectTools.getParentOfType(this, AdviceASTNode.class);
		List<ProceedASTNode> children = new LinkedList<ProceedASTNode>();
		if (adv == null)
			throw new ProceedError(this, "the keyword prooced is only allowed in advice blocks", null, null, null);
		else{
			children = (List<ProceedASTNode>) AspectTools.getChildrenOfType(adv, this.getClass());
			if (children.size() > 1)
				throw new ProceedError(adv, "Only one keyword prooced is allowed in the advice block "+adv.getLocator(), null, null, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.coreasm.aspects.PointCutASTNode#generateExpressionString()
	 */
	public String generateExpressionString() {
		// TODO Auto-generated method stub
		return null;
	}

}
