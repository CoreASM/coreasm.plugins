package org.coreasm.aspects.eclipse.ui.views;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.editors.text.TextEditor;

import org.coreasm.aspects.eclipse.ui.XReference;
import org.coreasm.aspects.eclipse.ui.providers.TreeObject;
import org.coreasm.aspects.eclipse.ui.providers.XReferenceContentProvider;
import org.coreasm.aspects.eclipse.ui.providers.XReferenceLabelProvider;

/**
 * @author Tobias
 *
 * This class represents the Cross Reference View
 * 
 * Implements Observer to get notified after parsing is finished,
 * ILinkedWithASMEditorView to get notified when editor has changed (e.g when switching between files)
 */
public class XReferenceView extends ViewPart implements IPartListener2, ISelectionChangedListener {

	private TreeViewer viewer;
	private TextEditor asmEditor = null;
	
	@Override
	public void createPartControl(Composite parent) {
		// set the view
		XReference.xRefView = this;
		
		// Create viewer
	    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	    viewer.setContentProvider(new XReferenceContentProvider());
	    viewer.setLabelProvider(new XReferenceLabelProvider());	    
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		viewer.addSelectionChangedListener(this);
		
		getSite().setSelectionProvider(viewer);
		getSite().getPage().addPartListener(this);	// to get called when editor is activated
	}
	
    /**
     * Resets the input for the viewer
     */
    public void refresh() {
		//has to be synchronous
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (asmEditor != null) {
					viewer.setInput(XReference.getTreeForFile(asmEditor.getPartName()));
				}
			}
		});	
	}
	
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		
		getSite().getPage().removePartListener(this);

		asmEditor = null;
		
		viewer.getTree().dispose();
		if (viewer.getContentProvider() != null)
			viewer.getContentProvider().dispose();
		viewer.getLabelProvider().dispose();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		
		if (selection.isEmpty()) {
			asmEditor.resetHighlightRange();
		} else {
			// highlight the line in the editor
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object ele = sel.getFirstElement();
			
			if (ele instanceof TreeObject) {
				int pos = (int) ((TreeObject) ele).getPos();
				if (pos != 0)
					asmEditor.setHighlightRange(pos, 1, true);
			}
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IEditorPart activeEditor = partRef.getPage().getActiveEditor();
		if (activeEditor != null){	
			
			// add/remove from parser observer
			if (asmEditor != null) {
				if (asmEditor.equals((TextEditor)activeEditor))
					return;

			}
			asmEditor = (TextEditor)activeEditor;
			
			// set the filename in logic part
			XReference.currentFileName = asmEditor.getPartName();
			
			// refresh the view
			refresh();
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}
}
