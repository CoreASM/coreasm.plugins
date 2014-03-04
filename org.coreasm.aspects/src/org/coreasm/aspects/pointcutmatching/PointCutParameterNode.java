package org.coreasm.aspects.pointcutmatching;

import org.coreasm.aspects.AoASMPlugin;
import org.coreasm.aspects.AspectWeaver;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.CoreASMWarning;
import org.coreasm.engine.absstorage.FunctionElement.FunctionClass;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.FunctionRuleTermNode;
import org.coreasm.engine.interpreter.ScannerInfo;
import org.coreasm.engine.plugins.signature.FunctionNode;

public class PointCutParameterNode extends ASTNode{

	public static final String NODE_TYPE = PointCutParameterNode.class.getSimpleName();

	private static final long serialVersionUID = 1L;

	/**
	 * this constructor is needed to support duplicate
	 * @param self this object
	 */
	public PointCutParameterNode(PointCutParameterNode self){
		super(self);
	}

	public PointCutParameterNode(ScannerInfo scannerInfo) {
		super(AoASMPlugin.PLUGIN_NAME, ASTNode.DECLARATION_CLASS, PointCutParameterNode.NODE_TYPE, null, scannerInfo);

	}
	
	public String getPattern(){
		if (this.getFirst().getGrammarRule().equals("ID")){
			ASTNode astNode = this;
			while(! (astNode instanceof AspectASTNode))
				astNode = astNode.getParent();
			while ((astNode = astNode.getNext() ) != null)
				if ( astNode.getGrammarRule().equals("Signature") ){
					FunctionNode fn = (FunctionNode) astNode;
					if ( fn.getName().equals(this.getFirst().getToken()) ){
						if (! ( fn.getRange().equals("STRING") &&
							 fn.getInitNode() != null && fn.getInitNode().getGrammarRule().equals("StringTerm") ))
							throw new CoreASMError("Value of function "+fn.getName()+" is not a string but is used as pointcut pattern.", fn);
						if (fn.getFunctionClass() != FunctionClass.fcStatic) {
							CoreASMWarning warn = new CoreASMWarning(AoASMPlugin.PLUGIN_NAME, "Function "+fn.getName()+" is not static but used as pointcut pattern.", fn);
							AspectWeaver.getInstance().getControlAPI().warning(warn);
						}
						return fn.getInitNode().getToken();
					}		
				}
			throw new CoreASMError(this.getFirst().getToken()+" is not a static string function.", this);
		}
		else
			return this.getFirst().getToken();		
	}
	
	public FunctionRuleTermNode getFuntionRuleTermNode() {
		return (FunctionRuleTermNode)this.getFirst().getNext();
	}
	
	public String getName(){
		FunctionRuleTermNode fnNode = getFuntionRuleTermNode();
		if (fnNode == null)
			return null;
		return fnNode.getFirst().getToken();
	}

}
