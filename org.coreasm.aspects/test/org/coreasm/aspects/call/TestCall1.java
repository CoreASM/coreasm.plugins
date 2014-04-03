package org.coreasm.aspects.call;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;

import org.coreasm.aspects.TestAllCasm;

public class TestCall1 extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = TestCall1.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), TestCall1.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
