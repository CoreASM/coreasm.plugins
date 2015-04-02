package org.coreasm.plugins.jung2;

import org.coreasm.plugins.jung2.nodes.CollapseVertexNode;

import edu.uci.ics.jung.graph.DelegateForest;

public class GraphControler {

	private GraphModel model;

	public GraphControler(){
		model = new GraphModel();
	}

	public GraphModel getModel() {
		return model;
	}
}
