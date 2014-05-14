package org.coreasm.aspects.args;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;

import org.coreasm.aspects.TestAllCasm;

public class TestArgs4 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestArgs4.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestArgs4.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
