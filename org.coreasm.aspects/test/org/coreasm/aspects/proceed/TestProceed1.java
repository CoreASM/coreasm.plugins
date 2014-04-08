package org.coreasm.aspects.proceed;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;

import org.coreasm.aspects.TestAllCasm;

public class TestProceed1 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestProceed1.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestProceed1.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
