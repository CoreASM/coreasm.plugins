package org.coreasm.aspects.eclipse.ui.views;

import java.util.Observable;
import java.util.Observer;

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
import org.eclipse.ui.part.ViewPart;

import org.coreasm.aspects.eclipse.ui.XReference;
import org.coreasm.aspects.eclipse.ui.providers.TreeObject;
import org.coreasm.aspects.eclipse.ui.providers.XReferenceContentProvider;
import org.coreasm.aspects.eclipse.ui.providers.XReferenceLabelProvider;
import org.coreasm.eclipse.editors.ASMEditor;
import org.coreasm.eclipse.editors.ui.ILinkedWithASMEditorView;
import org.coreasm.eclipse.editors.ui.LinkWithEditorPartListener;

/**
 * @author Tobias
 *
 * This class represents the Cross Reference View
 * 
 * Implements Observer to get notified after parsing is finished,
 * ILinkedWithASMEditorView to get notified when editor has changed (e.g when switching between files)
 */
public class XReferenceView extends ViewPart implements ILinkedWithASMEditorView, ISelectionChangedListener, Observer {

	private TreeViewer viewer;
	private IPartListener2 linkWithEditorPartListener  = new LinkWithEditorPartListener(this);
	private ASMEditor asmEditor = null;
	
	@Override
	public void createPartControl(Composite parent) {
		// Create viewer
	    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	    viewer.setContentProvider(new XReferenceContentProvider());
	    viewer.setLabelProvider(new XReferenceLabelProvider());	    
		viewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		viewer.addSelectionChangedListener(this);
		
		getSite().setSelectionProvider(viewer);
		getSite().getPage().addPartListener(linkWithEditorPartListener);	// to get called when editor is activated
	}
	
	@Override
	public void editorActivated(IEditorPart activeEditor) {
		if (activeEditor instanceof ASMEditor){	
			
			// add/remove from parser observer
			if (asmEditor != null) {
				if (asmEditor.equals((ASMEditor)activeEditor))
					return;
			
				asmEditor.getParser().deleteObserver(this);
			}
			asmEditor = (ASMEditor)activeEditor;
			asmEditor.getParser().addObserver(this);
			
			
			// refresh the view
			refresh();
		}	
	}
	
    /**
     * Resets the input for the viewer
     */
    public void refresh() {
		//has to be synchronous
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (asmEditor != null)
					viewer.setInput(XReference.getTreeForFile(asmEditor.getPartName()));
				
				XReference.resetTree();
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
		
		if (asmEditor != null)
			asmEditor.getParser().deleteObserver(this);
		
		getSite().getPage().removePartListener(linkWithEditorPartListener);
		
		linkWithEditorPartListener = null;
		asmEditor = null;
		
		viewer.getTree().dispose();
		if (viewer.getContentProvider() != null)
			viewer.getContentProvider().dispose();
		viewer.getLabelProvider().dispose();
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		refresh();
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
}
