package org.coreasm.ast2spec.eclipse.popup.actions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;


public class Ast2SpecPopupMenu implements IObjectActionDelegate {

	Shell shell;
	IWorkbenchPart activePart;
	IFile currentlySelectedFile;
	ISelection currentSelection;
	IEditorPart openedEditor;
	File openedFile;


	/**
	 * Constructor for Action1.
	 */
	public Ast2SpecPopupMenu() {
		super();
		shell = null;
		activePart = null;
		currentlySelectedFile = null;
		currentSelection = null;
		openedEditor = null;
		openedFile = null;
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		activePart = targetPart;
		shell = targetPart.getSite().getShell();
	}

	/**
	 * opens the editor for the currently selected file and returns its IEditorPart
	 * @return editor for the currently select
	 */
	private IEditorPart openEditor(){
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				try {
					if (currentlySelectedFile.exists())
					openedEditor = IDE.openEditor(page,
							currentlySelectedFile, false);
				} catch (PartInitException e) {
				}
			}
		});
		return openedEditor;
	}
	/**
	 * close the editor
	 */
	private void closeEditor(){
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				if (openedEditor != null)
				page.closeEditor(openedEditor, false);
			}
		});
	}

	/**
	 * checks if an open editor for the selected file exists and sets the variable openEditor if existing
	 * @return
	 */
	private boolean existsOpenEditor(){
		IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (IEditorReference iEditorReference : editors) {
			if (iEditorReference.getPage().isEditorAreaVisible() && iEditorReference.getName().equals(currentlySelectedFile.getName()) ){
				openedEditor =  iEditorReference.getEditor(true);
				return true;
			}
		}
		return false;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 * 
	 *      open and close the selected CoreASM file and insert and remove the
	 *      use statement for this plugin
	 */
	public void run(IAction action) {

		//if the action has been started by editor's menu, than get the related iFile to continue
		if (activePart instanceof IEditorPart)
			setCurrentlySelectedFileFromActiveEditor();

		if (currentlySelectedFile.exists()) {
			openedFile = currentlySelectedFile.getRawLocation().makeAbsolute()
					.toFile();
			{
				if (existsOpenEditor()) {
					closeEditor();//editor should be closed before the input will be changed
					insertUseDeclarationIntoFile(openedFile, "Ast2SpecPlugin");
					openEditor();
					closeEditor();
					removeUseDeclarationFromFile(currentlySelectedFile
							.getRawLocation().makeAbsolute().toFile(),
							"Ast2SpecPlugin");
					openEditor(); //reopen the editor
				} else {
					insertUseDeclarationIntoFile(openedFile, "Ast2SpecPlugin");
					openEditor();
					closeEditor();
					removeUseDeclarationFromFile(currentlySelectedFile
							.getRawLocation().makeAbsolute().toFile(),
							"Ast2SpecPlugin");
				}
			}
		}
	}

	/**
	 * set the currently selected IFile currentlySelectedFile from the selected Editor
	 */
	private void setCurrentlySelectedFileFromActiveEditor() {
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				openedEditor = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage()
						.getActiveEditor();
				// get IFile from editor
				IFileEditorInput input = null;
				try {
					input = (IFileEditorInput) openedEditor
							.getEditorInput();
				} catch (ClassCastException ex) {

				}
				if (input != null)
					currentlySelectedFile = input.getFile();
			}
		});
	}

	/**
	 * returns the iFile handle to the selected file from the navigator component
	 *
	 * @param sel
	 * @return
	 */
	private IFile getSelection(ISelection sel) {
		  // If sel is not a structured selection just return.
	      if (!(sel instanceof IStructuredSelection))
	         return null;
	      IStructuredSelection structured = (IStructuredSelection)sel;
   		  if (structured.getFirstElement() instanceof IFile) {
   			 return (IFile) structured.getFirstElement();
   		   }else
   		return null;
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (getSelection(selection) instanceof IFile)
			currentlySelectedFile = getSelection(selection);
	}

	/**
	 * insert a use statement for this plugin at the first line,
	 * where a use statement can be found in the given file
	 *
	 * @param file
	 * @param pluginName
	 */
	public static void insertUseDeclarationIntoFile(File file,
			String pluginName) {

		if (pluginName != null && pluginName.length() > 0 && file.exists()) {

			List<String> lines = new ArrayList<String>();

			// read the file into lines
			BufferedReader r;
			try {
				r = new BufferedReader(new FileReader(file));
				String in;
				while ((in = r.readLine()) != null)
					lines.add(in);
				r.close();

				boolean inserted = false;
				// insert new line with use statement
				if (pluginName != null && pluginName.length() > 0) {
					for (int i = 0; i < lines.size(); i++) {
						//if the current line contains a use statement insert the new use statement
						//and shift the existing one to the next line
						if (!inserted && lines.get(i).matches(
								"^[\\s\\t]*use[\\t\\s]{1}[a-zA-Z]*")) {
							String newLine = "use " + pluginName;
							//add new line and shift all other elements to the "right"
							lines.add(i, newLine);
							inserted = true;
						}
					}
				}

				// write it back
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				for(int i=0;i<lines.size();i++){
					//write line into file
					bw.append(lines.get(i));
					//if the line is not the last line,
					//insert a newline
					if( i < ( lines.size() -1) )
						bw.newLine();
				}
				bw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else
			return;
	}

	/**
	 * remove the use statements for this plugin from the given file
	 *
	 * @param file
	 * @param pluginName
	 */
	public static void removeUseDeclarationFromFile(File file,
			String pluginName) {

		if (pluginName != null && pluginName.length() > 0 && file.exists()) {

			List<String> lines = new ArrayList<String>();

			// read the file into lines
			BufferedReader r;
			try {
				r = new BufferedReader(new FileReader(file));
				String in;
				while ((in = r.readLine()) != null)
					lines.add(in);
				r.close();

				// clean up/remove line if it contains the use statement
				for (int i = 0; i < lines.size();i++){
					if ( lines.get(i).matches("^[\\s\\t]*use[\\t\\s]{1}"+"Ast2SpecPlugin"))
					{
						lines.add(i, lines.get(i).replaceFirst("^[\\s\\t]*use[\\t\\s]{1}"+"Ast2SpecPlugin", ""));
						if (lines.get(i).length()==0)
							lines.remove(i);
						lines.remove(i);
					}
				}

				// write it back
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				for (String line : lines) {
					bw.append(line);
					bw.newLine();
				}
				bw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else
			return;
	}
}
