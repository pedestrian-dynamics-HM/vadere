package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.util.delaunay.BowyerWatson;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.geom.PathIterator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PSDistmesh {
	private Set<MPoint> points = new HashSet<>();
	private Set<MLine> lines = new HashSet<>();
	private BowyerWatson<MPoint> bowyerWatson;
	private Collection<Triple<MPoint, MPoint, MPoint>> triangulation;
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
			relativeDesiredEdgeLengthFunc = IEdgeLengthFunction.create(regionBoundingBox, obstacles, distanceFunc);
		}

		this.points = new HashSet<>(generatePoints());
		System.out.println("unrejected number of points: " + points.size());
	}

	private void init(final VRectangle regionBoundingBox,
	             final Collection<? extends VShape> obstacles,
	             final double initialEdgeLen) {
		this.regionBoundingBox = regionBoundingBox;
		this.initialEdgeLen = initialEdgeLen;
		this.geps = .001 * initialEdgeLen;
		this.deps = 1.4901e-8 * initialEdgeLen;
		this.triangulation = new ArrayList<>();
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
		triangulation = triangulation.stream()
				.filter(triple -> obstacles.stream().noneMatch(obstacle -> tripleToTriangle(triple).intersect(obstacle))).collect(Collectors.toSet());
	}

	/*
	Stellt den Verlauf der Iterationen dar. Innerhalb der while(true) passiert eine Iteration des Algorithmus
	 */
	public void step()
	{
		steps++;
		if(firstStep || maxMovementLen / initialEdgeLen > Parameters.TOL) {
			maxMovementLen = 0;
			bowyerWatson = new BowyerWatson<>(points, (x, y) -> new MPoint(x, y, -1), (a, b, c) -> new VTriangle(a, b, c));
			bowyerWatson.init();

			System.out.println("triangulation started");
			bowyerWatson.execude();
			triangulation = bowyerWatson.getTrianglePoints();
			System.out.println("triangulation finished");

			// compte the line ste
			lines = new HashSet<>();
			//points = new ArrayList<>();
			for(Triple<MPoint, MPoint, MPoint> triple : triangulation) {
				VTriangle triangle = new VTriangle(triple.getLeft().toVPoint(), triple.getMiddle().toVPoint(), triple.getRight().toVPoint());

				MLine line1 = new MLine(triple.getLeft(), triple.getMiddle());
				MLine line2 = new MLine(triple.getMiddle(), triple.getRight());
				MLine line3 = new MLine(triple.getRight(), triple.getLeft());
				//fd.apply(p.midPoint()) < -geps
				if(distanceFunc.apply(triangle.midPoint()) < -geps) {
					lines.add(line1);
					lines.add(line2);
					lines.add(line3);
					points.add(line1.getP1());
					points.add(line2.getP1());
					points.add(line3.getP1());
				}
			//	else {
			//		System.out.println("mid:" + triangle.midPoint() + " func: " + distanceFunc.apply(triangle.midPoint()));
			//	}
			}

			System.out.println("number of edges: " + lines.size());
			System.out.println("number of points: " + points.size());

		}

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
		for(MLine line : lines) {
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
		Set<MPoint> newPoints = new HashSet();
		for(MPoint point : points) {
			if(!point.isFixPoint()) {
				IPoint movement = point.getVelocity().scalarMultiply(Parameters.DELTAT);
				double movementLen = point.getVelocity().distanceToOrigin();

				//System.out.println("movement="+movementLen);

				if(maxMovementLen < movementLen) {
					maxMovementLen = movementLen;
				}

				if(distanceFunc.apply(point.toVPoint().add(movement)) <= 0) {
					point.add(movement);
				}

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
		if(triangulation.isEmpty()) {
			return 0.0;
		}
		else {
			double aveSum = 0;
			for(Triple<MPoint, MPoint, MPoint> triple : triangulation) {
				VLine[] line = tripleToTriangle(triple).getLines();
				double a = line[0].length();
				double b = line[1].length();
				double c = line[2].length();
				double part = 0.0;
				if(a != 0.0 && b != 0.0 && c != 0.0) {
					part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
				}
				aveSum += part;
				if(Double.isNaN(part) || Double.isNaN(aveSum)) {
					throw new IllegalArgumentException(tripleToTriangle(triple) + " is not a feasible triangle!");
				}
			}

			return aveSum / triangulation.size();
		}
	}


	/**
	 * Formula 2.6
	 * @param lines
	 * @return
	 */
	private double computeScalingFactor(final Collection<MLine> lines) {
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


	private Set<MPoint> generatePoints() {
		List<MPoint> gridPoints = generateGridPoints();
		List<MPoint> fixPoints = generateFixPoints();

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
		Set<MPoint> generatedPoints = gridPoints.stream()
				.filter(vertex -> Math.random() < pointDensityFunc.apply(vertex) / max)
				.collect(Collectors.toSet());
		generatedPoints.addAll(fixPoints);

		return generatedPoints;
	}

	/**
	 * Generates the starting points of the algorithm.
	 * @param
	 * @return
	 */
	private List<MPoint> generateGridPoints() {
		int elementsInCol = (int) Math.ceil((regionBoundingBox.getX() + regionBoundingBox.getWidth())/ initialEdgeLen + 1);
		int elementsInRow = (int) Math.ceil((regionBoundingBox.getY() + regionBoundingBox.getHeight())/ (initialEdgeLen * Math.sqrt(3)/2));
		double startX = regionBoundingBox.getX();
		double startY = regionBoundingBox.getY();
		List<MPoint> generatedPoints = new ArrayList(elementsInRow * elementsInCol);
		double sqrt3 = Math.sqrt(3);

		for(int j = 0; j < elementsInRow; j++) {
			for(int i = 0; i < elementsInCol; i++) {
				MPoint point;
				if( j != 0 && j%2 != 0) {
					point = new MPoint(startX+i* initialEdgeLen + initialEdgeLen /2, startY+j* initialEdgeLen *sqrt3/2, false);
				} else {
					point = new MPoint(startX+i* initialEdgeLen, startY+j* initialEdgeLen *sqrt3/2, false);
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
	private List<MPoint> generateFixPoints()
	{
		List<VShape> allShapes = new ArrayList<>(obstacles.size() + 1);
		allShapes.addAll(obstacles);
		allShapes.add(regionBoundingBox);

		List<MPoint> fixPoints = new ArrayList<>();

		for(VShape obstacle : allShapes)
		{
			PathIterator path = obstacle.getPathIterator(null);
			double[] tempCoords = new double[6];
			double[] coordinates = new double[6];
			path.currentSegment(tempCoords);

			while (!path.isDone()) {
				path.next();

				path.currentSegment(coordinates);
				MPoint[] points = divLine(coordinates[0], coordinates[1], tempCoords[0], tempCoords[1], Parameters.SEGMENTDIVISION);
				if (coordinates[0] == tempCoords[0] && coordinates[1] == tempCoords[1]) {
					break;
				}
				fixPoints.addAll(Arrays.asList(points).subList(1, points.length));
				path.currentSegment(tempCoords);
			}
		}

		return fixPoints;
	}


	/*
	Unterteilt eine Linie eines Objekts in Fixpunkte
	 */
	private MPoint[] divLine(double x1, double y1, double x2, double y2, int segments)
	{
		MPoint[] points = new MPoint[segments+1];
		double dX = (x1-x2)/segments;
		double dY = (y1-y2)/segments;

		for(int i = 1; i < points.length; i++) {
			points[i] = new MPoint(x2 + i * dX, y2 + i * dY, true);
		}
		return points;
	}

	public Collection<MPoint> getPoints() {
		return points;
	}

	private double doDDiff(double d1, double d2)
	{
		return Math.max(d1, -d2);
	}

	private double doDUnion(double d1, double d2) {
		return Math.min(d1, d2);
	}

	private VTriangle tripleToTriangle(final Triple<MPoint, MPoint, MPoint> triple) {
		return new VTriangle(triple.getLeft().toVPoint(), triple.getMiddle().toVPoint(), triple.getRight().toVPoint());
	}

	public Collection<VTriangle> getTriangles() {
		return triangulation.stream().map(triple -> tripleToTriangle(triple)).collect(Collectors.toList());
	}
}
