package org.vadere.meshing.mesh.triangulation.improver.eikmesh.opencl;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.meshing.mesh.triangulation.triangulator.ITriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.UniformRefinementTriangulatorSFC;
import org.vadere.meshing.opencl.CLDistMeshHE;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.util.geometry.shapes.*;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 */
public class CLEikMeshHE<P extends EikMeshPoint, CE, CF> implements IMeshImprover<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>>, ITriangulator<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> {
    private static final Logger log = LogManager.getLogger(CLEikMesh.class);
    private boolean illegalMovement = false;
    private IDistanceFunction distanceFunc;
    private IEdgeLengthFunction edgeLengthFunc;
    private final IMeshSupplier<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> meshSupplier;
    private IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> triangulation;
    private Collection<? extends VShape> obstacleShapes;
    private ArrayList<Pair<EikMeshPoint, EikMeshPoint>> edges;
    private VRectangle bound;
    private double scalingFactor;
    private PriorityQueue<Pair<PFace<EikMeshPoint, CE, CF>, Double>> heap;

    private double initialEdgeLen = 0.4;
    private double deps;
    private double sumOfqDesiredEdgeLength;
    private double sumOfqLengths;

    private boolean initialized = false;

    private int numberOfRetriangulations = 0;
    private int numberOfIterations = 0;
    private int numberOfIllegalMovementTests = 0;
    private double minDeltaTravelDistance = 0.0;
    private double delta =  0.2;

    private final static int MAX_STEPS = 200;
    private int nSteps;

    private Object gobalAcessSynchronizer = new Object();

    private CLDistMeshHE<P, CE, CF> clDistMesh;
    private boolean hasToRead = false;

    public CLEikMeshHE(
            final IDistanceFunction distanceFunc,
            final IEdgeLengthFunction edgeLengthFunc,
            final double initialEdgeLen,
            final VRectangle bound,
            final Collection<? extends VShape> obstacleShapes,
            final IMeshSupplier<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> meshSupplier) {

        this.bound = bound;
        this.distanceFunc = distanceFunc;
        this.edgeLengthFunc = edgeLengthFunc;
        this.initialEdgeLen = initialEdgeLen;
        this.obstacleShapes = obstacleShapes;
        this.meshSupplier = meshSupplier;
        this.edges = new ArrayList<>();
        this.deps = 1.4901e-8 * initialEdgeLen;
        this.nSteps = 0;
    }

    /**
     * Start with a uniform refined triangulation
     * @throws OpenCLException if OpenCL is not or not correct installed.
     */
    public void initialize() throws OpenCLException {
        log.info("##### (start) compute a uniform refined triangulation #####");
        //UniformRefinementTriangulator uniformRefinementTriangulation = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
        //uniformRefinementTriangulation.generate();

        log.info("##### (end) compute a uniform refined triangulation #####");

        log.info("##### (start) generate a triangulation #####");
        UniformRefinementTriangulatorSFC<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> uniformRefinementTriangulation = new UniformRefinementTriangulatorSFC(
		        meshSupplier,
                bound,
                obstacleShapes,
                edgeLengthFunc,
		        initialEdgeLen,
                distanceFunc,
		        new ArrayList());
        triangulation = uniformRefinementTriangulation.generate();

        // TODO: dirty cast.
        clDistMesh = new CLDistMeshHE<>((AMesh<P, CE, CF>)triangulation.getMesh());
        clDistMesh.init();
        clDistMesh.refresh();
        initialized = true;
        log.info("##### (end) generate a triangulation #####");
    }

    public IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> getTriangulation() {
        return triangulation;
    }

    @Override
    public IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> generate() {
		try {
			if(!initialized) {
				initialize();
			}

			while (!isFinished()) {
				step();
			}

			return triangulation;
		}
		catch (OpenCLException e) {
			log.error(e);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }

    public boolean isFinished() {
        return nSteps >= MAX_STEPS;
    }

    public void step() {
        step(true);
    }

    public boolean step(boolean flipAll) {
        hasToRead = true;
        nSteps++;
        return clDistMesh.step(flipAll);
    }

    public void finish() {
        clDistMesh.finish();
    }

    public void refresh() {
        if(clDistMesh != null) {
            clDistMesh.refresh();
        }
    }

    private void removeLowQualityTriangles() {
        List<AFace<CF>> faces = getMesh().getFaces();
        for(AFace<CF> face : faces) {
            if(faceToQuality(face) < Parameters.MIN_TRIANGLE_QUALITY) {
                Optional<AHalfEdge<CE>> optEdge = getMesh().getLinkToBoundary(face);
                if(optEdge.isPresent() && !getMesh().isBoundary(getMesh().getTwin(getMesh().getNext(optEdge.get())))) {
                    AHalfEdge<CE> edge = getMesh().getNext(optEdge.get());
                    projectToBoundary(getMesh().getVertex(edge));
                    triangulation.removeFaceAtBorder(face, true);
                }
            }
        }
    }

    public boolean isMovementIllegal() {
        for(AFace<CF> face : getMesh().getFaces()) {
            if(!getMesh().isBoundary(face) && !getMesh().isDestroyed(face) && !triangulation.isCCW(face)) {
                return true;
            }
        }
        return false;
    }

    public synchronized AMesh<P, CE, CF> getMesh() {
        if(hasToRead) {
            hasToRead = false;
            clDistMesh.refresh();
        }
        // TODO: dirty casting
        return (AMesh<P, CE, CF>) triangulation.getMesh();
    }

    public double faceToQuality(final AFace<CF> face) {

        VLine[] lines = getMesh().toTriangle(face).getLines();
        double a = lines[0].length();
        double b = lines[1].length();
        double c = lines[2].length();
        double part = 0.0;
        if(a != 0.0 && b != 0.0 && c != 0.0) {
            part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
        }
        else {
            throw new IllegalArgumentException(face + " is not a feasible triangle!");
        }
        return part;
    }

    private void projectToBoundary(final AVertex<P> vertex) {
        P position = getMesh().getPoint(vertex);
        double distance = distanceFunc.apply(position);
        if(distance < 0) {
            double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps,0))) - distance) / deps;
            double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0,deps))) - distance) / deps;
            VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
            position.subtract(projection);
        }
    }

    private void removeTrianglesInsideObstacles() {
        List<AFace<CF>> faces = triangulation.getMesh().getFaces();
        for(AFace<CF> face : faces) {
            if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
                triangulation.removeFaceAtBorder(face, true);
            }
        }
    }

    @Override
    public Collection<VTriangle> getTriangles() {
        clDistMesh.refresh();
        return triangulation.streamTriangles().collect(Collectors.toList());
    }

    @Override
    public void improve() {
        step();
    }

}
