package org.coreasm.aspects.utils;

/*
 * TestEngineDriver.java $Revision: 108 $
 * 
 * Copyright (C) 2005 Vincenzo Gervasi
 * 
 * Later modified and improved by
 * Roozbeh Farahbod
 * Daniel Sadilek
 * 
 * Last modified by $Author: rfarahbod $ on $Date: 2009-12-15 14:06:24 -0500
 * (Tue, 15 Dec 2009) $.
 * 
 * Licensed under the Academic Free License version 3.0
 * http://www.opensource.org/licenses/afl-3.0.php
 * http://www.coreasm.org/afl-3.0.php
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Date;
import java.util.Set;

import javax.swing.JOptionPane;

import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.CoreASMEngineFactory;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.Engine;
import org.coreasm.engine.EngineErrorEvent;
import org.coreasm.engine.EngineErrorObserver;
import org.coreasm.engine.EngineEvent;
import org.coreasm.engine.EngineProperties;
import org.coreasm.engine.EngineStepObserver;
import org.coreasm.engine.EngineWarningObserver;
import org.coreasm.engine.Specification;
import org.coreasm.engine.StepFailedEvent;
import org.coreasm.engine.absstorage.Update;
import org.coreasm.engine.interpreter.ASTNode;
import org.coreasm.engine.plugin.PluginServiceInterface;
import org.coreasm.engine.plugins.debuginfo.DebugInfoPlugin.DebugInfoPSI;
import org.coreasm.engine.plugins.io.IOPlugin.IOPluginPSI;
import org.coreasm.engine.plugins.io.InputProvider;
import org.coreasm.util.CoreASMGlobal;
import org.coreasm.util.Logger;
import org.coreasm.util.Tools;

public class TestEngineDriver implements Runnable, EngineStepObserver, EngineErrorObserver,
		EngineWarningObserver {

	private static TestEngineDriver syntaxInstance = null;
	protected static TestEngineDriver runningInstance = null;

    protected Engine engine;
	private final boolean isSyntaxEngine;

	public enum TestEngineDriverStatus {
		stopped, running
	};

	private TestEngineDriverStatus status = TestEngineDriverStatus.stopped;

	private String abspathname;
	private boolean updateFailed;
	private String stepFailedMsg;
	protected CoreASMError lastError;
	private int stepsLimit;
	private boolean stopOnEmptyUpdates;
	private boolean stopOnStableUpdates;
	private boolean stopOnEmptyActiveAgents;
	private boolean stopOnFailedUpdates;
	private boolean stopOnError;
	private boolean stopOnStepsLimit;
	private boolean dumpUpdates;
	private boolean dumpState;
	private boolean dumpFinal;
	private boolean markSteps;
	private boolean printAgents;
	private PrintStream stderr;
	private PrintStream stddump;
	private PrintStream systemErr;
	private boolean shouldStop;
	static long lastPrefChangeTime;

	public static synchronized TestEngineDriver getSyntaxInstance() {
		if (syntaxInstance == null ) {
			syntaxInstance = new TestEngineDriver(true);
			lastPrefChangeTime = (new Date()).getTime();
		}
		return syntaxInstance;
	}

	public static TestEngineDriver getRunningInstance() {
		return runningInstance;
	}

	private TestEngineDriver(boolean isSyntaxEngine) {
		super();
		CoreASMGlobal.setRootFolder(Tools.getRootFolder());
		engine = (Engine) org.coreasm.engine.CoreASMEngineFactory.createEngine();
		String pluginFolders = "target/";
		if (System.getProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY) != null)
			pluginFolders += EngineProperties.PLUGIN_FOLDERS_DELIM
					+ System.getProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY);
		engine.setProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY, pluginFolders);
		engine.setClassLoader(CoreASMEngineFactory.class.getClassLoader());
		engine.initialize();
		engine.waitWhileBusy();
		shouldStop = false;
		this.isSyntaxEngine = isSyntaxEngine;
	}


	public TestEngineDriverStatus getStatus() {
		return status;
	}

	public void setDefaultConfig()
	{
		Logger.verbosityLevel = Logger.WARNING;
		stopOnEmptyUpdates = false;
		stopOnStableUpdates = false;
		stopOnEmptyActiveAgents = true;
		stopOnFailedUpdates = true;
		stopOnError = true;
		stopOnStepsLimit = false; 	// TODO this should probably be false
		stepsLimit = 10;
		dumpUpdates = false;
		dumpState = false;
		dumpFinal = false;
		markSteps = false;
		printAgents = false;
	}


	public Engine getEngine() {
		return engine;
	}

	public static void newLaunch(String abspathname) {
		runningInstance = new TestEngineDriver(false);
		runningInstance.setDefaultConfig();
		runningInstance.dolaunch(abspathname);
	}

	public void dolaunch(String abspathname) {
		this.abspathname = abspathname;
		Thread t = new Thread(this);
		try {
			t.setName("CoreASM run of " + abspathname.substring(abspathname.lastIndexOf(File.separator)));
		}
		catch (Throwable e) {
			t.setName("CoreASM run of " + abspathname);
		}

		setInputOutputPhase2();

		if (engine.getEngineMode() == EngineMode.emError) {
			engine.recover();
			engine.waitWhileBusy();
		}
		engine.loadSpecification(abspathname);
		engine.waitWhileBusy();

		t.start();

	}

	protected void preExecutionCallback() {
		// Empty implementation. Can be overridden by subclasses.
	}

	protected void postExecutionCallback() {
		// Empty implementation. Can be overridden by subclasses.
	}

	@Override
	public void run()
	{
		if (this == runningInstance)
			status = TestEngineDriverStatus.running;

		int step = 0;
		Exception exception = null;

		engine.addObserver(this); // TODO this too prevents more than a single syntaxInstance being run at the same time...
		Set<Update> updates, prevupdates = null;

		try {

			if (engine.getEngineMode() != EngineMode.emIdle) {
				handleError();
				return;
			}

			preExecutionCallback();

			while (engine.getEngineMode() == EngineMode.emIdle) {

				engine.step();
				step++;

				while (engine.isBusy() && !shouldStop)
					Thread.sleep(50);

				if (shouldStop) {
					// give some time to the engine to finish
					if (engine.isBusy())
						Thread.sleep(200);
					break;//stop engine => see finally
				}

				updates = engine.getUpdateSet(0);
				if (markSteps)
					stddump.println("#--- end of step " + step);
				if (dumpUpdates)
					stddump.println("Updates at step " + step + ": " + updates);
				if (dumpState)
					stddump.println("State at step " + step + ":\n" + engine.getState());
				if (printAgents)
					stddump.println("Last selected agents: " + engine.getLastSelectedAgents());
				if (terminated(step, updates, prevupdates))
					break;
				prevupdates = updates;

			}
			if (engine.getEngineMode() != EngineMode.emIdle)
				handleError();
		}
		catch (Exception e) {
			exception = e;
		}
		finally {
			if (runningInstance != null && runningInstance.engine != null) {
				runningInstance.engine.removeObserver(this);

				if (exception != null)
					if (exception instanceof TestEngineDriverException)
						stderr.println("[!] Run is terminated by user.");
					else {
						stderr.println("[!] Run is terminated with exception " + exception);
					}

				if (dumpFinal && step > 0) {
					stddump.println("--------------------FINISHED---------------------");
					stddump.println("Final engine mode was " + runningInstance.engine.getEngineMode());
					if (lastError != null)
						stddump.println("Last error was " + lastError);
					if (stepFailedMsg != null)
						stddump.println("Step failed reason was " + stepFailedMsg);
					stddump.println("Final state was:\n" + runningInstance.engine.getState());

					// Repeating 
					if (exception != null)
						if (exception instanceof TestEngineDriverException)
							stderr.println("[!] Run is terminated by user.");
						else
							stderr.println("[!] Run is terminated with exception " + exception);
				}
				System.setErr(systemErr);

				if (this == runningInstance)
					status = TestEngineDriverStatus.stopped;

				runningInstance.engine.hardInterrupt();

				runningInstance = null;

				postExecutionCallback();
			}
		}
	}

    /**
     * method that stops the currently running engine
     */
	public void stop() {
		shouldStop = true;
	}

	private boolean terminated(int step, Set<Update> updates, Set<Update> prevupdates) {
		if (stopOnEmptyUpdates && updates.isEmpty())
			return true;
		if (stopOnStableUpdates && updates.equals(prevupdates))
			return true;
		if (stopOnEmptyActiveAgents && engine.getAgentSet().size() < 1)
			return true;
		if (stopOnFailedUpdates && updateFailed)
			return true;
		if (stopOnError && lastError != null)
			return true;
		if (stopOnStepsLimit && step > stepsLimit)
			return true;
		return false;
	}

	private void setInputOutputPhase2() {

		stderr = System.err;
		stddump = System.out;
		// Setting input/output channels of the IO Plugin
		PluginServiceInterface pi = engine.getPluginInterface("IOPlugin");
		if (pi != null) {
			((IOPluginPSI) pi).setInputProvider(new InputProvider() {
				@Override
				public String getValue(String message) {
					String input = JOptionPane.showInputDialog(null, message, "");
					return input;
				}
			});

			((IOPluginPSI) pi).setOutputStream(new PrintStream(System.out));
		}

		// Setting input/output channels of the IO Plugin
		pi = engine.getPluginInterface("DebugInfoPlugin");
		if (pi != null) {
			((DebugInfoPSI) pi).setOutputStream(new PrintStream(System.out));
		}
	}

	public static ASTNode getRootNodeFromSpecification(String body) {
		return getRootNodeFromSpecification("", body);
	}

	public static ASTNode getRootNodeFromSpecification(String header, String body) {

		File tmpfile;
		ASTNode rootNode = null;
		try {
			String tmpDir = System.getProperty("java.io.tmpdir");
			tmpfile = new File(tmpDir + "/coreasm-spec.casm");
			tmpfile.getParentFile().mkdirs();

			PrintWriter output = new PrintWriter(new FileWriter(tmpfile));
			if (header.isEmpty())
				output.write("CoreASM TempSpec\nuse Standard\nuse AoASMPlugin\ninit test\nrule test = skip\n\n");
			else
				output.write(header);
			output.write(body + "\n");
			output.close();

			TestEngineDriver.newLaunch(tmpfile.getAbsolutePath());
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {

			}
			AspectTools.setCapi(getRunningInstance().getEngine());
			rootNode = getRunningInstance().getEngine().getParser().getRootNode();

			if (TestEngineDriver.getRunningInstance() != null)
				TestEngineDriver.getRunningInstance().stop();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (TestEngineDriver.getRunningInstance() != null)
			TestEngineDriver.getRunningInstance().stop();
		return rootNode;
	}

	public synchronized Specification getSpec(String text, boolean loadPlugins) {
		if (!isSyntaxEngine)
			return null;

		engine.waitWhileBusy();
		if (engine.getEngineMode() == EngineMode.emError) {
			engine.recover();
			return null;
		}
		engine.parseSpecificationHeader(new StringReader(text), loadPlugins);
		engine.waitWhileBusy();
		if (engine.getEngineMode() == EngineMode.emError) {
			engine.recover();
			return null;
		}
		else
			return engine.getSpec();
	}

	@Override
	public void update(EngineEvent event) {

		// Looking for StepFailed
		if (event instanceof StepFailedEvent) {
			StepFailedEvent sEvent = (StepFailedEvent) event;
			synchronized (this) {
				updateFailed = true;
				stepFailedMsg = sEvent.reason;
			}
		}

		// Looking for errors
		else if (event instanceof EngineErrorEvent) {
			synchronized (this) {
				lastError = ((EngineErrorEvent) event).getError();
			}
			System.out.println(lastError);
		}

	}

	/**
	 * @return Returns the maxsteps.
	 */
	public int getMaxsteps() {
		return stepsLimit;
	}

	/**
	 * @param maxsteps
	 *            The maxsteps to set.
	 */
	public void setMaxsteps(int maxsteps) {
		this.stepsLimit = maxsteps;
	}

	/**
	 * @return Returns the stopOnEmptyUpdates.
	 */
	public boolean isStopOnEmptyUpdates() {
		return stopOnEmptyUpdates;
	}

	/**
	 * @param stopOnEmptyUpdates
	 *            The stopOnEmptyUpdates to set.
	 */
	public void setStopOnEmptyUpdates(boolean stopOnEmptyUpdates) {
		this.stopOnEmptyUpdates = stopOnEmptyUpdates;
	}

	/**
	 * @return Returns the stopOnError.
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

	/**
	 * @param stopOnError
	 *            The stopOnError to set.
	 */
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	/**
	 * @return Returns the stopOnFailedUpdates.
	 */
	public boolean isStopOnFailedUpdates() {
		return stopOnFailedUpdates;
	}

	/**
	 * @param stopOnFailedUpdates
	 *            The stopOnFailedUpdates to set.
	 */
	public void setStopOnFailedUpdates(boolean stopOnFailedUpdates) {
		this.stopOnFailedUpdates = stopOnFailedUpdates;
	}

	public boolean isStopOnEmptyActiveAgents() {
		return stopOnEmptyActiveAgents;
	}

	public void setStopOnEmptyActiveAgents(boolean b) {
		stopOnEmptyActiveAgents = b;
	}

	/**
	 * @return Returns the stopOnStableUpdates.
	 */
	public boolean isStopOnStableUpdates() {
		return stopOnStableUpdates;
	}

	/**
	 * @param stopOnStableUpdates
	 *            The stopOnStableUpdates to set.
	 */
	public void setStopOnStableUpdates(boolean stopOnStableUpdates) {
		this.stopOnStableUpdates = stopOnStableUpdates;
	}

	/**
	 * @return Returns the stopOnStepsLimit.
	 */
	public boolean isStopOnStepsLimit() {
		return stopOnStepsLimit;
	}

	/**
	 * @param stopOnStepsLimit
	 *            The stopOnStepsLimit to set.
	 */
	public void setStopOnStepsLimit(boolean stopOnStepsLimit) {
		this.stopOnStepsLimit = stopOnStepsLimit;
	}

	/**
	 * @return Returns the stepsLimit.
	 */
	public int getStepsLimit() {
		return stepsLimit;
	}

	/**
	 * @param stepsLimit
	 *            The stepsLimit to set.
	 */
	public void setStepsLimit(int stepsLimit) {
		this.stepsLimit = stepsLimit;
	}

	/**
	 * @return Returns the lastError.
	 */
	public CoreASMError getLastError() {
		return lastError;
	}

	/**
	 * @return Returns the stepFailedMsg.
	 */
	public String getStepFailedMsg() {
		return stepFailedMsg;
	}

	/**
	 * @return Returns the updateFailed.
	 */
	public boolean isUpdateFailed() {
		return updateFailed;
	}

	protected void handleError() {
		String message = "";
		if (lastError != null)
			message = message + lastError.showError();
		else
			message = message + " unknown.";

		//		JOptionPane.showMessageDialog(null, message, "CoreASM Engine Error", JOptionPane.ERROR_MESSAGE);
		showErrorDialog("CoreASM Engine Error", message);

		lastError = null;
		stepFailedMsg = null;
		engine.recover();
		engine.waitWhileBusy();
	}

	private void showErrorDialog(String title, String message) {
		//MessageDialog.openError(shell, title, message);
		stderr.println("\n" + message);
	}

	/**
	 * An internal exception class.
	 */
	private class TestEngineDriverException extends Exception {
		private static final long serialVersionUID = 1L;

		public TestEngineDriverException() {

		}
	}

	public boolean isDumpFinal() {
		return dumpFinal;
	}

	public void setDumpFinal(boolean dumpFinal) {
		this.dumpFinal = dumpFinal;
	}

}
