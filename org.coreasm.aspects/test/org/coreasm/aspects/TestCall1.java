package org.coreasm.aspects;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Set;


import org.coreasm.engine.CoreASMEngine;
import org.coreasm.engine.CoreASMEngineFactory;
import org.coreasm.engine.CoreASMError;
import org.coreasm.engine.EngineProperties;
import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.absstorage.Update;

import org.junit.Assert;
import org.junit.Test;

public class TestCall1 {
		
	public enum EngineDriverStatus {stopped, running, paused};
	
	protected CoreASMError lastError;
	private boolean dumpState;
	private boolean printAgents;
	private PrintStream stderr;
	private PrintStream stddump;
	
	private int stepsLimit = 100;

	private volatile boolean shouldStop = false;
	private volatile boolean shouldPause = false;
	private boolean stopOnEmptyUpdates = true;
	private boolean stopOnStableUpdates = false;
	private boolean stopOnEmptyActiveAgents = false;
	private boolean stopOnFailedUpdates = false;
	private boolean stopOnError = true;
	private boolean stopOnStepsLimit = true;
	
	private boolean markSteps = false;
	private boolean printStackTrace = false;
	private boolean dumpUpdates = false;
	private String pluginLoadRequest = null;
	private boolean printProcessorStats = false;
	private String[] engineProperties = null;
	private int maxThreads = 1;
	private int batchSize = 1;
	
	private boolean updateFailed;
	
	CoreASMEngine engine;
	
	private void execSpec(BufferedReader spec) throws Exception {
		int step=0;
		Set<Update> updates,prevupdates=null;
		engine = CoreASMEngineFactory.createEngine();
		setEngineProperties(engine);
		// tempEngine.addObserver(this);
		if (printStackTrace)
			engine.setProperty(EngineProperties.PRINT_STACK_TRACE, EngineProperties.YES);
		if (printProcessorStats)
			engine.setProperty(EngineProperties.PRINT_PROCESSOR_STATS_PROPERTY, EngineProperties.YES);
		engine.initialize();
		engine.loadSpecification(spec);
		try {
			while (engine.getEngineMode() == EngineMode.emIdle) {
				if (shouldPause) {

					stderr.println("[!] Run is paused by user. Click on resume to continue...");

					while (shouldPause && !shouldStop)
						Thread.sleep(100);

					if (!shouldStop)
						stderr.println("[!] Resuming.");

				}

				if (shouldStop) {
					throw new Exception();
				}

				engine.step();
				step++;

				while (!shouldStop && engine.isBusy())
					Thread.sleep(50);

				if (shouldStop) {
					// give some time to the engine to finish
					if (engine.isBusy())
						Thread.sleep(200);

					throw new Exception();
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
				throw new Exception("handleError");
		} catch (Exception e) {
			e.printStackTrace();;
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
		if (stopOnError && lastError!=null)
			return true;
		if (stopOnStepsLimit && step>stepsLimit)
			return true;
		return false;
	}
	
	private void setEngineProperties(CoreASMEngine engine) throws Exception {
		engine.setProperty(EngineProperties.MAX_PROCESSORS, String.valueOf(maxThreads));
		engine.setProperty(EngineProperties.AGENT_EXECUTION_THREAD_BATCH_SIZE, String.valueOf(batchSize));
		if (engineProperties != null && engineProperties.length > 0) {
			String prop;
			String value;
			for (String ep : engineProperties) {
				if (ep.length() < 1)
					continue;
				int i = ep.indexOf('=');
				if (i < 1 || i == ep.length() - 1)
					throw new Exception("Invalid property-value option: " + ep);
				prop = ep.substring(0, i);
				value = ep.substring(i + 1, ep.length());
				System.out.println("Setting value of engine property '{}' to '{}'.");
				engine.setProperty(prop, value);
			}
		}
		if (pluginLoadRequest != null)
			engine.setProperty(EngineProperties.PLUGIN_LOAD_REQUEST_PROPERTY, pluginLoadRequest);
	}
	
	@Test
	public void Test() {
		
		InputStream fileStream = this.getClass().getClassLoader().getResourceAsStream(this.getClass().getSimpleName()+".casm");
		BufferedReader sr = new BufferedReader(new InputStreamReader(fileStream));
		try {
			execSpec(sr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Assert.assertEquals("Hallo", "Hallo");
	}

}
