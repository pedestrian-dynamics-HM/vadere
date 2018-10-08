package org.vadere.util.geometry.mesh.triangulation.triangulator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.geometry.mesh.inter.ITriangulationSupplier;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;

import java.util.*;

/**
 * <p>Triangulation creator: This class is implements an algorithm which refines a given triangulation
 * (which might be empty), by recursively splitting existing triangles (starting with the super triangle if
 * the triangulation is empty) into parts. More precisely, each tringle which should be split will be split
 * at its longest edge in one iteration. Which generates a new set of triangles. The process is repeated until
 * there is no more triangle which has to be split left.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> generic type of the point
 * @param <V> generic type of the vertex
 * @param <E> generic type of the half-edge
 * @param <F> generic type of the face
 */
public class UniformRefinementTriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {
	private final Collection<? extends VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, V, E, F>  triangulation;
	private Set<P> points;
	private IMesh<P, V, E, F> mesh;
	private  LinkedList<F> toRefineEdges;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulator.class);
	private final IDistanceFunction distFunc;
	private final Map<P,Integer> creationOrder;
	private boolean initialized;

    /**
     * <p>Default constructor.</p>
     *
     * @param supplier
     * @param bound         the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary      the boundaries e.g. obstacles
     * @param lenFunc       a edge length function
     * @param distFunc      a signed distance function
     */
	public UniformRefinementTriangulator(
			final ITriangulationSupplier<P, V, E, F> supplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc) {

	    this.distFunc = distFunc;
		this.triangulation = supplier.get();
		this.mesh = triangulation.getMesh();
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.points = new HashSet<>();
        this.toRefineEdges = new LinkedList<>();
        this.creationOrder = new HashMap<>();
        this.initialized = false;
	}

	/**
	 * <p>Initializes this triangulator. This has to be called before
	 * {@link UniformRefinementTriangulator#step()} can be called.</p>
	 */
    public void init() {
        triangulation.init();
	    toRefineEdges.addAll(triangulation.getMesh().getFaces());
	    initialized = true;
    }

	/**
	 * <p>Implements one split-iteration.</p>
	 */
	public void step() {
	    synchronized (mesh) {
	    	if(!initialized) {
	    		init();
		    }

            if (!toRefineEdges.isEmpty()) {
                F face = toRefineEdges.removeLast();
                E edge = getLongestEdge(face);

                if(!isCompleted(edge)) {
                    refine(edge);
                }
            }
        }
    }

	/**
	 * <p>Returns true if there is no more triangle which has to be split.</p>
	 *
	 * @return true if there is no more triangle which has to be split, false otherwise
	 */
	public boolean isFinished() {
        return toRefineEdges.isEmpty();
    }

	/**
	 * <p>Generates the triangulation, i.e.
	 * <ol>
	 *     <li>{@link UniformRefinementTriangulator#init()}</li>
	 *     <li>{@link UniformRefinementTriangulator#step()} until {@link UniformRefinementTriangulator#isFinished()}</li>
	 *     <li>{@link UniformRefinementTriangulator#finish()}</li>
	 * </ol></p>
	 *
	 * @return the generated triangulation
	 */
	public ITriangulation<P, V, E, F> generate() {
        logger.info("start triangulation generation");
        init();

		while (!isFinished()) {
			step();
		}

        finish();
		logger.info("end triangulation generation");
		return triangulation;
	}

	/**
	 * <p>Removes useless and unwanted triangles.</p>
	 */
    public void finish() {
        synchronized (mesh) {
            removeTrianglesOutsideBBox();
            removeTrianglesInsideObstacles();
            triangulation.finish();
        }
    }


    private E getLongestEdge(F face) {
	    return mesh.streamEdges(face).reduce((e1, e2) -> mesh.toLine(e1).length() > mesh.toLine(e2).length() ? e1 : e2).get();
    }

	private void removeTrianglesOutsideBBox() {
		triangulation.shrinkBorder(f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}

	private boolean isCompleted(E edge) {
		if(mesh.isBoundary(edge)){
			edge = mesh.getTwin(edge);
		}

		F face = mesh.getFace(edge);
		F twin = mesh.getTwinFace(edge);

		VTriangle triangle = mesh.toTriangle(face);
		VLine line = mesh.toLine(edge);

		return (line.length() <= lenFunc.apply(line.midPoint()));
	}

    private void refine(final E edge) {
        IPoint midPoint = mesh.toLine(edge).midPoint();
        P p = mesh.createPoint(midPoint.getX(), midPoint.getY());

        if(!points.contains(p)) {
            points.add(p);
            creationOrder.put(p, points.size()-1);
            Pair<E, E> createdEdges = triangulation.splitEdge(p, edge, true);
            List<F> createdFaces = new ArrayList<>(4);


            if(createdEdges.getRight() != null) {
                E e1 = createdEdges.getRight();
                E e2 = mesh.getTwin(createdEdges.getRight());

                F f1 = mesh.getFace(e1);
                F f2 = mesh.getFace(e2);

                toRefineEdges.addFirst(f1);
                toRefineEdges.addFirst(f2);

            }


            if(createdEdges.getLeft() != null) {
                E e1 = createdEdges.getLeft();
                E e2 = mesh.getTwin(createdEdges.getLeft());

                Integer index1 = creationOrder.get(mesh.getPoint(mesh.getNext(e1)));
                Integer index2 = creationOrder.get(mesh.getPoint(mesh.getNext(e2)));


                toRefineEdges.addFirst(mesh.getFace(e2));
                toRefineEdges.addFirst(mesh.getFace(e1));

            }
        }
    }
}
