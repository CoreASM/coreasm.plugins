package org.coreasm.aspects.eclipse.ui.providers;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class CrossReferenceLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();

		if (element instanceof CrossReferenceNode) {
			CrossReferenceNode node = (CrossReferenceNode)element;
			cell.setImage(node.getImage());
			if (node.getDescription() != null)
				cell.setText(node.getDescription());
			else
				cell.setText("Unknown Node");
		}

		super.update(cell);
	}
}
