package org.vadere.meshing.mesh.triangulation.improver.eikmesh.opencl;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulatorSFC;
import org.vadere.meshing.opencl.CLDistMesh;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.util.geometry.shapes.*;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 *
 */
public class CLEikMesh implements IMeshImprover<AVertex, AHalfEdge, AFace>, ITriangulator<AVertex, AHalfEdge, AFace> {
    private static final Logger log = Logger.getLogger(CLEikMesh.class);
    private boolean illegalMovement = false;
    private IDistanceFunction distanceFunc;
    private IEdgeLengthFunction edgeLengthFunc;
    private final IMeshSupplier<AVertex, AHalfEdge, AFace> meshSupplier;
    private IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation;
    private Collection<? extends VShape> obstacleShapes;
    private ArrayList<Pair<EikMeshPoint, EikMeshPoint>> edges;
    private VRectangle bound;
    private double scalingFactor;
    private PriorityQueue<Pair<PFace, Double>> heap;

	public final static double MIN_TRIANGLE_QUALITY = 0.1;

    private double initialEdgeLen = 0.4;
    private double deps;
    private double sumOfqDesiredEdgeLength;
    private double sumOfqLengths;

    private boolean initialized = false;

    private int numberOfRetriangulations = 0;
    private int numberOfIterations = 0;
    private int numberOfIllegalMovementTests = 0;
    private double minDeltaTravelDistance = 0.0;
    private double delta = 0.2;

    private Object gobalAcessSynchronizer = new Object();

    private CLDistMesh clDistMesh;
    private boolean hasToRead = false;
    private int nSteps;
    private final static int MAX_STEPS = 200;

    public CLEikMesh(
            final IDistanceFunction distanceFunc,
            final IEdgeLengthFunction edgeLengthFunc,
            final VRectangle bound,
            final Collection<? extends VShape> obstacleShapes,
            final IMeshSupplier<AVertex, AHalfEdge, AFace> meshSupplier) {

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
     * @throws OpenCLException if there is OpenCL is not or not correct installed.
     */
    public void initialize() throws OpenCLException {
        log.info("##### (start) compute a uniform refined triangulation #####");
        //UniformRefinementTriangulator uniformRefinementTriangulation = new UniformRefinementTriangulator(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
        //uniformRefinementTriangulation.generate();

        log.info("##### (end) compute a uniform refined triangulation #####");

        log.info("##### (start) generate a triangulation #####");
        GenUniformRefinementTriangulatorSFC<AVertex, AHalfEdge, AFace> uniformRefinementTriangulation = new GenUniformRefinementTriangulatorSFC(
                meshSupplier,
                bound,
                obstacleShapes,
		        edgeLengthFunc,
                distanceFunc);
        triangulation = uniformRefinementTriangulation.generate();

        // TODO: dirty cast.
        clDistMesh = new CLDistMesh((AMesh)triangulation.getMesh());
	    clDistMesh.init();

	    //clDistMesh.refresh();
        initialized = true;
        log.info("##### (end) generate a triangulation #####");
    }

    public IIncrementalTriangulation<AVertex, AHalfEdge, AFace> getTriangulation() {
        return triangulation;
    }

    @Override
    public IIncrementalTriangulation<AVertex, AHalfEdge, AFace> generate(boolean finalize) {
        return generate();
    }

	@Override
	public IIncrementalTriangulation<AVertex, AHalfEdge, AFace> generate() {
		try {
			initialize();

			// TODO: quality check!
			while (nSteps < MAX_STEPS) {
				improve();
				//log.info("quality: " + quality);
			}
			refresh();
			finish();
			return triangulation;
		}
		catch (OpenCLException e) {
			log.error(e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void execute() {
    	try {
		    if(!initialized) {
			    initialize();
		    }

		    while (!isFinished()) {
			    step();
		    }
	    }
	    catch (OpenCLException e) {
    		log.error(e.getMessage());
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
        minDeltaTravelDistance = Double.MAX_VALUE;
        illegalMovement = false;
        //log.info(scalingFactor);


        //flipEdges();
        //retriangulate();

        // get new cooridnates

        // there might be some illegal movements
        if(minDeltaTravelDistance < 0.0) {
            illegalMovement = isMovementIllegal();
            numberOfIllegalMovementTests++;
        }

        if(illegalMovement) {
            //retriangulate();
//			while (flipEdges());

            numberOfRetriangulations++;
        }
        else {
//			flipEdges();
        }

        if(minDeltaTravelDistance < 0) {
//			computeMaxLegalMovements();
        }

        numberOfIterations++;
	    try {
		    return clDistMesh.step(flipAll);
	    } catch (OpenCLException e) {
		    e.printStackTrace();
		    return false;
	    }
		/*log.info("#illegalMovementTests: " + numberOfIllegalMovementTests);
		log.info("#retriangulations: " + numberOfRetriangulations);
		log.info("#steps: " + numberOfIterations);
		log.info("#points: " + getMesh().getVertices().size());*/
    }

    public void finish() {
	    try {
		    clDistMesh.finish();
	    } catch (OpenCLException e) {
		    e.printStackTrace();
	    }
    }

    public void refresh() {
        if(clDistMesh != null) {
            clDistMesh.refresh();
            hasToRead = false;
        }
    }

    private void removeLowQualityTriangles() {
        List<AFace> faces = getMesh().getFaces();
        for(AFace face : faces) {
            if(faceToQuality(face) < MIN_TRIANGLE_QUALITY) {
                Optional<AHalfEdge> optEdge = getMesh().getLinkToBoundary(face);
                if(optEdge.isPresent() && !getMesh().isBoundary(getMesh().getTwin(getMesh().getNext(optEdge.get())))) {
                    AHalfEdge edge = getMesh().getNext(optEdge.get());
                    projectToBoundary(getMesh().getVertex(edge));
                    triangulation.removeFaceAtBorder(face, true);
                }
            }
        }
    }

    public boolean isMovementIllegal() {
        for(AFace face : getMesh().getFaces()) {
            if(!getMesh().isBoundary(face) && !getMesh().isDestroyed(face) && !triangulation.isCCW(face)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized AMesh getMesh() {
        refresh();
        // TODO: dirty casting
        return (AMesh) triangulation.getMesh();
    }

    public double faceToQuality(final AFace face) {

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

    private void projectToBoundary(final AVertex vertex) {
    	// TODO: get rid of VPoint
	    VPoint position = getMesh().toPoint(vertex);
        double distance = distanceFunc.apply(position);
        if(distance < 0) {
            double dGradPX = (distanceFunc.apply(position.add(new VPoint(deps,0))) - distance) / deps;
            double dGradPY = (distanceFunc.apply(position.add(new VPoint(0,deps))) - distance) / deps;
            VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
            position.subtract(projection);
        }
    }

    private void removeTrianglesInsideObstacles() {
        List<AFace> faces = triangulation.getMesh().getFaces();
        for(AFace face : faces) {
            if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
                triangulation.removeFaceAtBorder(face, true);
            }
        }
    }

    @Override
    public Collection<VTriangle> getTriangles() {
        refresh();
        return triangulation.streamTriangles().collect(Collectors.toList());
    }

    @Override
    public void improve() {
        step();
        nSteps++;
    }
}
