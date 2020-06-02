package org.vadere.meshing.mesh.gen;


import org.apache.commons.lang3.tuple.Triple;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An implementation of the so called Delaunay Tree which does not suppport deletion of points from the
 * triangulation {@link IIncrementalTriangulation}.
 *
 * The Delaunay Tree see Computational Geometry: Algorithms and Applications (berg-2008) page 191.
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class DelaunayTree<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IPointLocator<V, E, F> {
	private DAG<DAGElement<V, E, F>> dag;
	private final HashMap<F, DAG<DAGElement<V, E, F>>> map;
	private final IMesh<V, E, F> mesh;
	private double eps = 0.0000001;

	public DelaunayTree(final IIncrementalTriangulation<V, E, F> triangulation) {
		this.mesh = triangulation.getMesh();
		this.map = new HashMap<>();
	}

	private void checkRoot() {
		if(dag == null) {
			F face = mesh.getFace();

			if(mesh.isBoundary(face)) {
				face = mesh.getTwinFace(mesh.getEdge(face));
			}
			this.dag = new DAG<>(new DAGElement<>(face, mesh.toTriple(face)));
			this.map.put(face, dag);
		}
	}

	@Override
	public F locatePoint(final IPoint point) {
		checkRoot();

		Set<DAG<DAGElement<V, E, F>>> leafs = new HashSet<>();
		LinkedList<DAG<DAGElement<V, E, F>>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(dag);

		while(!nodesToVisit.isEmpty()) {
			DAG<DAGElement<V, E, F>> currentNode = nodesToVisit.removeLast();
			if(currentNode.getElement().getTriangle().isPartOf(point, eps)) {
				if(currentNode.isLeaf() && !mesh.isDestroyed(currentNode.getElement().getFace())) {
					leafs.add(currentNode);

					// if we are not interested in insertion we just want to find one triangle.
					return currentNode.getElement().getFace();
				}
				else {
					nodesToVisit.addAll(currentNode.getChildren());
				}
			}
		}

		throw new IllegalArgumentException(point + " is invalid, it can not be located by " + this);
	}

	@Override
	public Optional<F> locate(final IPoint point) {
		checkRoot();
		return Optional.of(locatePoint(point));
	}

    @Override
    public Optional<F> locate(double x, double y) {
        return locate(mesh.createPoint(x, y));
    }

	@Override
	public Type getType() {
		return Type.DELAUNAY_TREE;
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {
		DAG<DAGElement<V, E, F>> faceDag = map.remove(original);

		F face = f1;
		List<V> points1 = mesh.getVertices(face);
		DAG<DAGElement<V, E, F>> newFaceDag1 = new DAG<>(new DAGElement<V, E, F>(face, Triple.of(points1.get(0), points1.get(1), points1.get(2))));
		faceDag.addChild(newFaceDag1);
		map.put(face, newFaceDag1);

		face = f2;
		List<V> points2 = mesh.getVertices(face);
		DAG<DAGElement<V, E, F>> newFaceDag2 = new DAG<>(new DAGElement<V, E, F>(face, Triple.of(points2.get(0), points2.get(1), points2.get(2))));
		faceDag.addChild(newFaceDag2);
		map.put(face, newFaceDag2);

		face = f3;
		List<V> points3 = mesh.getVertices(face);
		DAG<DAGElement<V, E, F>> newFaceDag3 = new DAG<>(new DAGElement<V, E, F>(face, Triple.of(points3.get(0), points3.get(1), points3.get(2))));
		faceDag.addChild(newFaceDag3);
		map.put(face, newFaceDag3);
	}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {
		checkRoot();
		DAG<DAGElement<V, E, F>> faceDag = map.remove(original);

		F face = f1;
		List<V> points1 = mesh.getVertices(face);
		DAG<DAGElement<V, E, F>> newFaceDag1 = new DAG<>(new DAGElement<V, E, F>(face, Triple.of(points1.get(0), points1.get(1), points1.get(2))));
		faceDag.addChild(newFaceDag1);
		map.put(face, newFaceDag1);

		face = f2;
		List<V> points2 = mesh.getVertices(face);
		DAG<DAGElement<V, E, F>> newFaceDag2 = new DAG<>(new DAGElement<V, E, F>(face, Triple.of(points2.get(0), points2.get(1), points2.get(2))));
		faceDag.addChild(newFaceDag2);
		map.put(face, newFaceDag2);
	}

	@Override
	public void postFlipEdgeEvent(final F f1, final F f2) {
		checkRoot();
		DAG<DAGElement<V, E, F>> f1Dag = map.remove(f1);
		DAG<DAGElement<V, E, F>> f2Dag = map.remove(f2);
		List<V> points1 = mesh.getVertices(f1);
		List<V> points2 = mesh.getVertices(f2);

		DAG<DAGElement<V, E, F>> newf1Dag = new DAG<>(new DAGElement<V, E, F>(f1, Triple.of(points1.get(0), points1.get(1), points1.get(2))));
		DAG<DAGElement<V, E, F>> newf2Dag = new DAG<>(new DAGElement<V, E, F>(f2, Triple.of(points2.get(0), points2.get(1), points2.get(2))));

		f1Dag.addChild(newf1Dag);
		f1Dag.addChild(newf2Dag);

		f2Dag.addChild(newf1Dag);
		f2Dag.addChild(newf2Dag);

		map.put(f1, newf1Dag);
		map.put(f2, newf2Dag);
	}

	@Override
	public void postInsertEvent(V vertex) {}
}
