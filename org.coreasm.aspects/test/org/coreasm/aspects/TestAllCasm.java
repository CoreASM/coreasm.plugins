package org.coreasm.aspects;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.coreasm.aspects.utils.TestEngineDriver;
import org.coreasm.util.Tools;

public class TestAllCasm {

	private static List<File> testFiles = null;

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestAllCasm.class.getClassLoader().getResource(TestAllCasm.class.getSimpleName() + ".casm");
		
		try {
			testFiles = new LinkedList<File>();
			getTestFiles(testFiles, new File(url.toURI()).getParentFile());
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	private final ByteArrayOutputStream logContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private final PrintStream origOutput = System.out;
	private final PrintStream origError = System.err;

	public static List<String> getFilteredOutput(File file, String filter) {
		List<String> requiredOutputList = new LinkedList<String>();
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(file));
			String line = null; //not declared within while loop
			while ((line = input.readLine()) != null) {
				if (Pattern.matches(".*" + filter + ".*", line)) {
					int first = line.indexOf("\"");
					int last = line.lastIndexOf("\"");
					if (first >= 0 && last >= 0)
						requiredOutputList.add(Tools.convertFromEscapeSequence(line.substring(first + 1, last)));
				}
			}
			input.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return requiredOutputList;
	}

	private static void getTestFiles(List<File> testFiles, File file) {
		if (file != null && file.isDirectory())
			for (File child : file.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {
					return (file.isDirectory()
							|| file.getName().toLowerCase().endsWith(".casm")
							|| file.getName().toLowerCase().endsWith(".coreasm"));
				}
			})) {
				getTestFiles(testFiles, child);
			}
		else if (file != null)
			testFiles.add(file);
	}

	@Before
	public void setUpStreams() {
		System.setOut(new PrintStream(logContent));
		System.setErr(new PrintStream(errContent));
	}

	@After
	public void cleanUpStreams() {
		System.setOut(origOutput);
		System.setErr(origError);
	}

	@Test
	public void runSpecifications() {
		for (File testFile : testFiles) {
			List<String> requiredOutputList = getFilteredOutput(testFile, "@require");
			List<String> refusedOutputList = getFilteredOutput(testFile, "@refuse");
			List<String> minStepsList = getFilteredOutput(testFile, "@minsteps");
			List<String> maxStepsList = getFilteredOutput(testFile, "@maxsteps");
			int minSteps = 1;
			int maxSteps = 1;
			if (!minStepsList.isEmpty()) {
				try {
					minSteps = Integer.parseInt(minStepsList.get(0));
				} catch (NumberFormatException e) {
				}
			}
			if (!maxStepsList.isEmpty()) {
				try {
					maxSteps = Integer.parseInt(maxStepsList.get(0));
				} catch (NumberFormatException e) {
				}
			}
			TestEngineDriver td = null;
			try {
				outContent.reset();
				td = TestEngineDriver.newLaunch(testFile.getAbsolutePath());
				td.setOutputStream(new PrintStream(outContent));
				for (int steps = minSteps; steps <= maxSteps; steps++) {
					td.executeSteps(minSteps);
					minSteps = 1;
					//test if no error has been occured and maybe output error message
					if (!errContent.toString().isEmpty()) {
						origError.println("An error occurred in " + testFile.getName() + ":");
						origError.println(errContent);
						Assert.fail();
					}
					//check if no refused output is contained
					for (String refusedOutput : refusedOutputList) {
						if (outContent.toString().contains(refusedOutput)) {
							String failMessage = "refused output found in test file:" + testFile.getName()
									+ ", refused output: "
									+ refusedOutput
									+ ", actual output: " + outContent.toString();
							origError.println(failMessage);
							Assert.fail(failMessage);
						}
					}
				}
				//check if no required output is missing
				for (String requiredOutput : requiredOutputList) {
					if (!outContent.toString().contains(requiredOutput)) {
						String failMessage = "missing required output for test file:" + testFile.getName()
								+ ", missing output: "
								+ requiredOutput
								+ ", actual output: " + outContent.toString();
						origError.println(failMessage);
						Assert.fail(failMessage);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(origOutput);
			}
			finally {
				td.stop();
			}
			if (td.isRunning())
				origError.println(testFile.getName() + " has a running instance but is stopped!");
			Assert.assertFalse(td.isRunning());
			origOutput.println("Test of " + testFile.getName() + " successful");
		}

	}

}
