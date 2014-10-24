package org.coreasm.aspects.eclipse.ui.views;

import org.coreasm.aspects.eclipse.AoASMEclipsePlugin;
import org.coreasm.aspects.eclipse.ui.providers.CrossReferenceLabelProvider;
import org.coreasm.aspects.eclipse.ui.providers.CrossReferenceNode;
import org.coreasm.eclipse.util.Utilities;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.ViewPart;

public class CrossReferenceView extends ViewPart implements IPartListener2, ISelectionChangedListener {
	private TreeViewer treeViewer;

	@Override
	public void createPartControl(Composite parent) {
	    treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	    treeViewer.setContentProvider(AoASMEclipsePlugin.getCrossReferenceContentProvider());
	    treeViewer.setLabelProvider(new CrossReferenceLabelProvider());
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		treeViewer.addSelectionChangedListener(this);

		getSite().setSelectionProvider(treeViewer);
		getSite().getPage().addPartListener(this);
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		getSite().getPage().removePartListener(this);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		IStructuredSelection sel = (IStructuredSelection) selection;
		Object element = sel.getFirstElement();
		try {
			if (element instanceof CrossReferenceNode) {
				CrossReferenceNode node = (CrossReferenceNode) element;
				if (node.isNodeOfTheAST()) {
					TextEditor editor = (TextEditor)Utilities.openEditor(node.getParentFile());
					IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
					editor.setHighlightRange(document.getLineOffset(node.getLine() - 1), 1, true);
				}
			}
		} catch (IllegalArgumentException exeption) {
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IEditorPart activeEditor = partRef.getPage().getActiveEditor();
		if (activeEditor instanceof TextEditor) {
			TextEditor textEditor = (TextEditor)activeEditor;
			treeViewer.setInput(textEditor.getEditorInput());
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}
}
