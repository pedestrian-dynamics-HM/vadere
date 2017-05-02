package org.vadere.util.geometry.mesh.triangulations;


import org.apache.commons.lang3.tuple.Triple;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DelaunayTree<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> implements IPointLocator<P, E, F> {
	private DAG<DAGElement<P, F>> dag;
	private final HashMap<F, DAG<DAGElement<P, F>>> map;
	private final IMesh<P, E, F> mesh;
	private double eps = 0.0000001;

	public DelaunayTree(final ITriangulation<P, E, F> triangulation) {
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
	public Collection<F> locatePoint(final P point, final boolean insertion) {
		checkRoot();


		Set<DAG<DAGElement<P, F>>> leafs = new HashSet<>();
		LinkedList<DAG<DAGElement<P, F>>> nodesToVisit = new LinkedList<>();
		nodesToVisit.add(dag);

		while(!nodesToVisit.isEmpty()) {
			DAG<DAGElement<P, F>> currentNode = nodesToVisit.removeLast();
			if(currentNode.getElement().getTriangle().isPartOf(point, eps)) {
				if(currentNode.isLeaf() && !mesh.isDestroyed(currentNode.getElement().getFace())) {
					leafs.add(currentNode);

					// if we are not interested in insertion we just want to find one triangle.
					if(!insertion) {
						return leafs.stream().map(dag -> dag.getElement().getFace()).collect(Collectors.toList());
					}
				}
				else {
					nodesToVisit.addAll(currentNode.getChildren());
				}
			}
		}

		return leafs.stream().map(dag -> dag.getElement().getFace()).collect(Collectors.toList());
	}

	@Override
	public Optional<F> locate(final P point) {
		checkRoot();
		Optional<F> optFace = locatePoint(point, false).stream().findAny();
		if(optFace.isPresent()) {
			return Optional.of(optFace.get());
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public void splitFaceEvent(F original, F[] faces) {
		checkRoot();
		DAG<DAGElement<P, F>> faceDag = map.remove(original);
		for(F face : faces) {
			List<P> points = mesh.getVertices(face);
			DAG<DAGElement<P, F>> newFaceDag = new DAG<>(new DAGElement(face, Triple.of(points.get(0), points.get(1), points.get(2))));
			faceDag.addChild(newFaceDag);
			map.put(face, newFaceDag);
		}
	}

	@Override
	public void flipEdgeEvent(final F f1, final F f2) {
		checkRoot();
		DAG<DAGElement<P, F>> f1Dag = map.remove(f1);
		DAG<DAGElement<P, F>> f2Dag = map.remove(f2);
		List<P> points1 = mesh.getVertices(f1);
		List<P> points2 = mesh.getVertices(f2);

		DAG<DAGElement<P, F>> newf1Dag = new DAG<>(new DAGElement(f1, Triple.of(points1.get(0), points1.get(1), points1.get(2))));
		DAG<DAGElement<P, F>> newf2Dag = new DAG<>(new DAGElement(f2, Triple.of(points2.get(0), points2.get(1), points2.get(2))));

		f1Dag.addChild(newf1Dag);
		f1Dag.addChild(newf2Dag);

		f2Dag.addChild(newf1Dag);
		f2Dag.addChild(newf2Dag);

		map.put(f1, newf1Dag);
		map.put(f2, newf2Dag);
	}

	@Override
	public void insertEvent(E vertex) {}

	@Override
	public void deleteBoundaryFace(F face) {
		checkRoot();
		assert mesh.isBoundary(face);
		map.remove(face);
	}
}
