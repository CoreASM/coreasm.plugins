package org.coreasm.aspects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.coreasm.aspects.utils.TestEngineDriver;
import org.coreasm.aspects.utils.TestEngineDriver.TestEngineDriverStatus;
import org.coreasm.engine.interpreter.ASTNode;

public class TestCall1 {

	static BufferedReader resource;
	static File file = null;
	TestEngineDriver td = null;

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

	/**
	 * Fetch the entire contents of a text file, and return it in a String.
	 * This style of implementation does not throw Exceptions to the caller.
	 * 
	 * @param aFile
	 *            is a file which already exists and can be read.
	 */
	static public String getContents(File aFile) {
		//...checks on aFile are elided
		StringBuilder contents = new StringBuilder();

		try {
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try {
				String line = null; //not declared within while loop
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return contents.toString();
	}

	@Before
	public void testResourceNotEmpty() {
		String data = getContents(file);
		System.out.println("running test of " + file.getName() + ":\n" + data);
		Assert.assertTrue(!data.isEmpty());
	}

	@Test
	public void runsSpecification() {
		Assert.assertNotNull(file);
		try {
			td = TestEngineDriver.newLaunch(file.getAbsolutePath());
			Assert.assertNotNull(td);
			Thread.sleep(1000);
			Assert.assertEquals(
					TestEngineDriverStatus.running,
					td.getStatus()
					);
			Thread.sleep(500);
			ASTNode root = td.getEngine().getParser().getRootNode();
			System.out.println("The root node is " + root.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (td != null && TestEngineDriver.getRunningInstances().contains(td))
				td.stop();
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Assert.assertFalse(td != null && TestEngineDriver.getRunningInstances().contains(td));
	}

}
