package org.coreasm.aspects.eclipse.ui.providers;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author Tobias
 *
 * The content provider for the tree of cross references
 * Basic implementation of a content provider
 */
public class XReferenceContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TreeObject)
			return ((TreeObject)parentElement).getChildrenAsArray();
		
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TreeObject)
			return ((TreeObject)element).getParent();
		
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TreeObject)
			return ((TreeObject)element).hasChildren();

		return false;
	}
}
