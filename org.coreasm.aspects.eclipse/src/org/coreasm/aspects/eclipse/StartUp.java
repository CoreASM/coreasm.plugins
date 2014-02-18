package org.coreasm.aspects.eclipse;
import org.eclipse.ui.IStartup;


public class StartUp implements IStartup {

	@Override
	public void earlyStartup() {
		new AopASMEclipsePlugin();
	}

}
