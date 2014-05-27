package org.coreasm.aspects.eclipse.ui.providers;

import java.util.ArrayList;

/**
 * @author Tobias
 *
 * Represents a tree node in the cross reference
 * Contains various members to describe the node
 * 
 * Consists mostly of getter and setter
 */
public class TreeObject {

	private String name;			// name of the node
	private String icon;			// path to the icon
	private ArrayList<TreeObject> children;	
	private TreeObject parent;		// the parent node
	private int pos;				// the position in the editor, so highlighting can be used 
	
	public TreeObject(String name) {
		this.name = name;
		children = new ArrayList<TreeObject>();
	}
	
	public TreeObject(String name, String icon) {
		this.name = name;
		this.icon = icon;
		children = new ArrayList<TreeObject>();
	}
	
	public TreeObject(String name, String icon, int pos) {
		this.name = name;
		this.icon = icon;
		this.pos = pos;
		children = new ArrayList<TreeObject>();
	}
	
	public TreeObject(String name, int pos) {
		this.name = name;
		this.pos = pos;
		children = new ArrayList<TreeObject>();
	}
	
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	public int getPos() {
		return pos;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * @param child
	 * 
	 * adds a new child to the list children and also
	 * sets the parent of the child to this
	 */
	public void addChild(TreeObject child) {
		children.add(child);
		child.setParent(this);
	}
	
	public void removeChild(TreeObject child) {
		children.remove(child);
		child.setParent(null);
	}
	
	public TreeObject getChild(String name) {
		for (TreeObject obj : children) {
			if (obj.getName().equals(name))
				return obj;
		}
		
		return null;
	}
	
	public TreeObject[] getChildren() {
		return (TreeObject[]) children.toArray(
			new TreeObject[children.size()]);
	}
	
	public boolean hasChildren() {
		return children.size() > 0;
	}
	
	public void setParent(TreeObject parent) {
		this.parent = parent;
	}
	
	public TreeObject getParent() {
		return parent;
	}
	
	public String toString() {
		return name;
	}
}