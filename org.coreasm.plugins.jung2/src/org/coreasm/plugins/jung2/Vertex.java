package org.coreasm.plugins.jung2;

import java.util.Calendar;

public class Vertex {

	private final long timeStamp;
	private final String id;
	private final String label;
	private String toolTip;

	public Vertex(String id, String label, String tooltip) {
		Calendar calobj = Calendar.getInstance();
		timeStamp = calobj.getTime().getTime();
		this.id = id;
		this.label = label;
		this.toolTip = tooltip;
	}

	public Vertex(String id, String label) {
		this(id, label, "");
	}

	public long getCreationTime() {
		return timeStamp;
	}

	public String getId() {
		return id;
	}
	public String getLabel() {
		return label;
	}
	@Override
	public String toString() {
		return label;
	}

	public String getTooltip() {
		return toolTip;
	}

}
