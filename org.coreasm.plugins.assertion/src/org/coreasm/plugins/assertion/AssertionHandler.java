package org.coreasm.plugins.assertion;

import org.coreasm.compiler.CodeType;
import org.coreasm.compiler.CompilerEngine;
import org.coreasm.compiler.codefragment.CodeFragment;
import org.coreasm.compiler.exception.CompilerException;
import org.coreasm.compiler.interfaces.CompilerCodeHandler;
import org.coreasm.engine.interpreter.ASTNode;

public class AssertionHandler implements CompilerCodeHandler {

	@Override
	public void compile(CodeFragment result, ASTNode node, CompilerEngine engine)
			throws CompilerException {
		CodeFragment term = engine.compile(node.getFirst(), CodeType.R);
		
		ASTNode msgNode = node.getFirst().getNext();
		CodeFragment message = null;
		if(msgNode == null){
			message = new CodeFragment("throw new Exception(\"Assertion " + node.getFirst().unparseTree() + " failed\");\n");
		}
		else{
			message = new CodeFragment("");
			message.appendFragment(engine.compile(msgNode, CodeType.R));
			message.appendLine("@decl(String,msg)=((plugins.StringPlugin.StringElement) evalStack.pop()).toString();\n");
			message.appendLine("throw new Exception(\"Assertion " + node.getFirst().unparseTree() + " failed: \" + @msg@);\n");
		}
		
		//first, evaluate the bool
		result.appendFragment(term);
		result.appendLine("@decl(CompilerRuntime.BooleanElement,check)=(CompilerRuntime.BooleanElement)evalStack.pop();\n");
		result.appendLine("if(!@check@.getValue()){\n");
		result.appendFragment(message);
		result.appendLine("}\n");
		result.appendLine("evalStack.push(new CompilerRuntime.UpdateList());\n");
	}

}
