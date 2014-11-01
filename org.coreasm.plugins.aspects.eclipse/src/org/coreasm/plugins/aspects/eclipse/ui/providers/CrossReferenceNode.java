package org.coreasm.plugins.aspects.eclipse.ui.providers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.coreasm.eclipse.util.IconManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.FrameworkUtil;

public class CrossReferenceNode {
	private final String description;
	private final Image image;
	private final int line;
	private final int column;
	private final IFile file;

	private Object parent;
	private final Map<CrossReferenceNode, CrossReferenceNode> children = new LinkedHashMap<CrossReferenceNode, CrossReferenceNode>();

	public CrossReferenceNode(String description, String image, IFile file, int line, int column) {
		this.description = description;
		if (image != null)
			this.image = IconManager.getIcon(FileLocator.find(FrameworkUtil.getBundle(getClass()), new Path(image), null));
		else
			this.image = null;
		this.file = file;
		this.line = line;
		this.column = column;
	}

	public CrossReferenceNode(String description, String image) {
		this(description, image, null, -1, -1);
	}

	public CrossReferenceNode(IFile parentFile) {
		this(null, null, parentFile, -1, -1);
		parent = parentFile;
	}

	public CrossReferenceNode addChild(CrossReferenceNode child) {
		child.parent = this;
		CrossReferenceNode node = children.get(child);
		if (node != null)
			return node;
		children.put(child, child);
		return child;
	}

	public boolean isNodeOfTheAST() {
		return line >= 0 && column >= 0;
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	public String getDescription() {
		return description;
	}

	public Image getImage() {
		return image;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public IFile getParentFile() {
		if (file != null)
			return file;
		if (parent instanceof IFile)
			return (IFile)parent;
		if (parent instanceof CrossReferenceNode)
			return ((CrossReferenceNode)parent).getParentFile();
		return null;
	}

	public Object getParent() {
		return parent;
	}

	public Set<CrossReferenceNode> getChildren() {
		return children.keySet();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + column;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + line;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrossReferenceNode other = (CrossReferenceNode) obj;
		if (column != other.column)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (line != other.line)
			return false;
		return true;
	}
}
