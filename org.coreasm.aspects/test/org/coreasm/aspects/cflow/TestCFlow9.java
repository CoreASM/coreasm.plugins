package org.coreasm.aspects.cflow;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;

import org.coreasm.aspects.TestAllCasm;

public class TestCFlow9 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCFlow9.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestCFlow9.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
