package org.vadere.meshing.mesh.triangulation.improver;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.ITriangleMeshPointLocator;
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

        /**
         * Start with a uniform refined triangulation
         */
        log.info("##### (start) generate a uniform refined triangulation #####");
        //UniformRefinementTriangulator uniformRefinementTriangulator = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
        //uniformRefinementTriangulator.generate();

        GenRandomPointsSetTriangulator randomTriangulator = new GenRandomPointsSetTriangulator(PTriangleMeshBuilder::new,3000, bound, distanceFunc);
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
        streamVertices().filter(v -> !getMesh().vertices().isAtBoundary(v)).forEach(v -> shrinkForce(v));
        streamVertices().filter(v -> !getMesh().vertices().isAtBoundary(v)).forEach(v -> applyLaplacian(v));
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
        IPoint p = getMesh().vertices().toPoint(vertex);
        long numberOfNeighbours = StreamSupport.stream(getMesh().vertices().adjacentIterableFor(vertex).spliterator(), false).count();

        double weightsSum = StreamSupport.stream(getMesh().vertices().adjacentIterableFor(vertex).spliterator(), false)
                .map(v -> getMesh().vertices().toPoint(v))
                .mapToDouble(m -> 1.0 / m.distance(p)).sum();

        IPoint laplacian = StreamSupport.stream(getMesh().vertices().adjacentIterableFor(vertex).spliterator(), false)
                .map(v -> getMesh().vertices().toPoint(v))
                .map(m -> m.scalarMultiply(1.0 / m.distance(p)))
                .reduce(new VPoint(0,0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / weightsSum);

        return laplacian;
    }

    private IPoint laplacianSquare(final PVertex vertex) {
	    IPoint laplacian = laplacian(vertex);

        long numberOfNeighbours = StreamSupport.stream(getMesh().vertices().adjacentIterableFor(vertex).spliterator(), false).count();
	    IPoint laplacianSquare = StreamSupport.stream(getMesh().vertices().adjacentIterableFor(vertex).spliterator(), false)
                .map(v -> laplacian(v).subtract(laplacian))
                .reduce(new VPoint(0, 0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / numberOfNeighbours);

        return laplacianSquare;
    }

    private void shrinkForce(final PVertex vertex) {
	    IPoint p = getMesh().vertices().toPoint(vertex);


        double alpha = 0.05;
        double beta = 0.5;

	    IPoint shrink = laplacian(vertex).subtract(p).scalarMultiply(alpha);
	    IPoint inflate = laplacian(vertex).subtract(p).scalarMultiply(-beta);

        //getMesh().getPoint(vertex).setVelocity(p.add(shrink.add(inflate)));
	    getMeshDataStorage().setData(vertex, "velocity", p.add(shrink));
    }

    private void inflateForce(final PVertex vertex) {
	    IPoint p = getMesh().vertices().toPoint(vertex);


        double alpha = 1;
        double beta = 0.5;

	    IPoint shrink = laplacian(vertex).subtract(p).scalarMultiply(alpha);
	    IPoint inflate = laplacian(vertex).subtract(p).scalarMultiply(-beta);

        //getMesh().getPoint(vertex).setVelocity(p.add(shrink.add(inflate)));
        getMeshDataStorage().setData(vertex, "velocity", p.add(inflate));
    }

    private void applyLaplacian(final PVertex vertex) {
        IPoint force = getMeshDataStorage().getData(vertex, "velocity", IPoint.class).get();
        getTriangulation().getMeshBuilder().vertices().setCoords(vertex, force.getX(), force.getY());
    }

    private void removeTrianglesInsideObstacles() {
        List<PFace> faces = triangulation.getMesh().faces().getAll();
        for(PFace face : faces) {
            if(!triangulation.getMesh().faces().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().faces().toTriangle(face).midPoint()) > 0) {
                triangulation.getMeshBuilder().changeConnectivity().removeFaceAtBorder(face, true);
            }
        }
    }

    // helper methods
    private Stream<PHalfEdge> streamEdges() {
        return runParallel ? getMesh().edges().streamParallel() : getMesh().edges().stream();
    }

    private Stream<PVertex> streamVertices() {
        return runParallel ? getMesh().vertices().streamParallel() : getMesh().vertices().stream();
    }

    @Override
    public IMesh<PVertex, PHalfEdge, PFace> getMesh() {
        return triangulation.getMesh();
    }

    // TODO: parallize the whole triangulation
    public void retriangulate() {
        triangulation = IIncrementalTriangulation.createPTriangulation(ITriangleMeshPointLocator.Type.DELAUNAY_HIERARCHY, getMesh().vertices().toPoints());
        removeTrianglesInsideObstacles();
        triangulation.finish();
    }
}
