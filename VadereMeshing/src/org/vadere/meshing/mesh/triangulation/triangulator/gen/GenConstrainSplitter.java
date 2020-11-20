package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.SpaceFillingCurve;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GenConstrainSplitter<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {

	private final static Logger logger = Logger.getLogger(GenConstrainSplitter.class);
	private final IIncrementalTriangulation<V, E, F> triangulation;
	private final Collection<VLine> constrains;
	private final Collection<Pair<V, V>> vConstrains;
	private final Set<E> eConstrains;
	private final Collection<IPoint> points;
	private final Map<V, VLine> projectionMap;
	private boolean generated;
	private final double tol;
	private final GenSpaceFillingCurve<V, E, F> sfc;


	public GenConstrainSplitter(
			@NotNull final Supplier<IMesh<V, E, F>> meshSupply,
			@NotNull final PSLG pslg,
			final double tol) {
		this(new IncrementalTriangulation<>(meshSupply.get(), pslg.getBoundingBox()), pslg, tol);
	}

	public GenConstrainSplitter(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final PSLG pslg,
			final double tol) {
		this(triangulation, pslg.getAllSegments(), tol, null);
	}

	public GenConstrainSplitter(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Collection<VLine> constrains,
			final double tol) {
		this(triangulation, constrains, tol, null);
	}

	public GenConstrainSplitter(
			@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
			@NotNull final Collection<VLine> constrains,
			final double tol,
			@Nullable final GenSpaceFillingCurve<V, E, F> sfc) {
		this.constrains = constrains;
		this.points = Collections.EMPTY_LIST;
		this.vConstrains = new ArrayList<>(constrains.size());
		this.eConstrains = new HashSet<>(constrains.size());
		this.projectionMap = new HashMap<>();
		this.tol = tol;
		this.sfc = sfc;

		/**
		 * This prevent the flipping of constrained edges
		 */
		Predicate<E> canIllegal = e -> !eConstrains.contains(e) && !eConstrains.contains(getMesh().getTwin(e));
		this.triangulation = triangulation;
		this.triangulation.setCanIllegalPredicate(canIllegal);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!generated) {
			insertPoints();
			for(Pair<V, V> constrain : vConstrains) {
				enforceConstrain(constrain);
			}
			generated = true;
		}

		for(Pair<V, V> constrain : vConstrains) {
			V v1 = constrain.getLeft();
			V v2 = constrain.getRight();
			for(E e : getMesh().getEdgeIt(v1)) {
				if(getMesh().getTwinVertex(e).equals(v2)) {
					eConstrains.add(e);
					eConstrains.add(getMesh().getTwin(e));
					break;
				}
			}
		}

		if(finalize) {
			getTriangulation().finish();
		}

		return triangulation;
	}

	public Collection<E> getConstrains() {
		return eConstrains;
	}

	public Map<V, VLine> getProjections() {
		return projectionMap;
	}

	private void enforceConstrain(@NotNull final Pair<V, V> constrain) {
		V v1 = constrain.getLeft();
		V v2 = constrain.getRight();

		// this may also contain co-linear edges!
		LinkedList<E> intersectingEdges = triangulation.getIntersectingEdges(v1, v2);

		for(E edge : intersectingEdges) {
			VLine vEdge = triangulation.getMesh().toLine(edge);
			V v11 = getMesh().getVertex(edge);
			V v22 = getMesh().getVertex(getMesh().getPrev(edge));

			// to be save, TODO inconsistent geometry check which may lead to a deadlock (isLeftOfRobust is "robust" while intersectLineSegment is not")
			if(GeometryUtils.intersectLineSegment(vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, v1.getX(), v1.getY(), v2.getX(), v2.getY())) {
				//E next = triangulation.getMesh().getNext(edge);
				//E prev = triangulation.getMesh().getPrev(edge);
				//E twin = triangulation.getMesh().getTwin(edge);
				VPoint intersectionPoint = GeometryUtils.intersectionPoint(vEdge.x1, vEdge.y1, vEdge.x2, vEdge.y2, v1.getX(), v1.getY(), v2.getX(), v2.getY());

				if(intersectionPoint.distanceSq(v11) <= tol * tol) {
					//getMesh().setPoint(v11, intersectionPoint);
					projectionMap.put(v11, vEdge);
				} else if(intersectionPoint.distanceSq(v22) <= tol * tol) {
					//getMesh().setPoint(v22, intersectionPoint);
					projectionMap.put(v22, vEdge);
				} else {
					V vertex = getMesh().createVertex(intersectionPoint);
					triangulation.splitEdge(vertex, edge, false);
					projectionMap.put(vertex, vEdge);
				}
			} else {
				// the edge is co-linear that means we add the complete edge to be part of the constrains or one vertex is very close to (v1--v2).
				/*if(GeometryUtils.distanceToLineSegment(v1, v2, v11.getX(), v11.getY()) <= tol) {
					VPoint projection1 = GeometryUtils.projectOntoLine(v11.getX(), v11.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());
					getMesh().setPoint(v11, projection1);
					projectionMap.put(v11, vEdge);
				}

				if(GeometryUtils.distanceToLineSegment(v1, v2, v22.getX(), v22.getY()) <= tol) {
					VPoint projection2 = GeometryUtils.projectOntoLine(v22.getX(), v22.getY(), v1.getX(), v1.getY(), v2.getX(), v2.getY());
					getMesh().setPoint(v22, projection2);
					projectionMap.put(v22, vEdge);
				}*/
			}
		}
	}

	private void insertPoints() {
		triangulation.init();
		IMesh<V, E, F> mesh = triangulation.getMesh();

		triangulation.insert(points);

		for(VLine constrain : constrains) {
			boolean insertPair = true;
			IPoint p1 = mesh.createPoint(constrain.x1, constrain.y1);
			IPoint p2 = mesh.createPoint(constrain.x2, constrain.y2);

			E edge1 = triangulation.insert(p1);
			V v1 = triangulation.getMesh().getVertex(edge1);
			// could not insertVertex p1
			if(!getMesh().getPoint(v1).equals(p1)) {
				logger.warn("could not insertVertex " + p1);
				insertPair = false;
			}

			E edge2 = triangulation.insert(p2);
			V v2 = triangulation.getMesh().getVertex(edge2);
			// could not insertVertex p2
			if(!getMesh().getPoint(v2).equals(p2)) {
				logger.warn("could not insertVertex " + p2);
				insertPair = false;
			}

			if(insertPair) {
				vConstrains.add(Pair.of(v1, v2));
			}
		}
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}
}
