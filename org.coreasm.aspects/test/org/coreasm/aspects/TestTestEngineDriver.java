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
		try {
			TestEngineDriver.newLaunch(testFile.getAbsolutePath());
			Assert.assertNotNull(TestEngineDriver.getRunningInstance());
			Thread.sleep(250);
			Assert.assertEquals(
					TestEngineDriverStatus.running,
					TestEngineDriver.getRunningInstance().getStatus()
					);
			Thread.sleep(250);
			ASTNode root = TestEngineDriver.getRunningInstance().getEngine().getParser().getRootNode();
			System.out.println("The root node is " + root.toString());
			TestEngineDriver.getRunningInstance().stop();
			Thread.sleep(250);
			Assert.assertNull(TestEngineDriver.getRunningInstance());
		}
		catch (Exception e) {
			if (TestEngineDriver.getRunningInstance() != null)
				TestEngineDriver.getRunningInstance().stop();
			e.printStackTrace();
		}
	}

}
