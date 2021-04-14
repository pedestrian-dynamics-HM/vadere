package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IPlacementStrategy;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>Ruperts-Algorithm: not jet finished: Slow implementation!</p>
 *
 * @author Benedikt Zonnchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenRuppertsTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F>, ITriEventListener<V, E, F> {

	private static Logger logger = Logger.getLogger(GenRuppertsTriangulator.class);

	/**
	 * A triangulator for generating the constrained Delaunay triangulation
	 */
	private final GenConstrainedDelaunayTriangulator<V, E, F> cdt;

	/**
	 * The (segment bounded) planar straight line graph which will be triangulated.
	 */
	private final PSLG pslgBound;

	private final PSLG pslg;

	/**
	 * A user defined function for the desired circumcenter radius.
	 */
	private final Function<IPoint, Double> circumRadiusFunc;

	/**
	 * True if Ruppert's algorithm has finished, false otherwise
	 */
    private boolean generated;

	/**
	 * True if Ruppert's algorithm has been initialized i.e. the constrained Delaunay triangulation is constructed, false otherwise
	 */
	private boolean initialized;

	/**
	 * The set of segments, i.e. those should not be flipped
	 */
	private Set<E> segments;

	/**
	 * The triangulation which will be constructed.
 	 */
	private IIncrementalTriangulation<V, E, F> triangulation;

	/**
	 * The minimal angle3D (in degree) Ruppert's algorithm should achieve, i.e. after termination no
	 * triangle has an angle3D smaller than this angle3D.
	 */
	private double minAngle;

	/**
	 * The angle3D which guarantees that Ruppert's algorithm terminates. If the {@link GenRuppertsTriangulator#minAngle}
	 * is smaller the algorithm might not terminate.
	 */
	public static double MIN_ANGLE_TO_TERMINATE = 20.6;

	/**
	 * If true, all triangles inside holes and outside the segment-bound generated during the construction
	 * will be removed before termination.
	 */
	private boolean createHoles;

	private boolean allowSegmentFaces;

	/**
	 * A placement strategy which determines the position insertion points.
	 */
	private IPlacementStrategy<V, E, F> placementStrategy;


	private LinkedList<E> encroachedSegements;

	private Map<F, VTriangle> triangles;
	private Map<F, Double> qualities;

	private PriorityQueue<F> badTriangles;
	private Set<F> badTriangleSet;

	private PriorityQueue<F> largeTriangles;
	private Set<F> largeTriangleSet;


	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<V, E, F>> meshSupplier,
			@NotNull final PSLG pslg,
			final double minAngle,
			@NotNull Function<IPoint, Double> circumRadiusFunc,
			final boolean createHoles) {
		this(meshSupplier, pslg, minAngle, circumRadiusFunc, createHoles, true);
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<V, E, F>> meshSupplier,
			@NotNull final PSLG pslg,
			final double minAngle,
			@NotNull Function<IPoint, Double> circumRadiusFunc,
			final boolean createHoles,
			final boolean allowSegmentFaces) {
		this(meshSupplier, pslg, pslg, minAngle, circumRadiusFunc, createHoles, allowSegmentFaces);
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<V, E, F>> meshSupplier,
			@NotNull final PSLG pslgBound,
			@NotNull final PSLG pslg,
			final double minAngle,
			@NotNull Function<IPoint, Double> circumRadiusFunc,
			final boolean createHoles,
			final boolean allowSegmentFaces) {
		this.pslgBound = pslgBound;
		this.pslg = pslg;
		this.generated = false;
		this.segments = new HashSet<>();
		this.initialized = false;
		this.minAngle = minAngle;
		this.createHoles = createHoles;
		this.allowSegmentFaces = allowSegmentFaces;
		this.circumRadiusFunc = circumRadiusFunc;
		this.encroachedSegements = new LinkedList<>();
		this.badTriangles = new PriorityQueue<>(new FaceQualityComparator());
		this.largeTriangles = new PriorityQueue<>(new FaceCircumradiusComparator());
		this.badTriangleSet = new HashSet<>();
		this.largeTriangleSet = new HashSet<>();
		this.triangles = new HashMap<>();
		this.qualities = new HashMap<>();
		this.cdt = new GenConstrainedDelaunayTriangulator<>(meshSupplier, pslgBound, false);
		this.placementStrategy = new DelaunayPlacement<>(cdt.getMesh());
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<V, E, F>> meshSupplier,
			@NotNull final PSLG pslg) {
		this(meshSupplier, pslg, MIN_ANGLE_TO_TERMINATE, p -> Double.POSITIVE_INFINITY, true);
	}

	public Set<E> getSegments() {
		return segments;
	}

	public boolean isFinished() {
		return generated;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return cdt.getMesh();
	}

	/**
	 * main refinement
	 */
	public void refineSimplex2D() {

    	// split the next skinny triangle at its circumcenter TODO: order by quality ie worst triangle first!
		if(!badTriangles.isEmpty() || !largeTriangles.isEmpty()) {
			boolean handleBad = !badTriangles.isEmpty();
			// (1) get the next bad triangle
			F face = handleBad ? pollBadTriangle() : pollLargeTriangle();

			// the triangle might be no longer skinny due to the insertion of points
			if((handleBad && isBad(face)) || (!handleBad && isLarge(face))) {
				// (2) compute the insertion point
				VTriangle triangle = triangles.get(face);
				assert getMesh().toTriangle(face).midPoint().distance(triangle.midPoint()) < GeometryUtils.DOUBLE_EPS;
				VPoint circumCenter = placementStrategy.computePlacement(getMesh().getEdge(face), triangle);

				// (3) find segements which are encroached by the insertion point
				findEncrocedSegments(circumCenter);
				// (4.1) if there are any encroached segments split them
				if(!encroachedSegements.isEmpty()) {
					deEncrocheSgements(circumCenter);
					if(isBad(face)) {
						addBadTriangle(face);
					}
					else if(isLarge(face)) {
						addLargeTriangle(face);
					}
				} else { // (4.2) else insertVertex the point (and update data structure)
//					assert segments.stream().noneMatch(edge -> isEncroachedExpensive(edge));
					E e = triangulation.insert(circumCenter.getX(), circumCenter.getY());
//					assert segments.stream().noneMatch(edge -> isEncroachedExpensive(edge));
					logger.debug("inserted: " + circumCenter);
					for(F f : getMesh().getFaceIt(getMesh().getVertex(e))) {
						if(isBad(f)) {
							addBadTriangle(f);
						}
						else if(isLarge(f)) {
							addLargeTriangle(f);
						}
					}
				}
			}
		}
	}

	private void findEncrocedSegments(@NotNull final VPoint circumCenter) {
		segments.stream().filter(e -> isEncroached(e, circumCenter)).forEach(e -> encroachedSegements.add(e));
		//segments.stream().filter(e -> isEncroached(e, circumCenter)).forEach(e -> encroachedSegements.add(e));
	}

	public void refineSub() {
    	while (getMesh().streamFaces().anyMatch(f -> isBad(f))) {
    		refineSimplex2D();
	    }
	}

	private void removeOutsideTriangles() {

		// (1) remove triangles inside holes
	    for(VPolygon hole : pslgBound.getHoles()) {
		    Predicate<F> mergeCondition = f -> hole.contains(getMesh().toTriangle(f).midPoint());
		    Optional<F> optFace = getMesh().streamFaces(f -> !getMesh().isHole(f)).filter(mergeCondition).findAny();
		    if (optFace.isPresent()) {
			    Optional<F> optionalF = triangulation.createHole(optFace.get(), mergeCondition, true);
		    }
	    }

	    // (2) remove triangles outside the boundary
	    if(pslgBound.getSegmentBound() != null) {
		    Predicate<F> mergeCondition = f -> !pslgBound.getSegmentBound().contains(getMesh().toTriangle(f).midPoint());
		    triangulation.shrinkBorder(mergeCondition, true);
	    }
	}

	private void markOutsideTriangles() {
		for(VPolygon hole : pslgBound.getHoles()) {
			Predicate<F> markCondition = f -> !isMarked(f) && hole.contains(getMesh().toTriangle(f).midPoint());
			Optional<F> optFace = getMesh().streamFaces(f -> !getMesh().isHole(f)).filter(markCondition).findAny();
			if (optFace.isPresent()) {
				List<F> faces = triangulation.findFaces(optFace.get(), markCondition, 0);
				faces.stream().forEach(f -> mark(f));
			}
		}
	}

	public void step() {
    	if(!initialized) {
		    // (1) compute the constrained Delaunay triangulation (CDT)
		    triangulation = cdt.generate();
		    triangulation.addTriEventListener(this);

		    // (2) remove triangles inside holes and at concavities
		    //removeTriangles();

		    // (3) get the segments which should not be flipped!
		    segments.addAll(cdt.getConstrains());

		    triangulation.setCanIllegalPredicate(edge -> !segments.contains(edge) && !segments.contains(getMesh().getTwin(edge)));

		    if(createHoles) {
			    removeOutsideTriangles();
		    } else {
		    	markOutsideTriangles();
		    }

		    // (4) split all encroached segments
		    refineSimplex1D();

//		    assert segments.stream().noneMatch(edge -> isEncroachedExpensive(edge));

		    // (5) gather all bad triangles
		    getMesh().streamFaces().filter(f -> isBad(f)).forEach(f -> addBadTriangle(f));
		    getMesh().streamFaces().filter(f -> isLarge(f) && !isBad(f)).forEach(f -> addLargeTriangle(f));

		    initialized = true;
	    } else if(!badTriangles.isEmpty() || !largeTriangles.isEmpty()) {
		    refineSimplex2D();
    	} else if(!generated){
    		if(!allowSegmentFaces) {
			    List<E> initialSegments = segments.stream().collect(Collectors.toList());
			    for (E segment : initialSegments) {
				    VLine line = getMesh().toLine(segment);
				    VLine smallest = null;
				    for (E edge : getMesh().getEdgeIt(getMesh().getVertex(segment))) {
					    VLine nLine = getMesh().toLine(edge);
					    if(smallest == null || smallest.length() > nLine.length()) {
						    smallest = nLine;
					    }
				    }

				    for (E edge : getMesh().getEdgeIt(getMesh().getTwinVertex(segment))) {
					    VLine nLine = getMesh().toLine(edge);
					    if(smallest == null || smallest.length() > nLine.length()) {
						    smallest = nLine;
					    }
				    }

				    if(smallest.length() < line.length() * 0.5) {
					    split(segment);
				    }
			    }
			    split();
		    }
			generated = true;
	    } else {
		    logger.info("finished");
	    }
	}

	private void split() {
		List<E> edges = getMesh().getEdges();
		for(E edge : edges) {
			if(!segments.contains(edge)) {
				V v1 = getMesh().getVertex(edge);
				V v2 = getMesh().getTwinVertex(edge);
				if(isSegmentVertex(v1) && isSegmentVertex(v2)) {
					getTriangulation().splitEdge(edge, true);
				}
			}
		}
	}

	private F pollBadTriangle() {
		F badFace = badTriangles.poll();
		badTriangleSet.remove(badFace);
		return badFace;
	}

	private F pollLargeTriangle() {
		F badFace = largeTriangles.poll();
		largeTriangleSet.remove(badFace);
		return badFace;
	}

	private void addBadTriangle(@NotNull F face) {
		VTriangle triangle = getMesh().toTriangle(face);
		if(pslgBound.getSegmentBound().contains(triangle.midPoint())) {
			triangles.put(face, getMesh().toTriangle(face));
			qualities.put(face, getTriangulation().faceToQuality(face));
			if(!badTriangleSet.contains(face)) {
				badTriangles.add(face);
				badTriangleSet.add(face);
			}
		}

	}

	private boolean isConstrainsValid(@NotNull final VPolygon polygon){
		List<VLine> constrains = polygon.getLinePath();
		for(int i = 0; i < constrains.size(); i++) {
			VLine l1 = constrains.get(i);
			VLine l2 = constrains.get((i+1) % constrains.size());

			VPoint p1 = l1.getVPoint1();
			VPoint p2 = l1.getVPoint2();
			VPoint p3 = l2.getVPoint2();

			double angle = GeometryUtils.angle(p1, p2, p3);
			// angle3D should be larger than 60 degree
			assert GeometryUtils.isCW(p1, p2, p3) || angle >= 2 * Math.PI / 6 : p1 + "," + p2 + "," + p3;
			if(angle <= 2 * Math.PI / 6 ){
				return false;
			}
		}
		return true;
	}

	private void addLargeTriangle(@NotNull F face) {
		VTriangle triangle = getMesh().toTriangle(face);
		if(pslgBound.getSegmentBound().contains(triangle.midPoint())) {
			triangles.put(face, getMesh().toTriangle(face));
			qualities.put(face, getTriangulation().faceToQuality(face));
			if(!largeTriangleSet.contains(face)) {
				largeTriangles.add(face);
				largeTriangleSet.add(face);
			}
		}
	}

	private void refineSimplex1D() {
		segments.stream().filter(e -> isEncroached(e)).forEach(e -> encroachedSegements.addFirst(e));
		//segments.stream().filter(e -> isEncroachedExpensive(e)).forEach(e -> encroachedSegements.addFirst(e));
		deEncrocheSgements();
	}

	private void deEncrocheSgements(@NotNull final VPoint circumcenter) {
		while (!encroachedSegements.isEmpty()) {
			E segment = encroachedSegements.poll();
			assert segments.contains(segment);

			// to be robust for duplicates
			if(isEncroached(segment, circumcenter)) {
				split(segment);
			}
		}

		// we require this because split may other edges which are not direct neighbours encroached!
		refineSimplex1D();
	}

	private void deEncrocheSgements() {
		while (!encroachedSegements.isEmpty()) {
			E segment = encroachedSegements.poll();
			assert segments.contains(segment);

			// to be robust for duplicates
			if(isEncroached(segment)) {
				split(segment);
			}
		}
	}

	private Pair<E, E> split(@NotNull final E segment) {
		E splitSegment = isBoundary(segment) ? getMesh().getTwin(segment) : segment;
		int size = segments.size();
		segments.remove(splitSegment);
		segments.remove(getMesh().getTwin(splitSegment));
		assert segments.size() == size - 2;

		// add s1, s2
		VLine line = getMesh().toLine(splitSegment);
		VPoint midPoint = line.midPoint();
		V vertex = getMesh().createVertex(midPoint.getX(), midPoint.getY());
		V v1 = getMesh().getVertex(splitSegment);
		V v2 = getMesh().getTwinVertex(splitSegment);

		boolean mark = !createHoles && isMarked(getMesh().getTwinFace(splitSegment));

		// split s
		List<E> toLegalize = triangulation.splitEdgeAndReturn(vertex, splitSegment, false);

		// update data structure: add s1, s2
		E e1 = getMesh().getEdge(vertex, v1).get();
		E e2 = getMesh().getEdge(vertex, v2).get();

		segments.add(e1);
		segments.add(getMesh().getTwin(e1));
		segments.add(e2);
		segments.add(getMesh().getTwin(e2));

		if(mark) {
			// we have to mark the correct face, this depends on the call above i.e. V v1 = getMesh().getVertex(splitSegment);
			// and the fact that splitSegment is not a boundary edge.
			mark(getMesh().getFace(e1));
			mark(getMesh().getTwinFace(e2));
		}

		for(E e : toLegalize) {
			triangulation.legalize(e, vertex);
		}

		if(isEncroached(e1)) {
			encroachedSegements.add(e1);
			assert segments.contains(e1);
		}

		if(isEncroached(e2)) {
			encroachedSegements.add(e2);
			assert segments.contains(e2);
		}

		for(F f : getMesh().getFaceIt(vertex)) {
			if(!isBoundary(f)) {
				if(isBad(f)) {
					addBadTriangle(f);
				}
				else if(isLarge(f)) {
					addLargeTriangle(f);
				}
			}
		}
		handleVertexInsertion(vertex);
		return Pair.of(e1, e2);
	}

	private boolean isBoundary(@NotNull final F f) {
		return getMesh().isBoundary(f) || isMarked(f);
	}

	private boolean isBoundary(@NotNull final E edge) {
		return isBoundary(getMesh().getFace(edge));
	}

	private boolean isAtBoundary(@NotNull final E edge) {
		return isBoundary(edge) || isBoundary(getMesh().getTwin(edge));
	}

    @Override
    public IIncrementalTriangulation<V, E, F> generate() {
	   return generate(true);
    }

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		while (!isFinished()) {
			step();
		}
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	private boolean isLarge(@NotNull final F face) {
		VTriangle triangle = getMesh().toTriangle(face);
		return isInside(face)
				&& (circumRadiusFunc.apply(triangle.getCircumcenter()) < triangle.getCircumscribedRadius());
	}

	private boolean isSegmentFace(@NotNull final F face) {
		if(allowSegmentFaces) {
			return false;
		}
		else {
			return getMesh().streamVertices(face).allMatch(v -> isSegmentVertex(v));
		}
	}

	private boolean isSegmentVertex(@NotNull final V v) {
		return getMesh().streamEdges(v).anyMatch(e -> segments.contains(e));
	}

	private boolean isBad(@NotNull final F face) {
		return isInside(face) && isSkinny(face, minAngle);
    }

    private boolean isInside(@NotNull final F face) {
		if(isBoundary(face)) {
			return false;
		}

		//TODO: this might be expensive!
		return pslg.getSegmentBound().contains(getMesh().toTriangle(face).midPoint());
    }

	private boolean isSkinny(@NotNull final F face, final double angle) {
		double alpha = angle; // lowest angle3D in degree
		double radAlpha = Math.toRadians(alpha);
		VTriangle triangle = getMesh().toTriangle(face);

		return GeometryUtils.angle(triangle.p1, triangle.p2, triangle.p3) < radAlpha
				|| GeometryUtils.angle(triangle.p3, triangle.p1, triangle.p2) < radAlpha
				|| GeometryUtils.angle(triangle.p2, triangle.p3, triangle.p1) < radAlpha;
	}

	private boolean isEncroached(@NotNull final E segment, @NotNull final VPoint p) {
		VLine line = getMesh().toLine(segment);
		VPoint midPoint = line.midPoint();
		VCircle diameterCircle = new VCircle(midPoint, midPoint.distance(line.getX1(), line.getY1()));
		return p.distance(line.getVPoint1()) > GeometryUtils.DOUBLE_EPS && p.distance(line.getVPoint2()) > GeometryUtils.DOUBLE_EPS && diameterCircle.contains(p);
	}

	private boolean isEncroached(@NotNull final E segment) {
		E seg = isBoundary(segment) ? getMesh().getTwin(segment) : segment;
		VPoint p1 = getMesh().toPoint(getMesh().getNext(seg));
		if(isEncroached(seg, p1)) {
			return true;
		} else if(isAtBoundary(seg)) {
			return false;
		} else {
			VPoint p2 = getMesh().toPoint(getMesh().getNext(getMesh().getTwin(seg)));
			return isEncroached(seg, p2);
		}
	}

	private boolean isMarked(@NotNull final F face) {
		return getMesh().getBooleanData(face, "boundary");
	}

	private void mark(@NotNull final F face) {
		getMesh().setBooleanData(face, "boundary", true);
	}

    /*private boolean isEncroached(@NotNull final E segment) {
		E seg = getMesh().isBoundary(segment) ? getMesh().getTwin(segment) : segment;
	    VLine line = getMesh().toLine(seg);
	    VPoint midPoint = line.midPoint();
	    VCircle diameterCircle = new VCircle(midPoint, midPoint.distance(line.getX1(), line.getY1()));

	    IPoint p1 = getMesh().getPoint(getMesh().getNext(seg));

	    if(diameterCircle.getCenter().distance(p1) < diameterCircle.getRadius()) {
		    return true;
	    }

	    if(!getMesh().isAtBoundary(seg)) {
		    IPoint p2 = getMesh().getPoint(getMesh().getNext(getMesh().getTwin(seg)));
		    if((diameterCircle.getCenter().distance(p2) < diameterCircle.getRadius())) {
			    return true;
		    }
	    }

	    return false;
    }*/

    // TODO replace this!
	private boolean isEncroachedExpensive(@NotNull final E segment) {
		VLine line = getMesh().toLine(segment);
		VPoint midPoint = line.midPoint();
		VCircle diameterCircle = new VCircle(midPoint, midPoint.distance(line.getX1(), line.getY1()));
		return getMesh().streamPoints().anyMatch(p -> isEncroached(segment, new VPoint(p.getX(), p.getY())));
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {
		//handleVertexInsertion(v);
	}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {
		//handleVertexInsertion(v);
	}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {

	}

	@Override
	public void postInsertEvent(V vertex) {
		handleVertexInsertion(vertex);
	}

	private void handleVertexInsertion(@NotNull final V vertex) {
		for(E e : getMesh().getEdgeIt(vertex)) {
			E prev = getMesh().getPrev(e);
			if(segments.contains(prev) && isEncroached(prev)) {
				encroachedSegements.add(prev);
				assert segments.contains(prev);
			}
		}
	}

	private final class FaceCircumradiusComparator implements Comparator<F> {

		@Override
		public int compare(F o1, F o2) {
			return Double.compare(-triangles.get(o1).getCircumscribedRadius(), -triangles.get(o2).getCircumscribedRadius());
		}
	}

	private final class FaceQualityComparator implements Comparator<F> {

		@Override
		public int compare(F o1, F o2) {
			return Double.compare(qualities.get(o1), qualities.get(o2));
		}
	}
}
