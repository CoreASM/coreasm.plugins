package org.coreasm.plugins.jung2;

import java.util.HashMap;

import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;

public class GraphModel {

	private GraphViewer viewer;

	private DelegateForest<Vertex, Integer> forest;
	private HashMap<String, Vertex> vertices = new HashMap<String, Vertex>();
	private int edgeNumber = 0;

	public GraphModel(){
		DirectedGraph<Vertex, Integer > graph = new DirectedOrderedSparseMultigraph<Vertex, Integer>();
		forest = new DelegateForest<Vertex, Integer>(graph);
		viewer = new GraphViewer(forest);
	}

	public GraphViewer getViewer(){
		return viewer;
	}

	public Forest<Vertex, Integer> getForest(){
		return forest;
	}


	public void addEdge(String parentId, String childId){
		forest.addEdge(edgeNumber ++, vertices.get(parentId), vertices.get(childId));
		this.getViewer().updateLayout();
	}

	public void addVertex(String id, String label, String tooltip) {
		Vertex vertex = new Vertex(id, label, tooltip);
		forest.addVertex(vertex);
		vertices.put(id, vertex);
		this.getViewer().updateLayout();
	}

	public void addVertex(String id, String label) {
		addVertex(id, label, "");
	}

	//TODO should also remove nodes from folded nodes
	public void removeVertex(String vertexId, boolean descandants) {
		Vertex vertex;
		vertex = vertices.get(vertexId);
		if (vertex != null){
			if (forest.getChildren(vertex) != null)
				if (descandants) {
					for (Vertex c : forest.getChildren(vertex)) {
						removeVertex(c.getId(), descandants);
					};
				}
			vertices.remove(vertexId);
			forest.removeVertex(vertex, false);
		}
		this.getViewer().updateLayout();
	}

	public Vertex getVertex(String vertexId) {
		return vertices.get(vertexId);
	}

	public void removeEdge(String parentId, String childId, boolean descandants) {
		Vertex parent;
		Vertex child;
		parent = vertices.get(parentId);
		child = vertices.get(childId);

		if (parent != null && child != null){
				if (forest.findEdge(parent, child) != null ) 
					forest.removeEdge(forest.findEdge(parent, child), false);
				if (descandants){
					removeVertex(child.getId(), true);
				}
		}
		this.getViewer().updateLayout();
	}

	public void clearForest() {
		for (Vertex vertex : vertices.values()) {
			forest.removeVertex(vertex);
		}
		vertices.clear();
		this.getViewer().updateLayout();
	}


}
