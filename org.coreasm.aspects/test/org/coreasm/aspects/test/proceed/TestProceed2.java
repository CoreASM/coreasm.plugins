package org.coreasm.aspects.test.proceed;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;
import org.coreasm.aspects.test.TestAllCasm;

public class TestProceed2 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestProceed2.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestProceed2.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
