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
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.coreasm.aspects.AoASMPlugin;
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
import org.coreasm.util.CoreASMGlobal;
import org.coreasm.util.Logger;
import org.coreasm.util.Tools;

public class TestEngineDriver implements Runnable, EngineStepObserver, EngineErrorObserver,
		EngineWarningObserver {

	protected static List<TestEngineDriver> runningInstances = null;

	protected Engine engine;
	private final boolean isSyntaxEngine;

	public enum TestEngineDriverStatus {
		stopped, running, paused
	};

	private TestEngineDriverStatus status = TestEngineDriverStatus.stopped;

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
	private boolean shouldStop;
	private boolean shouldPause;
	static long lastPrefChangeTime;

	private TestEngineDriver(boolean isSyntaxEngine) {
		//super();
		if (runningInstances == null)
			runningInstances = new LinkedList<TestEngineDriver>();
		runningInstances.add(this);
		CoreASMGlobal.setRootFolder(Tools.getRootFolder());
		engine = (Engine) org.coreasm.engine.CoreASMEngineFactory.createEngine();
		engine.addObserver(this);
		shouldStop = false;
		shouldPause = true;
		status = TestEngineDriverStatus.paused;
		this.isSyntaxEngine = isSyntaxEngine;

		String pluginFolders = Tools.getRootFolder(AoASMPlugin.class).split("target")[0] + "target/";
		if (System.getProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY) != null)
			pluginFolders += EngineProperties.PLUGIN_FOLDERS_DELIM
					+ System.getProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY);
		//		JOptionPane.showMessageDialog(null, "additional plugin folders: " + pluginFolders, "TestEngineDriver("
		//				+ isSyntaxEngine + ")", JOptionPane.INFORMATION_MESSAGE);
		engine.setProperty(EngineProperties.PLUGIN_FOLDERS_PROPERTY, pluginFolders);
		engine.setClassLoader(CoreASMEngineFactory.class.getClassLoader());
		engine.initialize();
		engine.waitWhileBusy();
	}

	public TestEngineDriverStatus getStatus() {
		return status;
	}

	public void setDefaultConfig()
	{
		Logger.verbosityLevel = Logger.ERROR;
		stopOnEmptyUpdates = false;
		stopOnStableUpdates = false;
		stopOnEmptyActiveAgents = true;
		stopOnFailedUpdates = false;
		stopOnError = false;
		stopOnStepsLimit = false; 	// TODO this should probably be false
		stepsLimit = -1; //means infinite steps
		dumpUpdates = false;
		dumpState = false;
		dumpFinal = false;
		markSteps = false;
		printAgents = false;
	}

	public Engine getEngine() {
		return engine;
	}

	public boolean isRunning() {
		return runningInstances.contains(this);
	}

	public static TestEngineDriver newLaunch(String abspathname) {
		TestEngineDriver td = new TestEngineDriver(false);
		td.setDefaultConfig();
		td.dolaunch(abspathname);
		return td;
	}

	public void dolaunch(String abspathname) {
		Thread t = new Thread(this);

		try {
			t.setName("CoreASM run of " +
					abspathname.substring(abspathname.lastIndexOf(File.separator)));
		}
		catch (Throwable e) {
			t.setName("CoreASM run of " + abspathname);
		}

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
		int step = 0;
		Exception exception = null;

		Set<Update> updates, prevupdates = null;

		try {

			if (engine.getEngineMode() != EngineMode.emIdle) {
				handleError();
				return;
			}

			preExecutionCallback();

			while (engine.getEngineMode() == EngineMode.emIdle) {

				//set current mode
				if (shouldStop && stepsLimit <= 0) {
					engine.waitWhileBusy();
					break;
				}
				else if (shouldPause || stepsLimit == 0) {
					status = TestEngineDriverStatus.paused;
					Thread.sleep(100);
				}
				else
				{
					status = TestEngineDriverStatus.running;

					//execute a step
					engine.waitWhileBusy();
					engine.step();
					step++;
					engine.waitWhileBusy();

					updates = engine.getUpdateSet(0);
					if (markSteps)
						System.out.println("#--- end of step " + step);
					if (dumpUpdates)
						System.out.println("Updates at step " + step + ": " + updates);
					if (dumpState)
						System.out.println("State at step " + step + ":\n" + engine.getState());
					if (printAgents)
						System.out.println("Last selected agents: " + engine.getLastSelectedAgents());
					if (terminated(step, updates, prevupdates))
						break;
					prevupdates = updates;
					stepsLimit--;
				}

			}
			if (engine.getEngineMode() != EngineMode.emIdle)
				handleError();
		}
		catch (Exception e) {
			exception = e;
			e.printStackTrace();
		}
		finally {
			if (runningInstances != null && runningInstances.contains(this)) {
				runningInstances.remove(this);
				this.engine.removeObserver(this);

				if (exception != null)
					System.err.println("[!] Run is terminated with exception " + exception);

				if (dumpFinal && step > 0) {
					System.out.println("--------------------FINISHED---------------------");
					System.out.println("Final engine mode was " + this.engine.getEngineMode());
					if (lastError != null)
						System.out.println("Last error was " + lastError);
					if (stepFailedMsg != null)
						System.out.println("Step failed reason was " + stepFailedMsg);
					System.out.println("Final state was:\n" + this.engine.getState());

					// Repeating 
					if (exception != null)
						System.err.println("[!] Run is terminated with exception " + exception);
				}

				this.engine.terminate();
				this.engine.hardInterrupt();

				postExecutionCallback();
				status = TestEngineDriverStatus.stopped;
			}
		}
	}

	/**
	 * starts the engine and resets
	 */
	public synchronized void start() {
		shouldPause = false;
		shouldStop = false;
		stopOnStepsLimit = false;
		resume();
	}

	public synchronized void restart() {
		shouldPause = false;
		shouldStop = false;
		stopOnStepsLimit = false;
		stepsLimit = -1;
		while (getStatus() == TestEngineDriverStatus.running)
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public synchronized void resume() {
		if (stepsLimit == 0)
			stepsLimit = -1;
		shouldPause = false;
		while (getStatus() == TestEngineDriverStatus.paused)
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public synchronized void stop() {
		shouldStop = true;
		while (getStatus() != TestEngineDriverStatus.stopped)
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public synchronized void pause() {
		shouldPause = true;
		while (getStatus() == TestEngineDriverStatus.running)
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public synchronized void executeSteps(int numberOfSteps) {
		stepsLimit = numberOfSteps;
		stopOnStepsLimit = true;
		resume();
		while (stepsLimit > 0 && getStatus() != TestEngineDriverStatus.stopped)
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
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
		if (stopOnStepsLimit && stepsLimit <= 0 && shouldStop)
			return true;
		return false;
	}

	public static ASTNode getRootNodeFromSpecification(String body) {
		return getRootNodeFromSpecification("", body);
	}

	public static ASTNode getRootNodeFromSpecification(String header, String body) {

		File tmpfile;
		ASTNode rootNode = null;
		TestEngineDriver td = null;
		try {
			String tmpDir = System.getProperty("java.io.tmpdir");
			tmpfile = new File(tmpDir + "/coreasm-spec.casm");
			tmpfile.getParentFile().mkdirs();
			tmpfile.deleteOnExit();

			PrintWriter output = new PrintWriter(new FileWriter(tmpfile));
			if (header.isEmpty())
				output.write("CoreASM TempSpec\nuse Standard\nuse AoASMPlugin\ninit test\nrule test = print \"Step\"\n\n");
			else
				output.write(header);
			output.write(body + "\n");
			output.close();

			td = TestEngineDriver.newLaunch(tmpfile.getAbsolutePath());
			AspectTools.setCapi(td.getEngine());
			rootNode = td.engine.getParser().getRootNode();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			td.stop();
		}
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
		System.err.println(title + "\n" + message);
	}

	public boolean isDumpFinal() {
		return dumpFinal;
	}

	public void setDumpFinal(boolean dumpFinal) {
		this.dumpFinal = dumpFinal;
	}

}
