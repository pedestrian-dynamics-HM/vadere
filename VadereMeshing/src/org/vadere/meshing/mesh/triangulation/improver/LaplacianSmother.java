package org.vadere.meshing.mesh.triangulation.improver;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRandomPointsSetTriangulator;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.*;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Benedikt Zoennchen
 */
public class LaplacianSmother implements IPMeshImprover {
    private static final Logger log = Logger.getLogger(LaplacianSmother.class);

    private IDistanceFunction distanceFunc;
    private IEdgeLengthFunction edgeLengthFunc;
    private IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation;
    private Collection<? extends VShape> obstacleShapes;
    private ArrayList<Pair<EikMeshPoint, EikMeshPoint>> edges;
    private final VRectangle bound;
    private final double initialEdgeLen;
    private final double deps;

    private double delta = 0.5;
    private boolean runParallel = false;

    private Object gobalAcessSynchronizer = new Object();

    public LaplacianSmother(final IDistanceFunction distanceFunc,
                            final IEdgeLengthFunction edgeLengthFunc,
                            final double initialEdgeLen,
                            final VRectangle bound,
                            final Collection<? extends VShape> obstacleShapes) {

        this.bound = bound;
        this.distanceFunc = distanceFunc;
        this.edgeLengthFunc = edgeLengthFunc;
        this.deps = 1.4901e-8 * initialEdgeLen;
        this.initialEdgeLen = initialEdgeLen;
        this.obstacleShapes = obstacleShapes;

        PMesh mesh = new PMesh();
        /**
         * Start with a uniform refined triangulation
         */
        log.info("##### (start) generate a uniform refined triangulation #####");
        //UniformRefinementTriangulator uniformRefinementTriangulator = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
        //uniformRefinementTriangulator.generate();

        GenRandomPointsSetTriangulator randomTriangulator = new GenRandomPointsSetTriangulator(mesh, 3000, bound, distanceFunc);
	    triangulation = randomTriangulator.generate();
        removeTrianglesInsideObstacles();
        log.info("##### (end) generate a uniform refined triangulation #####");
    }


    @Override
    public Collection<VTriangle> getTriangles() {
        return triangulation.streamTriangles().collect(Collectors.toList());
    }

    @Override
    public void improve() {
        streamVertices().filter(v -> !getMesh().isAtBoundary(v)).forEach(v -> shrinkForce(v));
        streamVertices().filter(v -> !getMesh().isAtBoundary(v)).forEach(v -> applyLaplacian(v));
        //streamVertices().filter(v -> !getMesh().isAtBoundary(v)).forEach(v -> inflateForce(v));
        //streamVertices().filter(v -> !getMesh().isAtBoundary(v)).forEach(v -> applyLaplacian(v));
        retriangulate();
        //streamVertices().filter(v -> !getMesh().isAtBoundary(v)).forEach(v -> projectBackVertex(v));
    }

	@Override
	public IIncrementalTriangulation<PVertex, PHalfEdge, PFace> getTriangulation() {
		return triangulation;
	}

	private IPoint laplacian(final PVertex vertex) {
        IPoint p = getMesh().getPoint(vertex);
        long numberOfNeighbours = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false).count();

        double weightsSum = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> getMesh().getPoint(v))
                .mapToDouble(m -> 1.0 / m.distance(p)).sum();

        IPoint laplacian = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> getMesh().getPoint(v))
                .map(m -> m.scalarMultiply(1.0 / m.distance(p)))
                .reduce(new VPoint(0,0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / weightsSum);

        return laplacian;
    }

    private IPoint laplacianSquare(final PVertex vertex) {
	    IPoint laplacian = laplacian(vertex);

        long numberOfNeighbours = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false).count();
	    IPoint laplacianSquare = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> laplacian(v).subtract(laplacian))
                .reduce(new VPoint(0, 0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / numberOfNeighbours);

        return laplacianSquare;
    }

    private void shrinkForce(final PVertex vertex) {
	    IPoint p = getMesh().getPoint(vertex);


        double alpha = 0.05;
        double beta = 0.5;

	    IPoint shrink = laplacian(vertex).subtract(p).scalarMultiply(alpha);
	    IPoint inflate = laplacian(vertex).subtract(p).scalarMultiply(-beta);

        //getMesh().getPoint(vertex).setVelocity(p.add(shrink.add(inflate)));
	    getMesh().setData(vertex, "velocity", p.add(shrink));
    }

    private void inflateForce(final PVertex vertex) {
	    IPoint p = getMesh().getPoint(vertex);


        double alpha = 1;
        double beta = 0.5;

	    IPoint shrink = laplacian(vertex).subtract(p).scalarMultiply(alpha);
	    IPoint inflate = laplacian(vertex).subtract(p).scalarMultiply(-beta);

        //getMesh().getPoint(vertex).setVelocity(p.add(shrink.add(inflate)));
	    getMesh().setData(vertex, "velocity", p.add(inflate));
    }

    private void applyLaplacian(final PVertex vertex) {
        IPoint force = getMesh().getData(vertex, "velocity", IPoint.class).get();
        getMesh().setCoords(vertex, force.getX(), force.getY());
    }

    private void removeTrianglesInsideObstacles() {
        List<PFace> faces = triangulation.getMesh().getFaces();
        for(PFace face : faces) {
            if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
                triangulation.removeFaceAtBorder(face, true);
            }
        }
    }

    // helper methods
    private Stream<PHalfEdge> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<PVertex> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

    @Override
    public IMesh<PVertex, PHalfEdge, PFace> getMesh() {
        return triangulation.getMesh();
    }

    // TODO: parallize the whole triangulation
    public void retriangulate() {
        triangulation = IIncrementalTriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, getMesh().getPoints());
        removeTrianglesInsideObstacles();
        triangulation.finish();
    }
}
