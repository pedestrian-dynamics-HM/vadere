package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.*;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.util.*;

/**
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
@Deprecated
public class UniformSFCTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {
    private final Collection<VShape> boundary;
    private final VRectangle bbox;
    private final IEdgeLengthFunction lenFunc;
    private IIncrementalTriangulation<V, E, F> triangulation;
    private Set<IPoint> points;
    private IMesh<V, E, F> mesh;
    private static final Logger logger = Logger.getLogger(UniformSFCTriangulator.class);
    private final IDistanceFunction distFunc;
    private final static Random random = new Random();
    private final LinkedList<F> sortedFaces;

    /**
     * @param triangulation an empty triangulation to fill
     * @param bound         the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary      the boundaries e.g. obstacles
     * @param lenFunc       a edge length function
     * @param distFunc      a signed distance function
     */
    public UniformSFCTriangulator(
            final IIncrementalTriangulation<V, E, F> triangulation,
            final VRectangle bound,
            final Collection<VShape> boundary,
            final IEdgeLengthFunction lenFunc,
            final IDistanceFunction distFunc) {

        this.distFunc = distFunc;
        this.triangulation = triangulation;
        this.mesh = triangulation.getMesh();
        this.boundary = boundary;
        this.lenFunc = lenFunc;
        this.bbox = bound;
        this.points = new HashSet<>();
        this.sortedFaces = new LinkedList<>();
    }

    public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
    }

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		triangulation.init();

		logger.info("start triangulation generation");
		LinkedList<F> toRefineFaces = new LinkedList<>();
		LinkedList<F> sortedFaces = new LinkedList<>();

		toRefineFaces.addAll(mesh.faces().getAll());

		while (!toRefineFaces.isEmpty()) {
			F face = toRefineFaces.removeFirst();

			E longestEdge = mesh.edges().streamEdgesOf(face)
					.reduce((e1, e2) -> mesh.edges().toLine(e1).length() > mesh.edges().toLine(e2).length() ? e1 : e2)
					.get();

			if(!isCompleted(longestEdge)) {
				IPoint midPoint = mesh.edges().toLine(longestEdge).midPoint();
				IPoint p = mesh.createPoint(midPoint.getX(), midPoint.getY());
				Pair<E, E> edges = triangulation.getMeshBuilder().changeConnectivity().splitEdge(p, longestEdge, false);

				F f1 = mesh.faces().getOf(edges.getLeft());
				F f2 = mesh.faces().getTwin(edges.getLeft());

				if(edges.getRight() != null) {

				}
			}
			else {
				sortedFaces.add(face);
			}
		}

		removeTrianglesOutsideBBox();
		removeTrianglesInsideObstacles();
		if(finalize) {
			triangulation.finish();
		}

		logger.info("end triangulation generation");
		return triangulation;
	}

	private void generate(E edge) {
        F face = mesh.faces().getOf(edge);

        E longestEdge = mesh.edges().streamEdgesOf(face)
                .reduce((e1, e2) -> mesh.edges().toLine(e1).length() > mesh.edges().toLine(e2).length() ? e1 : e2)
                .get();

        if(!isCompleted(longestEdge)) {
            IPoint midPoint = mesh.edges().toLine(longestEdge).midPoint();
	        IPoint p = mesh.createPoint(midPoint.getX(), midPoint.getY());
            Pair<E, E> edges = triangulation.getMeshBuilder().changeConnectivity().splitEdge(p, longestEdge, false);

            if(edge.equals(longestEdge)) {
                throw new IllegalArgumentException("invalid start triangle.");
            }

            // simple split
            if(edges.getRight() == null) {
                E e1 = edges.getLeft();
                E e2 = edges.getRight();
                F f1 = mesh.faces().getOf(edges.getLeft());
                F f2 = mesh.faces().getTwin(edges.getLeft());

                if(mesh.edges().streamEdgesOf(f1).anyMatch(e -> e.equals(edge))) {
                    F tmp = f1;
                    f1 = f2;
                    f2 = f1;

                    E tmpE = e1;
                    e1 = e2;
                    e2 = e1;

                    generate(e1);
                    generate(e2);
                }
                else {

                }

            }

        }
        else {
            //sortedFaces.add(face);
        }
    }

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

    @Override
    public IMeshDataStorage<V, E, F> getMeshDataStorage() {
        return triangulation.getMeshDataStorage();
    }

    @Override
    public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
        return triangulation.getMeshBuilder();
    }

    private void removeTrianglesOutsideBBox() {
        boolean removedSome = true;

        while (removedSome) {
            removedSome = false;

            List<F> candidates = mesh.faces().getSurroundingOf(mesh.faces().getOuterBorder());
            for(F face : candidates) {
                if(!mesh.faces().isDestroyed(face) && mesh.vertices().streamVerticesOf(face).anyMatch(v -> !bbox.contains(v))) {
                    triangulation.getMeshBuilder().changeConnectivity().removeFaceAtBorder(face, true);
                    removedSome = true;
                }
            }
        }
    }

    private void removeTrianglesInsideObstacles() {
        List<F> faces = triangulation.getMesh().faces().getAll();
        for(F face : faces) {
            if(!triangulation.getMesh().faces().isDestroyed(face) && distFunc.apply(triangulation.getMesh().faces().toTriangle(face).midPoint()) > 0) {
                triangulation.getMeshBuilder().changeConnectivity().removeFaceAtBorder(face, true);
            }
        }
    }

	/*private void removeTrianglesInsideObstacles() {
		for(VShape shape : boundary) {

			// 1. find a triangle inside the boundary
			VPoint centroid = shape.getPolygonCentroid();

			Optional<F> optFace = triangulation.locate(centroid.getX(), centroid.getY());

			if(optFace.isPresent()) {
				LinkedList<F> candidates = new LinkedList<>();
				candidates.add(optFace.get());

				// 2. as long as there is a face which has a vertex inside the shape remove it
				while (!candidates.isEmpty()) {
					F face = candidates.removeFirst();

					if(!mesh.isDestroyed(face) && mesh.streamEdges(face).map(mesh::toLine).anyMatch(line -> intersectShape(line, shape))) {
						mesh.streamFaces(face)
								//.filter(f -> !face.equals(f)).distinct()
								.forEach(candidate -> candidates.addFirst(candidate));
						triangulation.removeFaceAtBoundary(face, true);
					}
				}
			}
			else {
				logger.warn("no face found");
			}
		}
	}*/

    private boolean intersectShape(final VLine line, final VShape shape) {
        return shape.intersects(line) || shape.contains(line.getP1()) || shape.contains(line.getP2());
    }

    private boolean isCompleted(E edge) {
        if(mesh.edges().isBoundary(edge)){
            edge = mesh.edges().getTwin(edge);
        }

        F face = mesh.faces().getOf(edge);
        F twin = mesh.faces().getTwin(edge);

        VTriangle triangle = mesh.faces().toTriangle(face);
        VLine line = mesh.edges().toLine(edge);

        return (line.length() <= lenFunc.apply(line.midPoint()) && random.nextDouble() < 0.96)
                || (!triangle.intersectsRectangleLine(bbox) && (mesh.faces().isBoundary(twin) || !mesh.faces().toTriangle(twin).intersectsRectangleLine(bbox)))
                || boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D()) || (!mesh.faces().isBoundary(twin) && shape.contains(mesh.faces().toTriangle(twin).getBounds2D())));
    }

    private Collection<E> refine(final E edge) {
        IPoint midPoint = mesh.edges().toLine(edge).midPoint();
	    IPoint p = mesh.createPoint(midPoint.getX(), midPoint.getY());

        if(points.contains(p)) {
            return Collections.emptyList();
        }
        else {
            points.add(p);
            E createdEdge = triangulation.getMeshBuilder().changeConnectivity().splitEdge(p, edge, false).getLeft();
            return mesh.edges().getIncidentEdges(createdEdge);
        }
    }

	/*private IPoint midPoint(final E edge) {
		P p1 = mesh.getPoint(edge);
		P p2 = mesh.getPoint(mesh.getPrev(edge));
		return p2.add(p1).scalarMultiply(0.5);
	}*/

}