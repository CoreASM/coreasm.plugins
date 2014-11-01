package org.coreasm.plugins.aspects.test.call;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;
import org.coreasm.plugins.aspects.test.TestAllCasm;

public class TestCall8 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCall8.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestCall8.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
