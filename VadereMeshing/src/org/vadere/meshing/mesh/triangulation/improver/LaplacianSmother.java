package org.vadere.meshing.mesh.triangulation.improver;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRandomPointsSetTriangulator;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.*;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Benedikt Zoennchen
 */
public class LaplacianSmother implements IPMeshImprover<EikMeshPoint> {
    private static final Logger log = LogManager.getLogger(LaplacianSmother.class);

    private IDistanceFunction distanceFunc;
    private IEdgeLengthFunction edgeLengthFunc;
    private IIncrementalTriangulation<EikMeshPoint, Object, Object, PVertex<EikMeshPoint, Object, Object>, PHalfEdge<EikMeshPoint, Object, Object>, PFace<EikMeshPoint, Object, Object>> triangulation;
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

        PMesh<EikMeshPoint, Object, Object> mesh = new PMesh<>((x, y) -> new EikMeshPoint(x, y, false));
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
	public IIncrementalTriangulation<EikMeshPoint, Object, Object, PVertex<EikMeshPoint, Object, Object>, PHalfEdge<EikMeshPoint, Object, Object>, PFace<EikMeshPoint, Object, Object>> getTriangulation() {
		return triangulation;
	}

	private VPoint laplacian(final PVertex<EikMeshPoint, Object, Object> vertex) {
        VPoint p = getMesh().getPoint(vertex).toVPoint();
        long numberOfNeighbours = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false).count();

        double weightsSum = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> getMesh().getPoint(v))
                .map(m -> m.toVPoint())
                .mapToDouble(m -> 1.0 / m.distance(p)).sum();

        VPoint laplacian = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> getMesh().getPoint(v))
                .map(m -> m.toVPoint())
                .map(m -> m.scalarMultiply(1.0 / m.distance(p)))
                .reduce(new VPoint(0,0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / weightsSum);

        return laplacian;
    }

    private VPoint laplacianSquare(final PVertex<EikMeshPoint, Object, Object> vertex) {
        VPoint laplacian = laplacian(vertex);

        long numberOfNeighbours = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false).count();
        VPoint laplacianSquare = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> laplacian(v).subtract(laplacian))
                .reduce(new VPoint(0, 0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / numberOfNeighbours);

        return laplacianSquare;
    }

    private void shrinkForce(final PVertex<EikMeshPoint, Object, Object> vertex) {
        VPoint p = getMesh().getPoint(vertex).toVPoint();


        double alpha = 0.05;
        double beta = 0.5;

        VPoint shrink = laplacian(vertex).subtract(p).scalarMultiply(alpha);
        VPoint inflate = laplacian(vertex).subtract(p).scalarMultiply(-beta);

        //getMesh().getPoint(vertex).setVelocity(p.add(shrink.add(inflate)));
        getMesh().getPoint(vertex).setVelocity(p.add(shrink));
    }

    private void inflateForce(final PVertex<EikMeshPoint, Object, Object> vertex) {
        VPoint p = getMesh().getPoint(vertex).toVPoint();


        double alpha = 1;
        double beta = 0.5;

        VPoint shrink = laplacian(vertex).subtract(p).scalarMultiply(alpha);
        VPoint inflate = laplacian(vertex).subtract(p).scalarMultiply(-beta);

        //getMesh().getPoint(vertex).setVelocity(p.add(shrink.add(inflate)));
        getMesh().getPoint(vertex).setVelocity(p.add(inflate));
    }

    private void applyLaplacian(final PVertex<EikMeshPoint, Object, Object> vertex) {
        VPoint p = getMesh().getPoint(vertex).toVPoint();
        IPoint force = getMesh().getPoint(vertex).getVelocity();

        getMesh().getPoint(vertex).set(force.getX(), force.getY());

    }

    /**
     * projects the vertex back if it is no longer inside the boundary or inside an obstacle.
     *
     * @param vertex the vertex
     */
    private void projectBackVertex(final PVertex<EikMeshPoint, Object, Object> vertex) {
        EikMeshPoint position = getMesh().getPoint(vertex);
        double distance = distanceFunc.apply(position);
        if(distance > 0) {
            double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps, 0))) - distance) / deps;
            double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0, deps))) - distance) / deps;
            VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
            position.subtract(projection);
        }
    }

    private void removeTrianglesInsideObstacles() {
        List<PFace<EikMeshPoint, Object, Object>> faces = triangulation.getMesh().getFaces();
        for(PFace<EikMeshPoint, Object, Object> face : faces) {
            if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
                triangulation.removeFaceAtBorder(face, true);
            }
        }
    }

    // helper methods
    private Stream<PHalfEdge<EikMeshPoint, Object, Object>> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<PVertex<EikMeshPoint, Object, Object>> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

    @Override
    public IMesh<EikMeshPoint, Object, Object, PVertex<EikMeshPoint, Object, Object>, PHalfEdge<EikMeshPoint, Object, Object>, PFace<EikMeshPoint, Object, Object>> getMesh() {
        return triangulation.getMesh();
    }

    // TODO: parallize the whole triangulation
    public void retriangulate() {
        triangulation = IIncrementalTriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, getMesh().getPoints(), (x, y) -> new EikMeshPoint(x, y, false));
        removeTrianglesInsideObstacles();
        triangulation.finish();
    }
}
