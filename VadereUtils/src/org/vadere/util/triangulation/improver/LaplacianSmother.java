package org.vadere.util.triangulation.improver;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.*;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;
import org.vadere.util.triangulation.adaptive.MeshPoint;
import org.vadere.util.triangulation.adaptive.Parameters;
import org.vadere.util.triangulation.triangulator.RandomPointsSetTriangulator;
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Benedikt Zoennchen
 */
public class LaplacianSmother implements IMeshImprover<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> {
    private static final Logger log = LogManager.getLogger(LaplacianSmother.class);

    private IDistanceFunction distanceFunc;
    private IEdgeLengthFunction edgeLengthFunc;
    private ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> triangulation;
    private Collection<? extends VShape> obstacleShapes;
    private ArrayList<Pair<MeshPoint, MeshPoint>> edges;
    private final VRectangle bound;
    private final double initialEdgeLen;

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
        this.initialEdgeLen = initialEdgeLen;
        this.obstacleShapes = obstacleShapes;
        this.triangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bound, (x, y) -> new MeshPoint(x, y, false));

        /**
         * Start with a uniform refined triangulation
         */
        log.info("##### (start) generate a uniform refined triangulation #####");
        //UniformRefinementTriangulator uniformRefinementTriangulator = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
        //uniformRefinementTriangulator.generate();

        RandomPointsSetTriangulator randomTriangulator = new RandomPointsSetTriangulator(triangulation, 3000, bound, distanceFunc);
        randomTriangulator.generate();
        log.info("##### (end) generate a uniform refined triangulation #####");
    }


    @Override
    public Collection<VTriangle> getTriangles() {
        return triangulation.streamTriangles().collect(Collectors.toList());
    }

    @Override
    public void improve() {
        streamVertices().forEach(v -> computeForce(v));
        streamVertices().forEach(v -> applyLaplacian(v));
    }

    @Override
    public ITriangulation<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getTriangulation() {
        return triangulation;
    }

    private void computeForce(final PVertex<MeshPoint> vertex) {
        VPoint p = getMesh().getPoint(vertex).toVPoint();

        long numberOfNeighbours = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false).count();
        log.info("number of neighbours = " + numberOfNeighbours);
        VPoint laplacian = StreamSupport.stream(getMesh().getAdjacentVertexIt(vertex).spliterator(), false)
                .map(v -> getMesh().getPoint(v))
                .map(m -> m.toVPoint())
                .reduce(new VPoint(0,0), (p1, p2) -> p1.add(p2))
                .scalarMultiply(1.0 / numberOfNeighbours);
                //.subtract(p);

        log.info(laplacian);

        getMesh().getPoint(vertex).setVelocity(laplacian);
    }

    private void applyLaplacian(final PVertex<MeshPoint> vertex) {
        VPoint p = getMesh().getPoint(vertex).toVPoint();
        IPoint force = getMesh().getPoint(vertex).getVelocity();
        getMesh().getPoint(vertex).set(force.getX(), force.getY());

    }

    // helper methods
    private Stream<PHalfEdge<MeshPoint>> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<PVertex<MeshPoint>> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

    private IMesh<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getMesh() {
        return triangulation.getMesh();
    }
}
