package org.coreasm.aspects.eclipse;

import org.coreasm.util.information.InformationDispatcher;
import org.coreasm.util.information.InformationObject;
import org.coreasm.util.information.InformationObject.VerbosityLevel;
import org.coreasm.util.information.InformationObserver;

public class AopASMEclipsePlugin implements InformationObserver {

	public AopASMEclipsePlugin() {
		InformationDispatcher.addObserver(this);
	}

	@Override
	public void informationCreated(InformationObject information) {
		if ( information.getVerbosity() == VerbosityLevel.ERROR )
			System.err.println(information.toString());
		else if( information.getVerbosity() == VerbosityLevel.INFO )
			System.out.println("AopBla:" + information.getMessage());
		else
			System.out.println("AopASMEclipsePlugin:" + information);
	}

	private void createMarker(final InformationObject information){
//		Display.getDefault().asyncExec(new Runnable() {
//
//			@Override
//			public void run() {
//				IWorkbenchWindow window = PlatformUI.getWorkbench()
//						.getActiveWorkbenchWindow();
//				if (window.getActivePage().getActiveEditor() instanceof ASMEditor)
//					editor = (ASMEditor) window.getActivePage()
//							.getActiveEditor();
//
//				AspectASTNode astnode = null;
//				if ( information.getObjectOfInterest() instanceof AspectASTNode )
//					astnode = (AspectASTNode)information.getObjectOfInterest();
//
//				int line = 0;
//				try {
//					line = editor.getInputDocument().getLineOfOffset(astnode.getScannerInfo().charPosition);
//				} catch (BadLocationException e) {
//					e.printStackTrace();
//				}
//
//				Map<String, Object> map = new HashMap<String, Object>();
//				MarkerUtilities.setLineNumber(map, line);
//				MarkerUtilities.setMessage(map, information.getMessage());
//				MarkerUtilities.setCharStart(map, astnode.getScannerInfo().charPosition);
//				MarkerUtilities.setCharEnd(map, astnode.getScannerInfo().charPosition + 6);
//				map.put(IMarker.LOCATION, editor.getInputFile().getFullPath().toString());
//				map.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
//				map.put("data", astnode);/**TODO serialization for all nodes*/
//				try {
//					MarkerUtilities.createMarker(editor.getInputFile(), map, "org.coreasm.aspects.eclipse.AopASMEclipseErrorMarker");
//				} catch (CoreException e) {
//					e.printStackTrace();
//				}
//			}
//		});
	}

	@Override
	public void clearInformation() {
		// TODO Auto-generated method stub
		System.out.println("AopASMEclipsePlugin:" + "clear Information");
	}
}
