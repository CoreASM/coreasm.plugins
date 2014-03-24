package org.coreasm.aspects;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import org.coreasm.aspects.utils.TestEngineDriver;
import org.coreasm.aspects.utils.TestEngineDriver.TestEngineDriverStatus;
import org.coreasm.engine.interpreter.ASTNode;

public class TestTestEngineDriver {

	static File testFile;

	@BeforeClass
	public static void createTestFile() {
		String tmpDir = System.getProperty("java.io.tmpdir");
		testFile = new File(tmpDir + "/coreasm-spec.casm");
		testFile.getParentFile().mkdirs();

		try {
			PrintWriter output = new PrintWriter(new FileWriter(testFile));
			output.write(
					"CoreASM TempSpec\n" +
							"use Standard\n\n" +
							"init test\n" +
							"rule test = print \"Hallo Welt\"");
			output.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void runsSpecification() {
		Assert.assertNotNull(testFile);
		TestEngineDriver td = null;
		try {
			td = TestEngineDriver.newLaunch(testFile.getAbsolutePath());
			Assert.assertNotNull(td);
			Thread.sleep(500);
			Assert.assertEquals(
					TestEngineDriverStatus.running,
					td.getStatus()
					);
			Thread.sleep(500);
			ASTNode root = td.getEngine().getParser().getRootNode();
			System.out.println("The root node is " + root.toString());
			td.stop();
			Thread.sleep(500);
			Assert.assertFalse(td != null && TestEngineDriver.getRunningInstances().contains(td));
		}
		catch (Exception e) {
			if (TestEngineDriver.getRunningInstances().contains(td))
				td.stop();
			e.printStackTrace();
		}
	}

}
