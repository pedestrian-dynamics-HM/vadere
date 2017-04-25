package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.triangulations.IncrementalTriangulation;
import org.vadere.util.geometry.ConstantLineIterator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MLine;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.geom.PathIterator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class  PSDistmesh {
	private Set<MeshPoint> points = new HashSet<>();
	private Set<MLine<MeshPoint>> lines = new HashSet<>();
	private IncrementalTriangulation<MeshPoint, PHalfEdge<MeshPoint>, PFace<MeshPoint>> bowyerWatson;
	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction relativeDesiredEdgeLengthFunc;
	private VRectangle regionBoundingBox;
	private Collection<? extends VShape> obstacles;
	private int steps;

	// Parameters
	private double initialEdgeLen;
	private double geps;
	private double deps;
	private boolean firstStep = true;
	private double maxMovementLen = 0;


	public PSDistmesh(final VRectangle regionBoundingBox,
	                  final Collection<? extends VShape> obstacles,
	                  final double initialEdgeLen,
	                  final Function<IPoint, Double> density) {

		init(regionBoundingBox, obstacles, initialEdgeLen);
		distanceFunc = IDistanceFunction.create(regionBoundingBox, obstacles);
		relativeDesiredEdgeLengthFunc = IEdgeLengthFunction.create(regionBoundingBox, density);
		//relativeDesiredEdgeLengthFunc = IEdgeLengthFunction.create(1);
		this.points = generatePoints();
	}

	public PSDistmesh(final VRectangle regionBoundingBox,
	                  final Collection<? extends VShape> obstacles,
	                  final double initialEdgeLen,
	                  final boolean uniform) {

		init(regionBoundingBox, obstacles, initialEdgeLen);
		distanceFunc = IDistanceFunction.create(regionBoundingBox, obstacles);
		if(uniform) {
			relativeDesiredEdgeLengthFunc = IEdgeLengthFunction.create();
		}
		else {
			relativeDesiredEdgeLengthFunc = IEdgeLengthFunction.create(regionBoundingBox, distanceFunc);
		}

		this.points = generatePoints();
		System.out.println("unrejected number of points: " + points.size());
	}

	private void init(final VRectangle regionBoundingBox,
	                  final Collection<? extends VShape> obstacles,
	                  final double initialEdgeLen) {
		this.regionBoundingBox = regionBoundingBox;
		this.initialEdgeLen = initialEdgeLen;
		this.geps = .001 * initialEdgeLen;
		this.deps = 1.4901e-8 * initialEdgeLen;
		this.obstacles = obstacles;
		this.steps = 0;
	}

	public void execute() {
		double test = qualityCheck();
		while(test <= Parameters.qualityMeasurement) {
			System.out.println("quality: " + test);
			step();
			test = qualityCheck();
		}
		cleanUp();
	}

	public boolean hasConverged() {
		double test = qualityCheck();
		System.out.println("quality: " + test);
		return test > Parameters.qualityMeasurement;
	}

	public boolean hasMaximalSteps() {
		return steps >= Parameters.MAX_NUMBER_OF_STEPS;
	}

	/**
	 * Remove all triangles intersecting any obstacle shape.
	 */
	public void cleanUp() {
		/*bowyerWatson
				.stream()
				.map(face -> face.toTriangle())
				.filter(triangle -> triangle.isNonAcute())
				.map(triangle -> triangle.getCircumcenter())
				.collect(Collectors.toSet())
				.forEach(p -> bowyerWatson.insert(new MeshPoint(p, false)));*/

	}

	private void reTriangulate() {
		if(firstStep || maxMovementLen / initialEdgeLen > Parameters.TOL) {
			maxMovementLen = 0;
			bowyerWatson = new IncrementalTriangulation<>(new PMesh<>((x, y) -> new MeshPoint(x, y, false)), points);

			System.out.println("triangulation started");
			bowyerWatson.compute();
			System.out.println("triangulation finished");

			IMesh<MeshPoint, PHalfEdge<MeshPoint>, PFace<MeshPoint>> mesh = bowyerWatson.getMesh();
			Function<PHalfEdge<MeshPoint>, MLine<MeshPoint>> toLine = edge -> new MLine<>(mesh.getVertex(mesh.getPrev(edge)), mesh.getVertex(edge));

			// compute the line and points again, since we filter some triangles
			lines = bowyerWatson.streamFaces()
					.filter(face -> distanceFunc.apply(bowyerWatson.getMesh().toTriangle(face).midPoint()) < -geps)
					.flatMap(face ->mesh.streamEdges(face).map(halfEdge -> toLine.apply(halfEdge)))
					.collect(Collectors.toSet());

			points = lines.stream().flatMap(line -> line.streamPoints()).collect(Collectors.toSet());

			System.out.println("number of edges: " + lines.size());
			System.out.println("number of points: " + points.size());
		}
	}

	/*
	Stellt den Verlauf der Iterationen dar. Innerhalb der while(true) passiert eine Iteration des Algorithmus
	 */
	public void step()
	{
		steps++;
		reTriangulate();

		// compute the forces / velocities for each line
		double scalingFactor = computeScalingFactor(lines);
		System.out.println("scaling factor: " + scalingFactor);
		for(MLine line : lines) {
			double len = line.length();
			double desiredLen = relativeDesiredEdgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
			//double desiredLen = 10.4 * 1.2;
			//len = 10;
			double lenDiff = Math.max(desiredLen - len, 0);
			//double lenDiff = desiredLen - len;
			//double force = desiredLen - len;
			VPoint directedForce = new VPoint((lenDiff / len) * (line.getX1() - line.getX2()), (lenDiff / len) * (line.getY1() - line.getY2()));
			//VPoint directedForce = new VPoint(3, 3);
			line.setVelocity(directedForce);
			//line.setVelocity(line.getVelocity().add(new VPoint(Math.random(), Math.random())));
			//System.out.println("directed: " + directedForce);
		}

		// compute the total forces / velocities
		for(MLine<MeshPoint> line : lines) {
			if(!line.p1.isFixPoint()) {
				//line.p1.increaseVelocity(new VPoint(3, 3));
				line.p1.increaseVelocity(line.getVelocity());
			}

			if(!line.p2.isFixPoint()) {
				//line.p2.decreaseVelocity(new VPoint(3, 3));
				line.p2.decreaseVelocity(line.getVelocity());
			}
		}

		// do the euler step, we change the mutable point, this may destroy there unquieness.
		Set<MeshPoint> newPoints = new HashSet();
		for(MeshPoint point : points) {
			if(!point.isFixPoint()) {
				IPoint movement = point.getVelocity().scalarMultiply(Parameters.DELTAT);
				double movementLen = point.getVelocity().distanceToOrigin();

				//System.out.println("movement="+movementLen);

				if(maxMovementLen < movementLen) {
					maxMovementLen = movementLen;
				}

				//if(distanceFunc.apply(point.toVPoint().add(movement)) <= 0) {
				point.add(movement);
				//}

				point.setVelocity(new VPoint(0, 0));

				// back projection
				double distance = distanceFunc.apply(point);
				if(distance > 0) {
					double dGradPX = (distanceFunc.apply(point.add(new VPoint(deps,0))) - distance) / deps;
					double dGradPY = (distanceFunc.apply(point.add(new VPoint(0,deps))) - distance) / deps;

					point.subtract(new VPoint(dGradPX * distance, dGradPY * distance));
				}
			}
			newPoints.add(point);
		}
		points = newPoints;
		System.out.println("maxmove: " + maxMovementLen);

		firstStep = false;
	}

	/*
	Berechnet die durchschnittliche Qualität aller erzeugten Dreiecke
	 */
	public double qualityCheck()
	{
		if(bowyerWatson == null) {
			return 0.0;
		}
		else {
			Collection<VTriangle> triangles = bowyerWatson.getTriangles();
			double aveSum = 0;
			for(VTriangle triangle : triangles) {
				VLine[] lines = triangle.getLines();
				double a = lines[0].length();
				double b = lines[1].length();
				double c = lines[2].length();
				double part = 0.0;
				if(a != 0.0 && b != 0.0 && c != 0.0) {
					part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
				}
				aveSum += part;
				if(Double.isNaN(part) || Double.isNaN(aveSum)) {
					throw new IllegalArgumentException(triangle + " is not a feasible triangle!");
				}
			}

			return aveSum / triangles.size();
		}
	}


	/**
	 * Formula 2.6
	 * @param lines
	 * @return
	 */
	private double computeScalingFactor(final Collection<MLine<MeshPoint>> lines) {
		double sumOfqDesiredEdgeLength = 0;
		double sumOfqLengths = 0;
		for(MLine line : lines) {
			VPoint midPoint = line.midPoint();
			double desiredEdgeLength = relativeDesiredEdgeLengthFunc.apply(midPoint);
			sumOfqDesiredEdgeLength += (desiredEdgeLength * desiredEdgeLength);
			sumOfqLengths += (line.length() * line.length());
		}
		return Math.sqrt(sumOfqLengths/sumOfqDesiredEdgeLength);
	}


	private Set<MeshPoint> generatePoints() {
		Set<MeshPoint> gridPoints = generateGridPoints();
		//Set<MeshPoint> fixPoints = generateFixPoints();

		// point density function 1 / (desiredLen^2)
		Function<IPoint, Double> pointDensityFunc = vertex -> 1 / (relativeDesiredEdgeLengthFunc.apply(vertex) * relativeDesiredEdgeLengthFunc.apply(vertex));

		// max point density
		double max = gridPoints.stream()
				.mapToDouble(vertex -> pointDensityFunc.apply(vertex))
				.max()
				.getAsDouble();

		/*
		 * Reject points with a certain probability which depends on the relative desired edge length.
		 * The probability for the rejection increases for large desired edge length!
		 */
		Set<MeshPoint> generatedPoints = gridPoints.stream()
				.filter(vertex -> Math.random() < pointDensityFunc.apply(vertex) / max)
				.collect(Collectors.toSet());
		//generatedPoints.addAll(fixPoints);

		return generatedPoints;
	}

	/**
	 * Generates the starting points of the algorithm.
	 * @param
	 * @return
	 */
	private Set<MeshPoint> generateGridPoints() {
		int elementsInCol = (int) Math.ceil((regionBoundingBox.getX() + regionBoundingBox.getWidth())/ initialEdgeLen + 1);
		int elementsInRow = (int) Math.ceil((regionBoundingBox.getY() + regionBoundingBox.getHeight())/ (initialEdgeLen * Math.sqrt(3)/2));
		double startX = regionBoundingBox.getX();
		double startY = regionBoundingBox.getY();
		Set<MeshPoint> generatedPoints = new HashSet<>(elementsInRow * elementsInCol);
		double sqrt3 = Math.sqrt(3);

		for(int j = 0; j < elementsInRow; j++) {
			for(int i = 0; i < elementsInCol; i++) {
				MeshPoint point;
				if( j != 0 && j%2 != 0) {
					point = new MeshPoint(startX+i* initialEdgeLen + initialEdgeLen /2, startY+j* initialEdgeLen *sqrt3/2, false);
				} else {
					point = new MeshPoint(startX+i* initialEdgeLen, startY+j* initialEdgeLen *sqrt3/2, false);
				}

				//p -> fd.apply(p) < geps
				if(distanceFunc.apply(point) < geps) {
					generatedPoints.add(point);
				}
			}
		}
		return generatedPoints;
	}

	/*
	Fügt Fixpunkte hinzu. Dabei werden von allen Objekten die Ecken hinzugefügt bzw. anhand von SEGMENTDIVISION
	 */
	private Set<MeshPoint> generateFixPoints()
	{
		List<VShape> allShapes = new ArrayList<>(obstacles.size() + 1);
		allShapes.addAll(obstacles);
		//allShapes.add(regionBoundingBox);

		Set<MeshPoint> fixPoints = new HashSet<>();

		for(VShape obstacle : allShapes)
		{
			PathIterator path = obstacle.getPathIterator(null);
			double[] tempCoords = new double[6];
			double[] coordinates = new double[6];
			path.currentSegment(tempCoords);

			while (!path.isDone()) {
				path.next();
				path.currentSegment(coordinates);
				if (coordinates[0] == tempCoords[0] && coordinates[1] == tempCoords[1]) {
					break;
				}
				List<MeshPoint> points = divLine(coordinates[0], coordinates[1], tempCoords[0], tempCoords[1], Parameters.SEGMENTDIVISION);
				fixPoints.addAll(points);
				path.currentSegment(tempCoords);
			}
		}

		return fixPoints;
	}


	/*
	Unterteilt eine Linie eines Objekts in Fixpunkte
	 */
	private List<MeshPoint> divLine(double x1, double y1, double x2, double y2, int segments) {
		ConstantLineIterator lineIterator = new ConstantLineIterator(new VLine(x1, y1, x2, y2), initialEdgeLen);
		List<MeshPoint> points = new ArrayList<>();
		while (lineIterator.hasNext()) {
			IPoint iPoint = lineIterator.next();
			points.add(new MeshPoint(iPoint.getX(), iPoint.getY(), true));
		}

		return points;
	}

	public Collection<MeshPoint> getPoints() {
		return points;
	}

	private VTriangle tripleToTriangle(final Triple<MeshPoint, MeshPoint, MeshPoint> triple) {
		return new VTriangle(triple.getLeft().toVPoint(), triple.getMiddle().toVPoint(), triple.getRight().toVPoint());
	}

	public Collection<VTriangle> getTriangles() {
		if(bowyerWatson == null) {
			return new ArrayList<>();
		}
		return bowyerWatson.getTriangles();
	}

	public IncrementalTriangulation<MeshPoint, PHalfEdge<MeshPoint>, PFace<MeshPoint>> getTriangulation(){
		return bowyerWatson;
	}

}