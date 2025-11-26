package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.ITriangulationSupplier;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

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
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenUniformRefinementTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {
	private final Collection<? extends VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private IIncrementalTriangulation<V, E, F> triangulation;
	private Set<IPoint> points;
	private IMesh<V, E, F> mesh;
	private  LinkedList<F> toRefineEdges;
	private static final Logger logger = Logger.getLogger(GenUniformRefinementTriangulator.class);
	private final IDistanceFunction distFunc;
	private final Map<IPoint,Integer> creationOrder;
	private boolean initialized;
	private boolean generated;

    /**
     * <p>Default constructor.</p>
     *
     * @param supplier      a supplier to construct a completely fresh and empty triangulation {@link IIncrementalTriangulation}
     * @param bound         the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary      the boundaries e.g. obstacles
     * @param lenFunc       a edge length function
     * @param distFunc      a signed distance function
     */
	public GenUniformRefinementTriangulator(
			final ITriangulationSupplier<V, E, F> supplier,
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
        this.generated = false;
	}

	/**
	 * <p>Initializes this triangulator. This has to be called before
	 * {@link GenUniformRefinementTriangulator#step()} can be called.</p>
	 */
    public void init() {
        triangulation.init();
	    toRefineEdges.addAll(triangulation.getMesh().faces().getAll());
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

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return triangulation.getMeshDataStorage();
	}

	@Override
	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return triangulation.getMeshBuilder();
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
	 * Generates the triangulation, i.e.
	 * <ol>
	 *     <li>{@link GenUniformRefinementTriangulator#init()}</li>
	 *     <li>{@link GenUniformRefinementTriangulator#step()} until {@link GenUniformRefinementTriangulator#isFinished()}</li>
	 *     <li>{@link GenUniformRefinementTriangulator#finish()}</li>
	 * </ol>
	 *
	 * @return the generated triangulation
	 */
	public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!generated) {
			logger.info("start triangulation generation");
			init();

			while (!isFinished()) {
				step();
			}

			if(finalize) {
				finish();
			}

			logger.info("end triangulation generation");
			generated = true;
		}
		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
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
	    return mesh.edges().streamEdgesOf(face).reduce((e1, e2) -> mesh.edges().toLine(e1).length() > mesh.edges().toLine(e2).length() ? e1 : e2).get();
    }

	private void removeTrianglesOutsideBBox() {
		getMeshBuilder().changeConnectivity().shrinkBorder(f -> distFunc.apply(mesh.faces().toTriangle(f).midPoint()) > 0, true);
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = mesh.faces().getAll();
		for(F face : faces) {
			if(!mesh.faces().isDestroyed(face) && !mesh.faces().isHole(face)) {
				getMeshBuilder().changeConnectivity().createHole(face, f -> distFunc.apply(mesh.faces().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}

	private boolean isCompleted(E edge) {
		if(mesh.edges().isBoundary(edge)){
			edge = mesh.edges().getTwin(edge);
		}

		F face = mesh.faces().getOf(edge);
		F twin = mesh.faces().getTwin(edge);

		VTriangle triangle = mesh.faces().toTriangle(face);
		VLine line = mesh.edges().toLine(edge);

		return (line.length() <= lenFunc.apply(line.midPoint()));
	}

    private void refine(final E edge) {
        IPoint midPoint = mesh.edges().toLine(edge).midPoint();
	    IPoint p = mesh.createPoint(midPoint.getX(), midPoint.getY());

        if(!points.contains(p)) {
            points.add(p);
            creationOrder.put(p, points.size()-1);
            Pair<E, E> createdEdges = getMeshBuilder().changeConnectivity().splitEdge(p, edge, true);
            List<F> createdFaces = new ArrayList<>(4);


            if(createdEdges.getRight() != null) {
                E e1 = createdEdges.getRight();
                E e2 = mesh.edges().getTwin(createdEdges.getRight());

                F f1 = mesh.faces().getOf(e1);
                F f2 = mesh.faces().getOf(e2);

                toRefineEdges.addFirst(f1);
                toRefineEdges.addFirst(f2);

            }


            if(createdEdges.getLeft() != null) {
                E e1 = createdEdges.getLeft();
                E e2 = mesh.edges().getTwin(createdEdges.getLeft());

                Integer index1 = creationOrder.get(mesh.edges().getMutableEndPoint(mesh.edges().getNext(e1)));
                Integer index2 = creationOrder.get(mesh.edges().getMutableEndPoint(mesh.edges().getNext(e2)));


                toRefineEdges.addFirst(mesh.faces().getOf(e2));
                toRefineEdges.addFirst(mesh.faces().getOf(e1));

            }
        }
    }
}
