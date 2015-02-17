package org.coreasm.plugins.universalcontrol.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

import org.junit.BeforeClass;

public class OnceStepwise extends TestAllCasm {

	@BeforeClass
	public static void onlyOnce() {
		URL url = OnceStepwise.class.getClassLoader().getResource(".");

		try {
			testFiles = new LinkedList<File>();
			getTestFile(testFiles, new File(url.toURI()).getParentFile(), OnceStepwise.class);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}