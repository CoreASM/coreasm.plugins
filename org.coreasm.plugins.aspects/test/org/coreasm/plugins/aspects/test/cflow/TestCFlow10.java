package org.coreasm.plugins.aspects.test.cflow;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;
import org.coreasm.plugins.aspects.test.TestAllCasm;

public class TestCFlow10 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCFlow10.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestCFlow10.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
