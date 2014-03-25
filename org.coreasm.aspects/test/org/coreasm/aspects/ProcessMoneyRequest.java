package org.coreasm.aspects;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import org.coreasm.aspects.utils.TestEngineDriver;

public class ProcessMoneyRequest {

	static File file = null;
	TestEngineDriver td = null;

	@BeforeClass
	public static void onlyOnce() {
		URL url = ProcessMoneyRequest.class.getClassLoader().getResource(
				ProcessMoneyRequest.class.getSimpleName() + ".casm");
		try {
			file = new File(url.toURI());
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void runsSpecification() {
		Assert.assertNotNull(file);
		TestEngineDriver td = null;
		try {
			td = TestEngineDriver.newLaunch(file.getAbsolutePath());
			td.executeSteps(3);
		}
		catch (Exception e) {
			Assert.fail();
			e.printStackTrace();
		}
		finally {
			if (TestEngineDriver.getRunningInstances().contains(td))
				td.stop();
			System.out.println("successful test ProcessMoneyRequest");
		}
	}

}
