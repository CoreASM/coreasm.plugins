package org.coreasm.aspects;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.coreasm.aspects.utils.TestEngineDriver;

public class TestCall2 {

	static BufferedReader resource;
	static File file = null;
	TestEngineDriver td = null;

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCall2.class.getClassLoader().getResource(TestCall2.class.getSimpleName() + ".casm");
		try {
			file = new File(url.toURI());
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private final PrintStream origOutput = System.out;
	private final PrintStream origError = System.err;
	private static List<String> requiredOutputList = new LinkedList<String>();

	@BeforeClass
	public static void setRequiredOutput() {
		requiredOutputList.add("before_test1");
		requiredOutputList.add("after_test1");

	}

	@Before
	public void setUpStreams() {
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@After
	public void cleanUpStreams() {
		System.setOut(origOutput);
		System.setErr(origError);
	}

	@Test
	public void runsSpecification() {
		Assert.assertNotNull(file);
		try {
			td = TestEngineDriver.newLaunch(file.getAbsolutePath());
			Assert.assertNotNull(td);
			td.executeSteps(1);
			for (String requiredOutput : requiredOutputList) {
				if (!outContent.toString().contains(requiredOutput))
					Assert.fail("missing required output:\n" + requiredOutput);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			td.stop();
		}
		Assert.assertFalse(TestEngineDriver.getRunningInstances().contains(td));
	}

}
