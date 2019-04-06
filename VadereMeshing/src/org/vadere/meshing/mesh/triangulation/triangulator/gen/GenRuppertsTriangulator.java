package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IPlacementStrategy;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Ruperts-Algorithm: not jet finished: Slow implementation!</p>
 *
 * @author Benedikt Zonnchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenRuppertsTriangulator<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements ITriangulator<P, CE, CF, V, E, F> {

	private static Logger log = Logger.getLogger(GenRuppertsTriangulator.class);
    private final GenConstrainedDelaunayTriangulator<P, CE, CF, V, E, F> cdt;
    private final Function<IPoint, Double> circumRadiusFunc;
    private final Set<P> points;
    private boolean generated;

	// Improvements: maybe mark edges which should not be flipped instead of using a Set is slower.
    private Set<E> segments;

    private Collection<VPolygon> holes;
    @Nullable private VPolygon boundingBox;
	private IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private boolean initialized;
	private double minAngle;
	private double threashold;
	private static double MIN_ANGLE_TO_TERMINATE = 20;
	private boolean createHoles;
	private IPlacementStrategy<P, CE, CF, V, E, F> placementStrategy;

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<P, CE, CF, V, E, F>> meshSupplier,
			@NotNull final Collection<VLine> constrains,
			@NotNull final VPolygon bound,
			@NotNull final Set<P> points,
			final double minAngle,
			@NotNull Function<IPoint, Double> circumRadiusFunc,
			final boolean createHoles) {
		this.points = points;
		this.generated = false;
		this.segments = new HashSet<>();
		this.holes = Collections.EMPTY_LIST;
		this.boundingBox = bound;
		this.initialized = false;
		this.generated = false;
		this.minAngle = minAngle;
		this.threashold = 0.0;
		this.createHoles = createHoles;
		this.circumRadiusFunc = circumRadiusFunc;

		/**
		 * This prevent the flipping of constrained edges
		 */
		Predicate<E> canIllegal = e -> !segments.contains(e) && !segments.contains(getMesh().getTwin(e));
		this.cdt = new GenConstrainedDelaunayTriangulator<>(meshSupplier, GeometryUtils.boundRelative(bound.getPoints()), constrains, points, canIllegal);
		this.placementStrategy = new DelaunayPlacement<>(cdt.getMesh());
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<P, CE, CF, V, E, F>> meshSupplier,
			@NotNull final Collection<VLine> constrains,
			@NotNull final VPolygon bound) {
		this(meshSupplier, constrains, bound, Collections.EMPTY_SET, MIN_ANGLE_TO_TERMINATE, p -> Double.POSITIVE_INFINITY, true);
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<P, CE, CF, V, E, F>> meshSupplier,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final Set<P> points,
			final double minAngle,
			final boolean createHoles) {
		this(meshSupplier,
				Stream.concat(bound.getLinePath().stream(),constrains.stream().flatMap(polygon -> polygon.getLinePath().stream())).collect(Collectors.toList()),
			 bound,
			 points,
			 minAngle,
				p -> Double.POSITIVE_INFINITY,
			 createHoles);
		this.holes = constrains;
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<P, CE, CF, V, E, F>> meshSupplier,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final Set<P> points,
			final double minAngle) {
		this(meshSupplier, bound, constrains, points, minAngle, true);
	}

	public GenRuppertsTriangulator(
			@NotNull final Supplier<IMesh<P, CE, CF, V, E, F>> meshSupplier,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final Set<P> points) {
		this(meshSupplier, bound, constrains, points, MIN_ANGLE_TO_TERMINATE);
	}

	public Set<E> getSegments() {
		return segments;
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return cdt.getMesh();
	}

	public void refine(final double angle, final double threshold) {
		// (1) split segments until they are no longer encroached
		if (findAndSplit(threshold)) {
			return;
		}
		//while (findAndSplit(threshold)) {}

    	// (2) split the next skinny triangle at its circumcenter TODO: order by quality ie worst triangle first!
		Optional<F> skinnyTriangle = getMesh().streamFaces().filter(f -> isIllegal(f, angle, threshold)).findAny();
		if(skinnyTriangle.isPresent()) {
			F skinnyFace = skinnyTriangle.get();

			// (2.1) compute the circumcenter
			VTriangle triangle = getMesh().toTriangle(skinnyFace);
			VPoint circumCenter = placementStrategy.computePlacement(getMesh().getEdge(skinnyFace), triangle);

			// (2.2) find all encroached segements with respect to the circumcenter TODO: this is slow!
			Collection<E> encroachedSegements = segments.stream().filter(e -> isEncroached(e, circumCenter)).collect(Collectors.toList());
			if(encroachedSegements.size() > 0) {
				// (2.3) make sure that all encroached segements will be split
				for (E encroachedSegement : encroachedSegements) {
					split(encroachedSegement);
				}
			} else {
				triangulation.insert(circumCenter.getX(), circumCenter.getY());
			}
		}
	}

	public void refineSub() {
    	while (getMesh().streamFaces().anyMatch(f -> isIllegal(f, minAngle, threashold))) {
    		refine(minAngle, threashold);
	    }
	}

	public void removeTriangles() {
    	if(createHoles) {
		    for(VPolygon hole : holes) {
			    Predicate<F> mergeCondition = f -> hole.contains(getMesh().toTriangle(f).midPoint());
			    Optional<F> optFace = getMesh().streamFaces().filter(mergeCondition).findAny();
			    if(optFace.isPresent()) {
				    Optional<F> optionalF = triangulation.createHole(optFace.get(), mergeCondition, true);
			    }
		    }

		    if(boundingBox != null) {
			    Predicate<F> mergeCondition = f -> !boundingBox.contains(getMesh().toTriangle(f).midPoint());
			    triangulation.shrinkBorder(mergeCondition, true);
		    }
	    }
    	generated = true;
	}

	public void step() {
    	if(!initialized) {
		    // (1) compute the constrained Delaunay triangulation (CDT)
		    triangulation = cdt.generate();
		    //removeTriangles();

		    // (2) remove triangles inside holes and at concavities

		    // (3) get the half-edges representing the segments
		    segments.addAll(cdt.getConstrains());

		    // (3) the core algorithm
		    initialized = true;
	    } else if(getMesh().streamFaces().anyMatch(f -> isIllegal(f, minAngle, threashold))) {
		    refine(minAngle, threashold);
    	} else if(!generated){
			removeTriangles();
	    }
	}

	private boolean findAndSplit(final double threshold) {
		// remove s
		Optional<E> encroachedSegmentOpt = segments.stream().filter(e -> isEncroached(e, threashold)).findAny();
		if(encroachedSegmentOpt.isPresent()) {
			E segment = encroachedSegmentOpt.get();
			assert segments.contains(segment);
			split(segment);
			return true;
		}
		return false;
	}

	private void split(@NotNull final E segment) {
		segments.remove(segment);
		segments.remove(getMesh().getTwin(segment));

		// add s1, s2
		VLine line = getMesh().toLine(segment);
		VPoint midPoint = line.midPoint();
		V vertex = getMesh().createVertex(midPoint.getX(), midPoint.getY());
		V v1 = getMesh().getVertex(segment);
		V v2 = getMesh().getTwinVertex(segment);

		// split s
		List<E> toLegalize = triangulation.splitEdgeAndReturn(vertex, segment, false);

		// update data structure: add s1, s2
		E e1 = getMesh().getEdge(vertex, v1).get();
		E e2 = getMesh().getEdge(vertex, v2).get();

		segments.add(e1);
		segments.add(getMesh().getTwin(e1));
		segments.add(e2);
		segments.add(getMesh().getTwin(e2));

		for(E e : toLegalize) {
			triangulation.legalize(e, vertex);
		}
	}

    @Override
    public IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {
	   return generate(true);
    }

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate(boolean finalize) {
		if(!initialized) {
			// (1) compute the constrained Delaunay triangulation (CDT)
			triangulation = cdt.generate(finalize);

			// (2) remove triangles inside holes and at concavities

			// (3) get the half-edges representing the segments
			segments.addAll(cdt.getConstrains());

			// (3) the core algorithm
			initialized = true;
		}
		refineSub();
		removeTriangles();

		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation() {
		return triangulation;
	}

	private boolean isIllegal(@NotNull final F face, final double angle, final double threashold) {
		return isInside(face) && isSkinny(face, angle, threashold);
    }

    private boolean isInside(@NotNull final F face) {
		if(getMesh().isBoundary(face)) {
			return false;
		}

		//TODO: this might be expensive!
		return boundingBox.contains(getMesh().toTriangle(face).midPoint());
    }

	private boolean isSkinny(@NotNull final F face, final double angle, final double threashold) {
		double alpha = angle; // lowest angle in degree
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
		return diameterCircle.contains(p);
	}

    private boolean isEncroached(@NotNull final E segment, final double threashold) {
	    VLine line = getMesh().toLine(segment);
	    if(line.length() > threashold) {
		    VPoint midPoint = line.midPoint();
		    VCircle diameterCircle = new VCircle(midPoint, midPoint.distance(line.getX1(), line.getY1()));

		    P p1 = getMesh().getPoint(getMesh().getNext(segment));

		    if(diameterCircle.getCenter().distance(p1) < diameterCircle.getRadius() - GeometryUtils.DOUBLE_EPS) {
			    return true;
		    }

		    P p2 = getMesh().getPoint(getMesh().getNext(getMesh().getTwin(segment)));

		    if((diameterCircle.getCenter().distance(p2) < diameterCircle.getRadius()  - GeometryUtils.DOUBLE_EPS)) {
			    return true;
		    }
	    }

	    return false;
    }
}
