package org.coreasm.aspects.test.call;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;
import org.coreasm.aspects.test.TestAllCasm;

public class TestCall7 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCall7.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestCall7.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
