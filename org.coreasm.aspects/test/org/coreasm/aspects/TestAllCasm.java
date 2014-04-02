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
	private final static PrintStream origOutput = System.out;
	private final static PrintStream origError = System.err;

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
			if (!minStepsList.isEmpty()) {
				try {
					minSteps = Integer.parseInt(minStepsList.get(0));
				} catch (NumberFormatException e) {
				}
			}
			int maxSteps = minSteps;
			if (!maxStepsList.isEmpty()) {
				try {
					maxSteps = Integer.parseInt(maxStepsList.get(0));
				} catch (NumberFormatException e) {
				}
			}
			TestEngineDriver td = null;
			String failMessage = "";
			int steps = 0;
			try {
				outContent.reset();
				td = TestEngineDriver.newLaunch(testFile.getAbsolutePath());
				td.setOutputStream(new PrintStream(outContent));
				for (steps = minSteps; steps <= maxSteps; steps++) {
					td.executeSteps(minSteps);
					minSteps = 1;
					//test if no error has been occured and maybe output error message
					if (!errContent.toString().isEmpty()) {
						failMessage = "An error occurred in " + testFile.getName() + ":" + errContent;
						new TestReport(testFile, failMessage, steps, false);
						break;
					}
					//check if no refused output is contained
					for (String refusedOutput : refusedOutputList) {
						if (outContent.toString().contains(refusedOutput)) {
							failMessage = "refused output found in test file:" + testFile.getName()
									+ ", refused output: "
									+ refusedOutput
									+ ", actual output: " + outContent.toString();
							new TestReport(testFile, failMessage, steps, false);
							break;
						}
					}
					for (String requiredOutput : new LinkedList<String>(requiredOutputList)) {
						if (outContent.toString().contains(requiredOutput))
							requiredOutputList.remove(requiredOutput);
					}
					if (requiredOutputList.isEmpty())
						break;
				}
				//check if no required output is missing
				if (!requiredOutputList.isEmpty()) {
					failMessage = "missing required output for test file:" + testFile.getName()
							+ ", missing output: "
							+ requiredOutputList.get(0)
							+ ", actual output: " + outContent.toString();
					new TestReport(testFile, failMessage, steps, false);
					break;
				}
			}
			catch (Exception e) {
				e.printStackTrace(origOutput);
			}
			finally {
				td.stop();
			}
			if (td.isRunning()) {
				failMessage = testFile.getName() + " has a running instance but is stopped!";
				new TestReport(testFile, failMessage, steps, false);
			}
			else
				new TestReport(testFile, steps);
			TestReport.printLast();
		}
	}

	static class TestReport {
		private static LinkedList<TestReport> reports = new LinkedList<TestReport>();
		private File file;
		private String message;
		private int steps;
		private boolean successful;

		public TestReport(File file, int steps) {
			this(file, "", steps);
		}

		public TestReport(File file, String message, int steps) {
			this(file, message, steps, true);
		}

		public TestReport(File file, String message, int steps, boolean successful) {
			this.file = file;
			this.message = message;
			this.successful = successful;
			this.steps = steps;
			reports.add(this);
		}

		public static void printLast() {
			reports.getLast().print();
		}

		public void print() {
			if (this.successful) {
				String success = "Test of " + this.file.getName() + " successful after " + steps + (steps == 1 ? " step" : " steps");
				origOutput.println(this.message.isEmpty() ? success : success + "; " + this.message);
			}
			else
				origError.println("An error occurred after " + steps + " steps in " + this.file.getName() + ": "
						+ this.message);
		}

		public void printTestReports() {
			for (TestReport report : reports) {
				report.print();
			}
		}
	}

}
