/**
 * 
 */
package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.ScannerInfo;

/**
 * @author marcel
 *
 */
public class NamedPointCutASTNode extends PointCutASTNode{
	
	private static final long serialVersionUID = 1L;

	public static final String NODE_TYPE = NamedPointCutASTNode.class.getSimpleName();
	
	public NamedPointCutASTNode(ScannerInfo scannerInfo) {
		super(AopASMPlugin.PLUGIN_NAME, org.coreasm.engine.interpreter.Node.OTHER_NODE, NamedPointCutASTNode.NODE_TYPE, null, scannerInfo);

	}

    public PointCutASTNode getPointCutASTNode(){
        for(ASTNode node: this.getAbstractChildNodes() )
            if ( node instanceof  BinOrASTNode)
                return (BinOrASTNode)node;
        return null;
    }

	@Override
	public PointCutMatchingResult matches(ASTNode compareToNode) throws Exception {
        for(ASTNode node: this.getAbstractChildNodes() )
            if ( node instanceof  BinOrASTNode) {
                return ((BinOrASTNode) node).matches(compareToNode);
            }
        return null;

	}

	@Override
	public String generateExpressionString() {
        for(ASTNode node: this.getAbstractChildNodes() )
            if ( node instanceof  BinOrASTNode)
                return ((BinOrASTNode) node).generateExpressionString();
        return null;
    }


}
