package org.coreasm.aspects.eclipse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.coreasm.aspects.AopASMPlugin;
import org.coreasm.aspects.pointcutmatching.AspectASTNode;
import org.coreasm.eclipse.editors.ASMEditor;
import org.coreasm.engine.CoreASMEngine.EngineMode;
import org.coreasm.engine.EngineException;
import org.coreasm.engine.VersionInfo;
import org.coreasm.engine.informationHandler.IInformationDispatchObserver;
import org.coreasm.engine.informationHandler.InformationDispatcher;
import org.coreasm.engine.informationHandler.InformationObject;
import org.coreasm.engine.informationHandler.InformationObject.VerbosityLevel;
import org.coreasm.engine.plugin.ExtensionPointPlugin;
import org.coreasm.engine.plugin.InitializationFailedException;
import org.coreasm.engine.plugin.PackagePlugin;
import org.coreasm.engine.plugin.Plugin;
import org.coreasm.util.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.MarkerUtilities;

public class AopASMEclipsePlugin extends Plugin implements
		ExtensionPointPlugin, PackagePlugin, IInformationDispatchObserver {

	public static final VersionInfo VERSION_INFO = new VersionInfo(0, 0, 1, "alpha");

	ASMEditor editor;

	@Override
	public void initialize() throws InitializationFailedException {
		InformationDispatcher.addObserver(this);
		Logger.log(Logger.INFORMATION, Logger.parser, this.getClass().getClassLoader().toString()+"<<<<<");
	}

	@Override
	public VersionInfo getVersionInfo() {
		return VERSION_INFO;
	}

	@Override
	public Set<String> getDependencyNames() {
		Set<String> dependencies = new HashSet<String>();
		dependencies.add("AopASMPlugin");
		return dependencies;
	}

	@Override
	public Map<EngineMode, Integer> getTargetModes() {
		HashMap<EngineMode, Integer> targetModes = new HashMap<EngineMode, Integer>();
		// all EngineModes in alphabetical order
//		targetModes.put(EngineMode.emAggregation, 10);
		targetModes.put(EngineMode.emIdle, 10);
//		targetModes.put(EngineMode.emInitializingState, 10);
//		targetModes.put(EngineMode.emInitKernel, 10);
//		targetModes.put(EngineMode.emLoadingCatalog, 10);
//		targetModes.put(EngineMode.emLoadingCorePlugins, 10);
//		targetModes.put(EngineMode.emParsingHeader, 10);
//		targetModes.put(EngineMode.emParsingSpec, 10);
//		targetModes.put(EngineMode.emPreparingInitialState, 10);
//		targetModes.put(EngineMode.emRunningAgents, 10);
//		targetModes.put(EngineMode.emSelectingAgents, 10);
//		targetModes.put(EngineMode.emStartingStep, 10);
//		targetModes.put(EngineMode.emStepSucceeded, 10);
//		targetModes.put(EngineMode.emTerminated, 10);
//		targetModes.put(EngineMode.emTerminating, 10);
//		// the failure modes of the engine
//		targetModes.put(EngineMode.emError, 10);
//		targetModes.put(EngineMode.emStepFailed, 10);
//		targetModes.put(EngineMode.emUpdateFailed, 10);
		return targetModes;
	}

	@Override
	public Map<EngineMode, Integer> getSourceModes() {
		HashMap<EngineMode, Integer> sourceModes = new HashMap<EngineMode, Integer>();
		// all EngineModes in alphabetical order
//		sourceModes.put(EngineMode.emAggregation, 10);
//		sourceModes.put(EngineMode.emIdle, 10);
//		sourceModes.put(EngineMode.emInitializingState, 10);
//		sourceModes.put(EngineMode.emInitKernel, 10);
//		sourceModes.put(EngineMode.emLoadingCatalog, 10);
//		sourceModes.put(EngineMode.emLoadingCorePlugins, 10);
		sourceModes.put(EngineMode.emParsingHeader, 10);
//		sourceModes.put(EngineMode.emParsingSpec, 10);
//		sourceModes.put(EngineMode.emPreparingInitialState, 10);
//		sourceModes.put(EngineMode.emRunningAgents, 10);
//		sourceModes.put(EngineMode.emSelectingAgents, 10);
//		sourceModes.put(EngineMode.emStartingStep, 10);
//		sourceModes.put(EngineMode.emStepSucceeded, 10);
//		sourceModes.put(EngineMode.emTerminated, 10);
//		sourceModes.put(EngineMode.emTerminating, 10);
//		// the failure modes of the engine
//		sourceModes.put(EngineMode.emError, 10);
//		sourceModes.put(EngineMode.emStepFailed, 10);
//		sourceModes.put(EngineMode.emUpdateFailed, 10);
		return sourceModes;
	}

	@Override
	public void fireOnModeTransition(EngineMode source, EngineMode target)
			throws EngineException {
	}

	@Override
	public void informationCreated(InformationObject information) {
		if ( information.getVerbosity() == VerbosityLevel.ERROR )
			capi.error(information.toString());
		else if( information.getVerbosity() == VerbosityLevel.INFO ){
			capi.warning("AopBla",information.getObjectOfInterest().toString());
			if (information.getObjectOfInterest() instanceof AspectASTNode)
				createMarker(information);
		}
		else
			capi.warning(this.getName(), information.toString());
	}

	private void createMarker(final InformationObject information){
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				if (window.getActivePage().getActiveEditor() instanceof ASMEditor)
					editor = (ASMEditor) window.getActivePage()
							.getActiveEditor();

				AspectASTNode astnode = null;
				if ( information.getObjectOfInterest() instanceof AspectASTNode )
					astnode = (AspectASTNode)information.getObjectOfInterest();

				int line = 0;
				try {
					line = editor.getInputDocument().getLineOfOffset(astnode.getScannerInfo().charPosition);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}

				Map<String, Object> map = new HashMap<String, Object>();
				MarkerUtilities.setLineNumber(map, line);
				MarkerUtilities.setMessage(map, information.getMessage());
				MarkerUtilities.setCharStart(map, astnode.getScannerInfo().charPosition);
				MarkerUtilities.setCharEnd(map, astnode.getScannerInfo().charPosition + 6);
				map.put(IMarker.LOCATION, editor.getInputFile().getFullPath().toString());
				map.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
				map.put("data", astnode);/**TODO serialization for all nodes*/
				try {
					MarkerUtilities.createMarker(editor.getInputFile(), map, "org.coreasm.aspects.eclipse.AopASMEclipseErrorMarker");
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void clearInformation() {
		// TODO Auto-generated method stub
		capi.warning(this.getName(), "clear Information");
	}

	private Set<String> names = null;


	@Override
	public Set<String> getEnclosedPluginNames() {
		names = new HashSet<String>();
		names.add("AopASMPlugin");
		names.add("AopASMEclipsePlugin");
		return names;
	}
}
