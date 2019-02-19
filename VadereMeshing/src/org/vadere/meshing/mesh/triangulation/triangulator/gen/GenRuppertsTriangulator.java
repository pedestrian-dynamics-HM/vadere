package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>Ruperts-Algorithm: not jet finished!</p>
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

	private static Logger log = LogManager.getLogger(GenRuppertsTriangulator.class);
    private final GenConstrainedDelaunayTriangulator<P, CE, CF, V, E, F> cdt;
    private final Set<P> points;
    private boolean generated;
    private Set<E> segments;
    private Collection<VPolygon> holes;
    @Nullable private VPolygon boundingBox;
	private IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;
	private boolean initialized;
	private double minAngle;
	private double threashold;
	private static double MIN_ANGLE_TO_TERMINATE = 20;
	private boolean createHoles;


    public GenRuppertsTriangulator(
		    @NotNull final IMesh<P, CE, CF, V, E, F> mesh,
		    @NotNull final VRectangle bound,
		    @NotNull final Collection<VLine> constrains,
		    @NotNull final Set<P> points,
		    final double minAngle,
		    final boolean createHoles) {
        this.points = points;
        this.generated = false;
        this.segments = new HashSet<>();
		this.holes = Collections.EMPTY_LIST;
		this.boundingBox = null;
		this.initialized = false;
		this.generated = false;
		this.minAngle = minAngle;
		this.threashold = 0.0;
		this.createHoles = createHoles;

	    /**
	     * This prevent the flipping of constrained edges
	     */
	    Predicate<E> canIllegal = e -> !segments.contains(e) && !segments.contains(getMesh().getTwin(e));

	    this.cdt = new GenConstrainedDelaunayTriangulator<>(mesh, bound, constrains, points, canIllegal);
    }

	public GenRuppertsTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VRectangle bound,
			@NotNull final Collection<VLine> constrains,
			@NotNull final Set<P> points,
			final double minAngle) {
		this(mesh, bound, constrains, points, minAngle, true);
	}

	public GenRuppertsTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VRectangle bound,
			@NotNull final Collection<VLine> constrains,
			@NotNull final Set<P> points) {
		this(mesh, bound, constrains, points, MIN_ANGLE_TO_TERMINATE);
	}

	public GenRuppertsTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final Set<P> points,
			final double minAngle) {
		this(mesh, bound, constrains, points, minAngle, true);
	}

	public GenRuppertsTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final Set<P> points,
			final double minAngle,
			final boolean createHoles) {
		this.points = points;
		this.generated = false;
		this.segments = new HashSet<>();
		this.holes = constrains;
		this.boundingBox = bound;
		this.initialized = false;
		this.generated = false;
		this.minAngle = minAngle;
		this.threashold = 0.0;
		this.createHoles = createHoles;

		List<VLine> lines = new ArrayList<>();
		for(VPolygon polygon : constrains) {
			lines.addAll(polygon.getLinePath());
		}

		lines.addAll(bound.getLinePath());

		/**
		 * This prevent the flipping of constrained edges
		 */
		Predicate<E> canIllegal = e -> !segments.contains(e) && !segments.contains(getMesh().getTwin(e));

		this.cdt = new GenConstrainedDelaunayTriangulator<>(mesh, GeometryUtils.bound(bound.getPoints(), GeometryUtils.DOUBLE_EPS), lines, points, canIllegal);
	}

	public GenRuppertsTriangulator(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final VPolygon bound,
			@NotNull final Collection<VPolygon> constrains,
			@NotNull final Set<P> points) {
		this(mesh, bound, constrains, points, MIN_ANGLE_TO_TERMINATE);
	}

	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return cdt.getMesh();
	}

	public void refine(final double angle, final double threshold) {
		// (1) split segments until they are no longer encroached
    	while (findAndSplit(threshold)) {}

    	// (2) split the next skinny triangle at its circumcenter
		Optional<F> skinnyTriangle = getMesh().streamFaces().filter(f -> isSkinny(f, angle, threshold)).findAny();
		if(skinnyTriangle.isPresent()) {
			F skinnyFace = skinnyTriangle.get();

			// (2.1) compute the circumcenter
			VPoint circumCenter = getMesh().toTriangle(skinnyFace).getCircumcenter();

			// (2.2) find all encroached segements with respect to the circumcenter
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
    	while (getMesh().streamFaces().anyMatch(f -> isSkinny(f, minAngle, threashold))) {
    		refine(minAngle, threashold);
	    }
	}

	public void removeTriangles() {
    	if(createHoles) {
		    for(VPolygon hole : holes) {
			    Predicate<F> mergeCondition = f -> hole.contains(getMesh().toTriangle(f).midPoint());
			    Optional<F> optFace = getMesh().streamFaces().filter(mergeCondition).findAny();
			    if(optFace.isPresent()) {
				    triangulation.createHole(optFace.get(), mergeCondition, true);
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

		    // (2) remove triangles inside holes and at concavities

		    // (3) get the half-edges representing the segments
		    segments = new HashSet<>(cdt.getConstrains());

		    // (3) the core algorithm
		    initialized = true;
	    } else if(getMesh().streamFaces().anyMatch(f -> isSkinny(f, minAngle, threashold))) {
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

		// add s1, s2
		VLine line = getMesh().toLine(segment);
		VPoint midPoint = line.midPoint();
		V vertex = getMesh().createVertex(midPoint.getX(), midPoint.getY());

		// split s
		log.info("split edge at " + vertex);
		List<E> toLegalize = triangulation.splitEdgeAndReturn(vertex, segment, false);

		// add s1, s2
		int count = 0;
		for(E e : getMesh().getEdgeIt(vertex)) {
			// update data structure

			V twinPoint = getMesh().getTwinVertex(e);
			if(getMesh().toPoint(twinPoint).equals(new VPoint(line.getX1(), line.getY1())) ||
					getMesh().toPoint(twinPoint).equals(new VPoint(line.getX2(), line.getY2()))) {
				count++;
				//if(isEncroached(e)) {
					segments.add(e);
				//}
			}
		}

		assert count == 2;

		for(E e : toLegalize) {
			triangulation.legalize(e, vertex);
		}
	}

    @Override
    public IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {
	    if(!initialized) {
		    // (1) compute the constrained Delaunay triangulation (CDT)
		    triangulation = cdt.generate();

		    // (2) remove triangles inside holes and at concavities

		    // (3) get the half-edges representing the segments
		    segments = new HashSet<>(cdt.getConstrains());

		    // (3) the core algorithm
		    initialized = true;
	    }
	    refineSub();
	    removeTriangles();

		return triangulation;
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

		    if(diameterCircle.getCenter().distance(p1) < diameterCircle.getRadius() - 0.1) {
			    return true;
		    }

		    P p2 = getMesh().getPoint(getMesh().getNext(getMesh().getTwin(segment)));

		    if((diameterCircle.getCenter().distance(p2) < diameterCircle.getRadius()  - 0.1)) {
			    return true;
		    }
	    }

	    return false;
    }
}
