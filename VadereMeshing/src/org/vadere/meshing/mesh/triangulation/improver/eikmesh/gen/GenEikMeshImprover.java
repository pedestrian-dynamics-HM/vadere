package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenEikMeshImprover<P extends EikMeshPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IMeshImprover<P, CE, CF, V, E, F> {

	private final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private final IEdgeLengthFunction edgeLengthFunc;

	@Nullable private final IDistanceFunction distanceFunc;

	@Nullable private List<V> collapseVertices;
	@Nullable private List<E> splitEdges;
	private int nSteps;
	private boolean runParallel;
	private double deps;
	private Set<P> fixPoints;
	private Map<V, P> fixPointRelation;
	private double delta;

	public GenEikMeshImprover(@NotNull final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation) {
		this.triangulation = triangulation;
		this.edgeLengthFunc = p -> 1.0;
		this.distanceFunc = null;
		this.nSteps = 0;
		this.runParallel = false;
	}

	public GenEikMeshImprover(@NotNull final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation,
	                          @NotNull final IDistanceFunction distanceFunc,
	                          @NotNull final IEdgeLengthFunction edgeLengthFunc) {
		this.triangulation = triangulation;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.nSteps = 0;
		this.runParallel = false;
	}


	@Override
	public Collection<VTriangle> getTriangles() {
		return triangulation.streamTriangles().collect(Collectors.toList());
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public void improve() {
		removeBoundaryLowQualityTriangles();
		triangulation.smoothBoundary(distanceFunc);
		if(triangulation.isValid()) {
			flipEdges();
			removeTrianglesInsideHoles();
			removeTrianglesOutsideBBox();
			//removeBoundaryLowQualityTriangles();
		} else {
			triangulation.recompute();
			removeTrianglesOutsideBBox();
			removeTrianglesInsideObstacles();
		}
		double edgeLengthScaling = computeEdgeLengthScaling();
		Map<V, P> fixpointRelation = getFixPointRelation(fixPoints);
		setVertexForces(edgeLengthScaling, fixpointRelation);
		updateVertices();

		collapseEdges();
		splitEdges();

		nSteps++;
	}

	/**
	 * Computes and sets the force vector for each vertex. The force computation is highly sensitive to the
	 * <tt>scalingFactor</tt>.
	 *
	 * @param edgeLengthScaling a factor which transforms the relative edge scaling to an absolute value
	 */
	private void setVertexForces(final double edgeLengthScaling, @NotNull final Map<V, P> fixPointRelation) {
		streamVertices().forEach(v -> setForce(v, edgeLengthScaling, fixPointRelation));
	}

	/**
	 * Computes for each fix point in <tt>fixPoints</tt> the vertex which is closest to
	 * the fix point, i.e. the partial relation vertex -> fix point.
	 *
	 * @param fixPoints the set of fix points
	 * @return a partial relation V -> P i.e. vertex to fix point
	 */
	private Map<V, P> getFixPointRelation(@NotNull final Set<P> fixPoints) {
		Map<V, P> fixPointRelation = new HashMap<>();
		for(P fixPoint : fixPoints) {
			V closest = null;
			double distance = Double.MAX_VALUE;
			for(V vertex : getMesh().getVertices()) {
				if (closest == null || distance > vertex.distance(fixPoint)) {
					closest = vertex;
					distance = vertex.distance(fixPoint);
				}
			}
			fixPointRelation.put(closest, fixPoint);
		}
		return fixPointRelation;
	}

	/**
	 * Computes and sets the force for a specific vertex. The force computation is highly sensible to
	 * the <tt>scalingFactor</tt>. If the vertex is a vertex closest to a specific fix point the force
	 * will act towards this fix point and all other forces will be ignored.
	 *
	 * @param vertex            the specific vertex of interest
	 * @param edgeLengthScaling a factor which transforms the relative edge length to an absolute
	 * @param fixPointRelation  a partial relation of V -> P, i.e. vertex and its closest fix point.
	 */
	private void setForce(final V vertex, final double edgeLengthScaling, @NotNull final Map<V, P> fixPointRelation) {
		EikMeshPoint p1 = getMesh().getPoint(vertex);

		if(fixPointRelation.containsKey(vertex)) {
			P p2 = fixPointRelation.get(vertex);
			VPoint force = new VPoint((p2.getX() - p1.getX()), (p2.getY() - p1.getY())).scalarMultiply(1.0);
			p1.increaseVelocity(force);
			p1.increaseAbsoluteForce(force.distanceToOrigin());
		}
		else {
			for(V v2 : getMesh().getAdjacentVertexIt(vertex)) {
				EikMeshPoint p2 = getMesh().getPoint(v2);

				double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
				double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * edgeLengthScaling;

				double lenDiff = Math.max(desiredLen - len, 0);
				VPoint force = new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len));
				p1.increaseVelocity(force);
				p1.increaseAbsoluteForce(force.distanceToOrigin());
			}
		}
	}

	/**
	 * Computes and increases the force applied to the vertex of the half-edge by the half-edge.
	 *
	 * @param edge the half-edge
	 * @param edgeLengthScaling a factor which transforms the relative edge length to an absolute
	 */
	private void increaseForce(@NotNull final E edge, final double edgeLengthScaling) {
		EikMeshPoint p1 = getMesh().getPoint(edge);
		EikMeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

		double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
		double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * edgeLengthScaling;

		double lenDiff = Math.max(desiredLen - len, 0);
		p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len)));
	}


	/**
	 * projects the vertex back if it is no longer inside the boundary or inside an obstacle.
	 *
	 * @param vertex the vertex
	 */

	/**
	 * P
	 * @param vertex
	 * @param distance
	 * @param scale
	 * @param isAtBoundary
	 */
	private void projectBackVertex(@NotNull final V vertex, double distance, final double scale, final boolean isAtBoundary) {

		/*
		 * We only project back boundary vertices.
		 */
		if(isAtBoundary) {
			/*
			 * (1) compute the the back projection
			 */
			EikMeshPoint position = getMesh().getPoint(vertex);
			double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps, 0))) - distance) / deps;
			double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0, deps))) - distance) / deps;
			VPoint projection = new VPoint(dGradPX * distance * scale, dGradPY * distance * scale);

			/*
			 * (3.1) compute the new position of the vertex (after the back projection)
			 */
			VPoint newPosition = getMesh().toPoint(vertex).subtract(projection);


			/*
			 * (3.2) compute the angle at the vertex at the boundary
			 */
			E boundaryEdge = getMesh().getBoundaryEdge(vertex).get();
			VPoint p = getMesh().toPoint(vertex);
			VPoint q = getMesh().toPoint(getMesh().getNext(boundaryEdge));
			VPoint r = getMesh().toPoint(getMesh().getPrev(boundaryEdge));
			double angle = GeometryUtils.angle(r, p, q);

			/*
			 * (3) project back if the vertex is outside
			 */
			if(distance > 0) {
				/*if(GeometryUtils.isRightOf(r, p, newPosition)
						&& GeometryUtils.isRightOf(p, q, newPosition)
						&& GeometryUtils.isRightOf(q, r, newPosition)){
					position.subtract(projection);
				}*/
				position.subtract(projection);
			}
			// back projection to the outside
			else {
				/*
				 * (3.2) if the point is inside, only project back if the angle is smaller than 3
				 */
				if(angle > Math.PI || (GeometryUtils.isLeftOf(r, p, newPosition) && GeometryUtils.isLeftOf(p, q, newPosition))) {
					position.subtract(projection);
				}
				else {
					//triangulation.createFaceAtBoundary(boundaryEdge);
					// VTriangle triangle = new VTriangle(p, q, r);
					// position.add(triangle.midPoint().subtract(position));
				}
			}
		}
	}

	/**
	 * moves (which may include a back projection) each vertex according to their forces / velocity
	 * and resets their forces / velocities.
	 */
	private void updateVertices() {
		streamVertices().forEach(v -> updateVertex(v));
	}

	/**
	 * move the vertex (this may project the vertex back) and resets its velocity / forceå.
	 *
	 * @param vertex
	 */
	private void updateVertex(final V vertex) {
		// modify point placement only if it is not a fix point
		EikMeshPoint point = getMesh().getPoint(vertex);
		if(!isFixedVertex(vertex)) {

			// 1. p_{k+1} = p_k + dt * F(p_k)
			applyForce(vertex);

			// 2. project points outside of the boundary or if they are at the boundary
			//E optBoundaryEdge = getMesh().getEdge(vertex);
			boolean isAtBoundary = getMesh().isAtBoundary(vertex);
			double distance = distanceFunc.apply(vertex);
			if(isAtBoundary || distance > 0) {
				//(Math.abs(distance) <= deps &&
				// to prevent from large movements we project only by 0.5 if the distance is large!
				//projectBackVertex(vertex, distance, distance > initialEdgeLen / 3.0 ? 0.5 : 1.0, isAtBoundary);
				projectBackVertex(vertex, distance, 1.0, isAtBoundary);
			}

			if(isAtBoundary) {
				E boundaryEdge = getMesh().getBoundaryEdge(vertex).get();
				boolean collapse = false;

				// 3. remove unnecessary points
				if(getMesh().degree(vertex) == 3) {
					double force = getVelocity(vertex).distanceToOrigin();
					if(point.getAbsoluteForce() > 0 && force / point.getAbsoluteForce() < Parameters.MIN_FORCE_RATIO) {
						collapseVertices.add(vertex);
						collapse = true;
					}
				}

				// 4. split for low triangle quality
				if(!collapse && getMesh().isLongestEdge(boundaryEdge)
						&& faceToQuality(getMesh().getTwinFace(boundaryEdge)) < Parameters.MIN_SPLIT_TRIANGLE_QUALITY) {
					splitEdges.add(boundaryEdge);
				}
			}
		}
		point.setVelocity(new VPoint(0,0));
		point.setAbsoluteForce(0);
	}

	private boolean isFixedVertex(final V vertex) {
		return getMesh().getPoint(vertex).isFixPoint();
	}

	private IPoint getVelocity(final V vertex) {
		return getMesh().getPoint(vertex).getVelocity();
	}

	private void applyForce(final V vertex) {
		IPoint velocity = getVelocity(vertex);
		IPoint movement = velocity.scalarMultiply(delta);
		getMesh().getPoint(vertex).add(movement);
	}

	/**
	 * flips all edges which are illegal. Afterwards the triangulation is delaunay.
	 *
	 * @return true, if any flip was necessary, false otherwise.
	 */
	private boolean flipEdges() {
		if(runParallel) {
			streamEdges().filter(e -> triangulation.isIllegal(e)).forEach(e -> triangulation.flipSync(e));
		}
		else {
			streamEdges().filter(e -> triangulation.isIllegal(e)).forEach(e -> triangulation.flip(e));
		}
		return false;
	}

	/**
	 * Computation of the global scaling factor which is used to
	 */
	protected double computeEdgeLengthScaling() {
		double edgeLengthSum = streamEdges()
				.map(edge -> getMesh().toLine(edge))
				.mapToDouble(line -> line.length())
				.sum();

		double desiredEdgeLenSum = streamEdges()
				.map(edge -> getMesh().toLine(edge))
				.map(line -> line.midPoint())
				.mapToDouble(midPoint -> edgeLengthFunc.apply(midPoint)).sum();
		return Math.sqrt((edgeLengthSum * edgeLengthSum) / (desiredEdgeLenSum * desiredEdgeLenSum));
	}

	// helper methods
	protected Stream<E> streamEdges() {
		return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
	}

	protected Stream<V> streamVertices() {
		return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
	}

	// private methods
	private void removeBoundaryLowQualityTriangles() {
		List<F> holes = triangulation.getMesh().getHoles();
		Predicate<F> mergeCondition = f ->
				(!triangulation.getMesh().isDestroyed(f) && !triangulation.getMesh().isBoundary(f) && triangulation.getMesh().isAtBoundary(f)) // at boundary
						&& (!triangulation.isValid(f) /*|| (isCorner(f) || !isShortBoundaryEdge(f)) && faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY*/) // bad quality
				;

		for(F face : holes) {
			List<F> neighbouringFaces = getMesh().streamEdges(face).map(e -> getMesh().getTwinFace(e)).collect(Collectors.toList());
			for (F neighbouringFace : neighbouringFaces) {
				if (mergeCondition.test(neighbouringFace)) {
					triangulation.removeEdges(face, neighbouringFace, true);
				}
			}
		}

		List<F> neighbouringFaces = getMesh().streamEdges(getMesh().getBorder()).map(e -> getMesh().getTwinFace(e)).collect(Collectors.toList());
		for (F neighbouringFace : neighbouringFaces) {
			if (mergeCondition.test(neighbouringFace)) {
				triangulation.removeEdges(getMesh().getBorder(), neighbouringFace, true);
			}
		}

		//triangulation.mergeFaces(getMesh().getBorder(), mergeCondition, true);
	}

	private void removeTrianglesInsideHoles() {
		List<F> holes = triangulation.getMesh().getHoles();
		Predicate<F> mergeCondition = f -> !triangulation.getMesh().isBoundary(f) && distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0;
		for(F face : holes) {
			triangulation.mergeFaces(face, mergeCondition, true);
		}
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}

	private void collapseEdges() {
		for(V vertex : collapseVertices) {
			triangulation.collapse3DVertex(vertex, true);
		}
		collapseVertices = new ArrayList<>();
	}

	private void splitEdges() {
		for (E boundaryEdge : splitEdges) {
			if (!triangulation.getMesh().isDestroyed(boundaryEdge)) {
				triangulation.splitEdge(boundaryEdge, true);
			}
		}
		splitEdges = new ArrayList<>();
	}

	public void removeTrianglesOutsideBBox() {
		triangulation.shrinkBorder(f -> distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
	}


}
