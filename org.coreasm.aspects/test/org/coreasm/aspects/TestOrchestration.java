package org.coreasm.aspects;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import junit.framework.Assert;

import org.junit.Test;

import org.coreasm.aspects.utils.AspectTools;
import org.coreasm.aspects.utils.TestEngineDriver;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.interpreter.Node;

public class TestOrchestration {

	TestEngineDriver td = null;

	@Test
	public void TestOrchestration() {

		String MATCHING_RULE_INSIDE_CALLSTACK = "matchingRuleCallsInsideCallstack";
		String MATCHING_SIGNATURE_INSIDE_CALLSTACK = "matchingParameterSignatureInsideCallstack";

		//@formatter:off
			String ruleCallCheck =
					//"//returns a set of rule signatures from the callStack matching the given ruleSignature\n"+
					"rule "+ MATCHING_RULE_INSIDE_CALLSTACK+"(ruleSignature) =\n" +
					"	return res in {\n" +
					"		//just looking for a rulename (i.e. call(x))\n" +
					"		if(head(ruleSignature)!={} and tail(ruleSignature)={}) then\n" +
					"			res := {signature | signature in callStack(self) with matches(head(signature),head(ruleSignature))}\n" +
					"			//looking for a rule signature i.e. call(x,[p1,...,pn])\n" +
					"		else if ( tail(ruleSignature) != {} ) then\n" +
					"			res := { signature | signature in callStack(self) with signature = ruleSignature }\n" +
					"		else res := {}\n"+
					"	}";

			
			String argsCheck =
					"//returns a set of rule signatures from the callStack matching the given argument list\n"
							+ "rule "						+ MATCHING_SIGNATURE_INSIDE_CALLSTACK
							+ "(listOfArguments) =\n"
							+ "return res in\n"
							+ "{\n"
							+ "	res := { signature | signature in callStack(self) with tail(signature) = listOfArguments }\n"
							+ "}";
			//@formatter:on

		File tmpfile;
		//		try {
		String tmpDir = System.getProperty("java.io.tmpdir");
		tmpfile = new File(tmpDir + "/coreasm-spec.casm");
		tmpfile.getParentFile().mkdirs();

		PrintWriter output;
		try {
			output = new PrintWriter(new FileWriter(tmpfile));

			output.write("CoreASM TempSpec\nuse Standard\nuse AoASM\ninit test\nrule test = skip\n\n");
			output.write(argsCheck + "\n");
			output.write(ruleCallCheck);
			output.close();
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		td = TestEngineDriver.newLaunch(tmpfile.getAbsolutePath());
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ASTNode rootNode = td.getEngine().getParser().getRootNode();
		ASTNode ruleDelaration = AspectTools.findRuleDeclaration(rootNode, MATCHING_RULE_INSIDE_CALLSTACK);
		String dot = AspectTools.nodes2dot(ruleDelaration);
		AspectTools.createDotGraph(dot, new LinkedList<Node>());

		if (td != null && TestEngineDriver.getRunningInstances().contains(td))
			td.stop();
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertFalse(td != null && TestEngineDriver.getRunningInstances().contains(td));
	}

}
