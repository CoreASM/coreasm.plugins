package org.coreasm.plugins.jung2;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.subLayout.TreeCollapser;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

public class GraphViewer {

	private JFrame frame = new JFrame("CoreASM Graph Viewer");

	private Layout<Vertex, Integer> layout;

	private VisualizationViewer<Vertex, Integer> vv;

	private DelegateForest<Vertex, Integer> graph;

	private final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();

	private TreeCollapser collapser;

	public GraphViewer(DelegateForest<Vertex, Integer> forest) {
		this.graph = forest;
		initLayout(forest);
		initVisualizationViewer();
		initGraph();
	}

	private Layout<Vertex, Integer> initLayout(Forest<Vertex, Integer> forest) {
		layout = new TreeLayout<Vertex, Integer>(forest);
		collapser = new TreeCollapser();
		return layout;
	}

	@SuppressWarnings("unchecked")
	private VisualizationViewer<Vertex, Integer> initVisualizationViewer() {
		vv = new VisualizationViewer<Vertex, Integer>(layout);
		// vv.setPreferredSize(new Dimension(600, 600));
		vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
		vv.getRenderContext().setVertexLabelTransformer(new Transformer() {

			@Override
			public Object transform(Object obj) {
				if (obj instanceof Vertex)
					return ((Vertex) obj).getLabel();
				else if (obj instanceof Tree) {
					String result = "";
					Tree tree = (Tree) obj;
					//sort tree by creation time
					TreeMap<Long, Vertex> sortedMap = new TreeMap();
					for (Object v : tree.getVertices()) {
						if (v instanceof Vertex) {
							Vertex vertex = (Vertex) v;
							sortedMap.put(vertex.getCreationTime(), vertex);
						}
					}
					//create string from sorted tree
					for (Entry<Long, Vertex> entry : sortedMap.entrySet()) {
						result += " " + entry.getValue().getLabel();
					}
					return result;
				}
				else
					return obj.toString();
			}
		});
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		vv.setVertexToolTipTransformer(new Transformer<Vertex, String>() {

			@Override
			public String transform(Vertex v) {
				return v.getTooltip();
			}
		});
		// add mouse interaction
		graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
		vv.setGraphMouse(graphMouse);
		vv.addKeyListener(graphMouse.getModeKeyListener());
		vv.setToolTipText("<html><center>Type 'p' for Pick mode<p>Type 't' for Transform mode");
		return vv;
	}

	private void initGraph() {
		Container content = frame.getContentPane();
		final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
		// zoom component
		final ScalingControl scaler = new CrossoverScalingControl();
		// add mouse wheel zoom
		panel.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				if (notches < 0) {
					scaler.scale(vv, 1.0f + (notches * 0.1f),
							vv.getMousePosition(true));
				} else {
					scaler.scale(vv, 1.0f - (notches * 0.1f),
							vv.getMousePosition(true));
				}
			}
		});

		//fold and expand tree nodes
		JButton fold = new JButton("Fold");
		fold.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Collection<Vertex> picked = new HashSet<Vertex>(vv.getPickedVertexState().getPicked());
				if (picked.size() == 1) {
					Object root = picked.iterator().next();
					foldVertex(root);
					vv.getPickedVertexState().clear();
				}
			}
		});

		JButton expand = new JButton("Expand");
		expand.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Collection<?> picked = vv.getPickedVertexState().getPicked();
				for (Object v : picked) {
					expandVertex(v);
					vv.getPickedVertexState().clear();
				}
			}
		});

		// fold/expand grid
		JPanel foldExpandGrid = new JPanel(new GridLayout(1, 0));
		foldExpandGrid.setBorder(BorderFactory.createTitledBorder("Fold/Expand"));
		foldExpandGrid.add(fold);
		foldExpandGrid.add(expand);

		// add zoom buttons
		JButton plus = new JButton("+");
		plus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scaler.scale(vv, 1.1f, vv.getCenter());
			}
		});
		JButton minus = new JButton("-");
		minus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scaler.scale(vv, 1 / 1.1f, vv.getCenter());
			}
		});
		content.add(panel);

		// scale grid
		JPanel scaleGrid = new JPanel(new GridLayout(1, 0));
		scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));
		scaleGrid.add(plus);
		scaleGrid.add(minus);


		//reset layout
		JButton reset = new JButton("reset layout");
		reset.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				vv.getRenderContext().getMultiLayerTransformer()
						.getTransformer(Layer.LAYOUT).setToIdentity();
				vv.getRenderContext().getMultiLayerTransformer()
						.getTransformer(Layer.VIEW).setToIdentity();
				updateLayout();
			}
		});

		JPanel controls = new JPanel();
		controls.add(scaleGrid);
		controls.add(foldExpandGrid);
		controls.add(reset);

		content.add(controls, BorderLayout.SOUTH);
		frame.setVisible(true);
	}

	synchronized void updateLayout() {
		try {
			initLayout(graph);
			layout.setInitializer(vv.getGraphLayout());
			LayoutTransition<Integer, Number> lt = new LayoutTransition(vv, vv.getGraphLayout(), layout);
			// Animator animator = new Animator(lt);
			// animator.run();
			while (!lt.done())
				lt.step();
			vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
			vv.setPreferredSize(layout.getSize());
			if(graph.getVertexCount() > 0)
				frame.setVisible(true);
			else
				frame.setVisible(false);
			vv.repaint();
			frame.pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void foldVertex(Object root) {
		DelegateForest<Vertex, Integer> inGraph = (DelegateForest<Vertex, Integer>) layout.getGraph();
		try {
			if (root != null)
				collapser.collapse(vv.getGraphLayout(), inGraph, root);
		}
		catch (InstantiationException e1) {
			e1.printStackTrace();
		}
		catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}
		vv.repaint();
	}

	public void expandVertex(Object v) {
		DelegateForest<Vertex, Integer> inGraph = (DelegateForest<Vertex, Integer>) layout.getGraph();
		if (v instanceof Forest) {
			collapser.expand(inGraph, (Forest<?, ?>) v);
		}
		else if (v instanceof Vertex) {
			v = getContainer((Vertex) v, inGraph);
			if (v != null)
				collapser.expand(inGraph, (Forest<?, ?>) v);
		}
		vv.repaint();
	}

	/**
	 * get Object from the Graph that contains the given vertex
	 * @param vertex
	 * @param container
	 * @return
	 */
	public Object getContainer(Vertex vertex, Object container) {
		Object result = null;
		if (container instanceof Tree) {
			Tree tree = (Tree) container;
			if (tree.containsVertex(vertex))
				result = tree;
			else {
				Iterator it = tree.getVertices().iterator();
				while (it.hasNext()) {
					Object elem = it.next();
					if (elem instanceof Tree)
						result = getContainer(vertex, (Tree) elem);
				}
			}
		}
		else if (container instanceof Forest) {
			Forest f = (Forest) container;
			for (Object t : f.getTrees()) {
				result = getContainer(vertex, (Tree) t);
			}
		}
		return result;

	}

}
