package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.*;
import org.vadere.util.opencl.CLDistMesh;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class CLPSMeshing implements IPSMeshing {
	private static final Logger log = LogManager.getLogger(CLPSMeshing.class);
	private boolean illegalMovement = false;
	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private ITriangulation<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> triangulation;
	private Collection<? extends VShape> obstacleShapes;
	private ArrayList<Pair<MeshPoint, MeshPoint>> edges;
	private VRectangle bound;
	private double scalingFactor;
	private PriorityQueue<Pair<PFace<MeshPoint>, Double>> heap;

	private double initialEdgeLen = 0.4;
	private double deps;
	private double sumOfqDesiredEdgeLength;
	private double sumOfqLengths;

	private boolean initialized = false;

	private int numberOfRetriangulations = 0;
	private int numberOfIterations = 0;
	private int numberOfIllegalMovementTests = 0;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;

	private Object gobalAcessSynchronizer = new Object();

	private CLDistMesh<MeshPoint> clDistMesh;

	public CLPSMeshing(
			final IDistanceFunction distanceFunc,
			final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			final VRectangle bound,
			final Collection<? extends VShape> obstacleShapes) {

		this.bound = bound;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen = initialEdgeLen;
		this.obstacleShapes = obstacleShapes;
		this.edges = new ArrayList<>();
		this.deps = 1.4901e-8 * initialEdgeLen;
		this.triangulation = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bound, (x, y) -> new MeshPoint(x, y, false));
	}

	/**
	 * Start with a uniform refined triangulation
	 */
	public void initialize() {
		log.info("##### (start) compute a uniform refined triangulation #####");
		UniformRefinementTriangulation uniformRefinementTriangulation = new UniformRefinementTriangulation(triangulation, bound, obstacleShapes, p -> edgeLengthFunc.apply(p) * initialEdgeLen, distanceFunc);
		uniformRefinementTriangulation.compute();
		retriangulate();
		initialized = true;
		log.info("##### (end) compute a uniform refined triangulation #####");
	}

	public ITriangulation<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> getTriangulation() {
		return triangulation;
	}

	public void execute() {

		/*if(!initialized) {
			initialize();
		}

		double quality = getQuality();
		while (quality < Parameters.qualityMeasurement) {
			step();
			quality = getQuality();
			log.info("quality = " + quality);
		}

		computeScalingFactor();
		computeForces();
		computeDelta();
		updateVertices();
		retriangulate();*/
	}

	public void step() {
		step(true);
	}

	public boolean step(boolean flipAll) {
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
		return clDistMesh.step(flipAll);
		/*log.info("#illegalMovementTests: " + numberOfIllegalMovementTests);
		log.info("#retriangulations: " + numberOfRetriangulations);
		log.info("#steps: " + numberOfIterations);
		log.info("#points: " + getMesh().getVertices().size());*/
	}

	public boolean flipEdges() {
		refresh();
		boolean anyFlip = false;
		// Careful, iterate over all half-edges means iterate over each "real" edge twice!
		/*for(AHalfEdge<MeshPoint> edge : getMesh().getEdgeIt()) {
			if(triangulation.isIllegal(edge)) {
				//triangulation.flip(edge);
				anyFlip = true;
			}
		}*/

		if(clDistMesh != null) {
			clDistMesh.finish();
		}
		clDistMesh = new CLDistMesh<>((AMesh<MeshPoint>) triangulation.getMesh());
		clDistMesh.init();
		return anyFlip;
	}

	public void finish() {
		if(clDistMesh != null) {
			clDistMesh.finish();
		}
		triangulation = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, clDistMesh.getResult(), (x, y) -> new MeshPoint(x, y, false));
		removeTrianglesInsideObstacles();
		triangulation.finalize();
	}

	public void refresh() {
		if(clDistMesh != null) {
			clDistMesh.refresh();
		}
		triangulation = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, clDistMesh.getResult(), (x, y) -> new MeshPoint(x, y, false));
		removeTrianglesInsideObstacles();
		removeLowQualityTriangles();
		triangulation.finalize();
	}

    public void retriangulate() {
        //Set<MeshPoint> points = getMesh().getVertices().stream().map(vertex -> getMesh().getPoint(vertex)).collect(Collectors.toSet());
        //removeLowQualityTriangles();
        triangulation = ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, getMesh().getPoints(), (x, y) -> new MeshPoint(x, y, false));
        removeTrianglesInsideObstacles();
        triangulation.finalize();

        if(clDistMesh != null) {
            clDistMesh.finish();
        }
        else {

		}
		clDistMesh = new CLDistMesh<>((AMesh<MeshPoint>) triangulation.getMesh());
		clDistMesh.init();
    }

    private void removeLowQualityTriangles() {
        List<AFace<MeshPoint>> faces = getMesh().getFaces();
        for(AFace<MeshPoint> face : faces) {
            if(faceToQuality(face) < Parameters.MIN_QUALITY_TRIANGLE) {
                Optional<AHalfEdge<MeshPoint>> optEdge = getMesh().getLinkToBoundary(face);
                if(optEdge.isPresent() && !getMesh().isBoundary(getMesh().getTwin(getMesh().getNext(optEdge.get())))) {
                    AHalfEdge<MeshPoint> edge = getMesh().getNext(optEdge.get());
                    projectToBoundary(getMesh().getVertex(edge));
                    triangulation.removeFace(face, true);
                }
            }
        }
    }

	public boolean isMovementIllegal() {
		for(AFace<MeshPoint> face : getMesh().getFaces()) {
			if(!getMesh().isBoundary(face) && !getMesh().isDestroyed(face) && !triangulation.isCCW(face)) {
				return true;
			}
		}
		return false;
	}

    public IMesh<MeshPoint, AVertex<MeshPoint>, AHalfEdge<MeshPoint>, AFace<MeshPoint>> getMesh() {
        return triangulation.getMesh();
    }

    public double faceToQuality(final AFace<MeshPoint> face) {

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

    private void projectToBoundary(final AVertex<MeshPoint> vertex) {
        MeshPoint position = getMesh().getPoint(vertex);
        double distance = distanceFunc.apply(position);
        if(distance < 0) {
            double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps,0))) - distance) / deps;
            double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0,deps))) - distance) / deps;
            VPoint projection = new VPoint(dGradPX * distance, dGradPY * distance);
            position.subtract(projection);
        }
    }

	private void removeTrianglesInsideObstacles() {
		List<AFace<MeshPoint>> faces = triangulation.getMesh().getFaces();
		for(AFace<MeshPoint> face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distanceFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeFace(face, true);
			}
		}
	}

    @Override
    public Collection<VTriangle> getTriangles() {
		return triangulation.streamTriangles().collect(Collectors.toList());
    }
}
