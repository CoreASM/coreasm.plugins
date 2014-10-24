package org.coreasm.aspects.eclipse.ui.providers;

import java.util.HashMap;
import java.util.Map;

import org.coreasm.eclipse.util.Utilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.progress.UIJob;

public class CrossReferenceContentProvider implements ITreeContentProvider {
	private final Map<IFile, CrossReferenceNode> roots = new HashMap<IFile, CrossReferenceNode>();
	private Viewer viewer;

	public void addCrossReference(final Map<String, String> data) {
		IFile file = Utilities.getFile(data.get("file"));
		CrossReferenceNode root = roots.get(file);
		if (root == null) {
			root = new CrossReferenceNode(file);
			roots.put(file, root);
		}
		IFile aspectFile = Utilities.getFile(data.get("aspectFile"));
		IFile adviceFile = Utilities.getFile(data.get("adviceFile"));
		IFile parentFile = Utilities.getFile(data.get("parentFile"));
		CrossReferenceNode aspect = root.addChild(new CrossReferenceNode(data.get("aspect"), "icons/aspect.gif", aspectFile, Integer.parseInt(data.get("aspectLine")), Integer.parseInt(data.get("aspectColumn"))));
		CrossReferenceNode advice = aspect.addChild(new CrossReferenceNode(data.get("advice"), "icons/advice.gif", adviceFile, Integer.parseInt(data.get("adviceLine")), Integer.parseInt(data.get("adviceColumn"))));
		CrossReferenceNode advices = advice.addChild(new CrossReferenceNode("advices", "icons/arrow.gif"));
		advices.addChild(new CrossReferenceNode(data.get("function"), null, file, Integer.parseInt(data.get("line")), Integer.parseInt(data.get("column"))));

		CrossReferenceNode parent = root.addChild(new CrossReferenceNode(data.get("parent"), "icons/rule.gif", parentFile, Integer.parseInt(data.get("parentLine")), Integer.parseInt(data.get("parentColumn"))));
		CrossReferenceNode function = parent.addChild(new CrossReferenceNode(data.get("function"), null, file, Integer.parseInt(data.get("line")), Integer.parseInt(data.get("column"))));
		CrossReferenceNode advicesBy = function.addChild(new CrossReferenceNode("adviced by", "icons/arrow.gif"));
		advicesBy.addChild(new CrossReferenceNode(data.get("advice"), "icons/advice.gif", adviceFile, Integer.parseInt(data.get("adviceLine")), Integer.parseInt(data.get("adviceColumn"))));

		new UIJob("Updating Cross References") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (viewer != null && !viewer.getControl().isDisposed())
					viewer.refresh();
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	public void clearTree(String filename) {
		roots.remove(Utilities.getFile(filename));
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFile) {
			CrossReferenceNode root = roots.get(parentElement);
			if (root == null) {
				root = new CrossReferenceNode((IFile)parentElement);
				root.addChild(new CrossReferenceNode("Cross references are showing after parsing", "aspect.gif"));
			}
			return getChildren(roots.get(parentElement));
		}
		if (parentElement instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput)parentElement;
			return getChildren(input.getFile());
		}
		if (!(parentElement instanceof CrossReferenceNode))
			return new Object[0];
		CrossReferenceNode parent = (CrossReferenceNode)parentElement;
		return parent.getChildren().toArray();
	}

	@Override
	public Object getParent(Object element) {
		if (!(element instanceof CrossReferenceNode))
			return null;
		CrossReferenceNode node = (CrossReferenceNode)element;
		return node.getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof CrossReferenceNode))
			return false;
		CrossReferenceNode node = (CrossReferenceNode)element;
		return node.hasChildren();
	}
}
