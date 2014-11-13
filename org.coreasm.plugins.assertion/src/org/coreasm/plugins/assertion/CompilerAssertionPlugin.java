package org.coreasm.plugins.assertion;

import java.util.ArrayList;
import java.util.List;

import org.coreasm.compiler.CodeType;
import org.coreasm.compiler.codefragment.CodeFragment;
import org.coreasm.compiler.exception.CompilerException;
import org.coreasm.compiler.interfaces.CompilerCodePlugin;
import org.coreasm.compiler.interfaces.CompilerExtensionPointPlugin;
import org.coreasm.compiler.interfaces.CompilerPlugin;
import org.coreasm.compiler.mainprogram.statemachine.EngineTransition;
import org.coreasm.engine.plugin.Plugin;

public class CompilerAssertionPlugin extends CompilerCodePlugin implements
		CompilerPlugin, CompilerExtensionPointPlugin {
	AssertionPlugin parent;
	
	public CompilerAssertionPlugin(AssertionPlugin parent) {
		this.parent = parent;
	}

	@Override
	public String getName() {
		return parent.getName();
	}

	@Override
	public Plugin getInterpreterPlugin() {
		return parent;
	}	
	
	@Override
	public List<EngineTransition> getTransitions() {
		List<EngineTransition> transitions = new ArrayList<EngineTransition>();
		
		CodeFragment c = new CodeFragment("");
		//TODO: Generate code to handle invariants
		
		transitions.add(new EngineTransition(c, null, "emStepSucceeded"));
		
		return transitions;
	}

	@Override
	public void registerCodeHandlers() throws CompilerException {
		// TODO: add bcode handler for invariants
		register(new AssertionHandler(), CodeType.U, "Rule", "AssertRule", null);
	}

}
