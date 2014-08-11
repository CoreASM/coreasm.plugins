package org.coreasm.aspects.test.cflow;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;
import org.coreasm.aspects.test.TestAllCasm;

public class TestCFlow4 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCFlow4.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestCFlow4.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
