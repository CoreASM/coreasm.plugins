package org.coreasm.aspects;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.coreasm.aspects.utils.TestEngineDriver;
import org.coreasm.aspects.utils.TestEngineDriver.TestEngineDriverStatus;

public class TestCall1 {

	static BufferedReader resource;
	static File file;

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCall1.class.getClassLoader().getResource(TestCall1.class.getSimpleName() + ".casm");
		try {
			file = new File(url.toURI());
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}

		InputStream fileStream = TestCall1.class.getClassLoader()
				.getResourceAsStream(TestCall1.class.getSimpleName() + ".casm");
		resource = new BufferedReader(new InputStreamReader(fileStream));
	}

	@Test
	public void testResourceNotEmpty() {
		String data = "";
		try {
			while (resource.readLine() != null) {
				data += resource.readLine() + "\n";
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(data);
		Assert.assertTrue(!data.isEmpty());
	}

	@Test
	public void runTestCall1() {
		try {
			Assert.assertNotNull(file);

			TestEngineDriver.newLaunch(file.getAbsolutePath());
			Assert.assertEquals(
					TestEngineDriverStatus.running,
					TestEngineDriver.getRunningInstance().getStatus()
					);
			TestEngineDriver.getRunningInstance().stop();
			Thread.sleep(500);
			Assert.assertNull(
					TestEngineDriver.getRunningInstance()
					);
		}
		catch (Exception e) {

		}
		finally {
			if (TestEngineDriver.getRunningInstance() != null)
				TestEngineDriver.getRunningInstance().stop();
		}
	}

}
