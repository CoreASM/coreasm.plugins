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
	public void Test() {
		Assert.assertNotNull(testFile);
		try {
			TestEngineDriver.newLaunch(testFile.getAbsolutePath());
			Thread.sleep(1000);
			Assert.assertEquals(
					TestEngineDriverStatus.running,
					TestEngineDriver.getRunningInstance().getStatus()
					);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
