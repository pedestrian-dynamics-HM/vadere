package org.vadere.meshing.mesh.triangulation.improver.distmesh;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.meshing.ConstantLineIterator;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MLine;
import org.vadere.util.geometry.shapes.MPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The original DistMesh algorithm
 *
 * @see <a href="http://persson.berkeley.edu/distmesh/">DistMesh</a>
 */
public class Distmesh {
	private static final Logger log = Logger.getLogger(Distmesh.class);
	private Set<EikMeshPoint> points = new HashSet<>();
	private Set<MLine<EikMeshPoint>> lines = new HashSet<>();
	private List<VTriangle> triangles = new ArrayList<>();
	private List<Triple<MPoint, MPoint, MPoint>> triangleT = new ArrayList<>();
	private final IDistanceFunction distanceFunc;
	private final IEdgeLengthFunction relativeDesiredEdgeLengthFunc;
	private VRectangle regionBoundingBox;
	private Collection<? extends VShape> obstacles;
	private int steps;
	private DelaunayTriangulationBuilder builder;
	private GeometryFactory fact;
	private int nTriangulations;
	private Set<EikMeshPoint> fixPoints;
	private Random random = new Random(0);

	// Parameters
	private double initialEdgeLen;
	private double geps;
	private double deps;
	private boolean firstStep = true;
	private boolean runParallel = false;
	private double maxMovementLen = 0;
	private boolean hasChanged;


	public Distmesh(final IDistanceFunction distanceFunc,
	                final IEdgeLengthFunction edgeLengthFunc,
	                final double initialEdgeLen,
	                final VRectangle bound,
	                final Collection<? extends VShape> obstacleShapes) {

		this.distanceFunc = distanceFunc;
		this.relativeDesiredEdgeLengthFunc = edgeLengthFunc;
		this.regionBoundingBox = bound;
		this.obstacles = obstacleShapes;
		this.initialEdgeLen = initialEdgeLen;

		this.geps = .001 * initialEdgeLen;
		this.deps = 1.4901e-13 * initialEdgeLen;
		this.steps = 0;
		this.nTriangulations = 0;

		this.fact = null;
		this.builder = null;
		this.hasChanged = true;

		// initialize points.
		this.points = generatePoints();
	}

	public void execute() {
		while(firstStep || maxMovementLen > Parameters.DPTOL && !hasMaximalSteps()) {
			//log.debug("quality: " + getQuality());
			step();
			log.info("quality:" + getQuality());
			log.info("min-quality: " + getMinQuality());
		}
	}

	public boolean isFinished() {
		//log.debug("quality: " + test);
		return maxMovementLen > Parameters.DPTOL && !hasMaximalSteps();
	}

	public boolean hasMaximalSteps() {
		return steps >= Parameters.MAX_NUMBER_OF_STEPS;
	}

	public void reTriangulate() {
		reTriangulate(false);
	}

	public void reTriangulate(boolean force) {
		if(force || firstStep || maxMovementLen / initialEdgeLen > Parameters.TOL) {
			firstStep = false;
			nTriangulations++;

			//log.debug("triangulation started");
			Collection<Coordinate> coords = points.stream().map(p -> new Coordinate(p.getX(), p.getY())).collect(Collectors.toList());
			fact = new GeometryFactory();
			builder = new DelaunayTriangulationBuilder();

			synchronized (this) {
				builder.setSites(coords);
				com.vividsolutions.jts.geom.GeometryCollection multiTris = (com.vividsolutions.jts.geom.GeometryCollection) builder.getTriangles(fact);

				HashMap<EikMeshPoint, EikMeshPoint> meshPoints = new HashMap();
				triangles = new ArrayList<>();
				triangleT = new ArrayList<>();
				lines = new HashSet<>();
				for (int i = 0; i < multiTris.getNumGeometries(); i++) {
					Polygon tri = (Polygon) multiTris.getGeometryN(i);
					Coordinate[] coordinates = tri.getCoordinates();

					EikMeshPoint p1 = new EikMeshPoint(coordinates[0].x, coordinates[0].y, false);
					EikMeshPoint p2 = new EikMeshPoint(coordinates[1].x, coordinates[1].y, false);
					EikMeshPoint p3 = new EikMeshPoint(coordinates[2].x, coordinates[2].y, false);
					p1.setFixPoint(fixPoints.contains(p1));
					p2.setFixPoint(fixPoints.contains(p2));
					p3.setFixPoint(fixPoints.contains(p3));

					VTriangle triangle = new VTriangle(new VPoint(p1.getX(), p1.getY()), new VPoint(p2.getX(), p2.getY()), new VPoint(p3.getX(), p3.getY()));
					if(distanceFunc.apply(triangle.midPoint()) < -geps) {
						if(meshPoints.containsKey(p1)) {
							p1 = meshPoints.get(p1);
						}
						else {
							meshPoints.put(p1, p1);
						}

						if(meshPoints.containsKey(p2)) {
							p2 = meshPoints.get(p2);
						}
						else {
							meshPoints.put(p2, p2);
						}

						if(meshPoints.containsKey(p3)) {
							p3 = meshPoints.get(p3);
						}
						else {
							meshPoints.put(p3, p3);
						}

						triangles.add(triangle);
						triangleT.add(Triple.of(p1, p2, p3));
						lines.add(new MLine<>(p1, p2));
						lines.add(new MLine<>(p2, p3));
						lines.add(new MLine<>(p3, p1));
					}
				}
				points = meshPoints.keySet();
			}

			//log.debug("triangulation finished");
			//log.debug("#edges: " + lines.size());
			//log.debug("#points: " + points.size());
		}
	}

	public int getNumberOfIllegalTriangles() {
		int count = 0;
		for(Triple<MPoint, MPoint, MPoint> triangle1 : triangleT) {
			MPoint p1 = triangle1.getLeft();
			MPoint p2 = triangle1.getMiddle();
			MPoint p3 = triangle1.getRight();

			for(MPoint point : points) {
				if(!point.equals(p1) && !point.equals(p2) && !point.equals(p3)) {
					if(GeometryUtils.isInsideCircle(p1, p2, p3, point.getX(), point.getY(), GeometryUtils.DOUBLE_EPS)) {
						count++;
						break;
					}
				}
			}
		}
		return count;
	}

	public void improve() {
		step();
	}

	/*
	Stellt den Verlauf der Iterationen dar. Innerhalb der while(true) passiert eine Iteration des Algorithmus
	 */
	public void step()
	{
		hasChanged = true;
		steps++;
		reTriangulate();

		/*
		 double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
        double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

		double lenDiff = Math.max(desiredLen - len, 0);
        p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len)));
		 */

		// compute the forces / velocities for each line
		double scalingFactor = computeScalingFactor(lines);
		//log.debug("scaling factor: " + scalingFactor);

		Stream<MLine<EikMeshPoint>> lineStream = runParallel ? lines.parallelStream() : lines.stream();
		lineStream.forEach(line -> {
			double len = line.length();
			double desiredLen = relativeDesiredEdgeLengthFunc.apply(line.midPoint()) * Parameters.FSCALE * scalingFactor;
			//double desiredLen = 10.4 * 1.2;
			//len = 10;
			double lenDiff = Math.max(desiredLen - len, 0);
			//double lenDiff = desiredLen - len;
			//double force = desiredLen - len;
			IPoint forceDirection = new VPoint(line.getP1()).subtract(new VPoint(line.getP2())).norm();
			VPoint directedForce = new VPoint(forceDirection.scalarMultiply(lenDiff));

			//VPoint directedForce = new VPoint((lenDiff / len) * (line.getX1() - line.getX2()), (lenDiff / len) * (line.getY1() - line.getY2()));
			//VPoint directedForce = new VPoint(3, 3);
			line.setVelocity(directedForce);
			//line.setVelocity(line.getVelocity().add(new VPoint(Math.random(), Math.random())));
			//System.out.println("directed: " + directedForce);
		});


		lineStream = runParallel ? lines.parallelStream() : lines.stream();

		lineStream.forEach(line -> {
			if(!line.p1.isFixPoint()) {
				synchronized (line.p1) {
					line.p1.increaseVelocity(line.getVelocity());
				}
			}

			if(!line.p2.isFixPoint()) {
				synchronized (line.p2) {
					line.p2.decreaseVelocity(line.getVelocity());
				}
			}
		});

		// do the euler step, we change the mutable point, this may destroy there unquieness.
		maxMovementLen = 0.0;

		Stream<EikMeshPoint> pointStream = runParallel ? points.parallelStream() : points.stream();

		pointStream.forEach(point -> {
			if(!point.isFixPoint()) {
				//log.info("vel:" + point.getVelocity().distanceToOrigin());
				VPoint pOld = new VPoint(point.getX(), point.getY());
				IPoint movement = point.getVelocity().scalarMultiply(Parameters.DELTAT);

				//System.out.println("movement="+movementLen);

				point.add(movement);
				point.setVelocity(new VPoint(0, 0));

				// back projection
				double distance = distanceFunc.apply(point);
				if(distance > 0) {
					VPoint dx = new VPoint(point.getX(), point.getY()).add(new VPoint(deps,0));
					VPoint dy = new VPoint(point.getX(), point.getY()).add(new VPoint(0,deps));
					double dGradPX = (distanceFunc.apply(dx) - distance) / deps;
					double dGradPY = (distanceFunc.apply(dy) - distance) / deps;
					point.subtract(new VPoint(dGradPX * distance,  dGradPY * distance));
					// extension
					//double dGrad2 = dGradPX * dGradPX + dGradPY * dGradPY;
					//point.subtract(new VPoint(initialEdgeLen * distance * dGradPX / dGrad2, initialEdgeLen * distance * dGradPY / dGrad2));
				}
				else {

				}

				double movementLen = pOld.subtract(point).distanceToOrigin();
				synchronized (this) {
					if(maxMovementLen < movementLen) {
						maxMovementLen = movementLen;
					}
				}
			}
		});

		//log.debug("maxmove: " + maxMovementLen / initialEdgeLen);
		//log.debug("scale: " + scalingFactor);

		firstStep = false;
	}
	/*
	d=feval(fd,p,varargin{:}); ix=d>0;                 % Find points outside (d>0)
  dgradx=(feval(fd,[p(ix,1)+deps,p(ix,2)],varargin{:})-d(ix))/deps; % Numerical
  dgrady=(feval(fd,[p(ix,1),p(ix,2)+deps],varargin{:})-d(ix))/deps; %    gradient
  dgrad2=dgradx.^2+dgrady.^2;
  p(ix,:)=p(ix,:)-[d(ix).*dgradx./dgrad2,d(ix).*dgrady./dgrad2];    % Project
	 */

	/*
	Berechnet die durchschnittliche Qualität aller erzeugten Dreiecke
	 */
	public double getQuality()
	{
		Collection<VTriangle> triangles = getCurrentTriangles();
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

	public int getNumberOfReTriangulations() {
		return nTriangulations;
	}

	public double getQuality(final VTriangle triangle) {
		VLine[] lines = triangle.getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}

		if(Double.isNaN(part)) {
			throw new IllegalArgumentException(triangle + " is not a feasible triangle!");
		}

		return part;
	}

	public double getMinQuality()
	{
		double minQuality = 100000;
		Collection<VTriangle> triangles = getCurrentTriangles();
		for(VTriangle triangle : triangles) {
			VLine[] lines = triangle.getLines();
			double a = lines[0].length();
			double b = lines[1].length();
			double c = lines[2].length();
			double part = 0.0;
			if(a != 0.0 && b != 0.0 && c != 0.0) {
				part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
			}
			if(part < minQuality) {
				minQuality = part;
			}

			if(Double.isNaN(part)) {
				throw new IllegalArgumentException(triangle + " is not a feasible triangle!");
			}
		}

		return minQuality;
	}

	/**
	 * Formula 2.6
	 * @param lines
	 * @return
	 */
	private double computeScalingFactor(final Collection<MLine<EikMeshPoint>> lines) {
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


	private Set<EikMeshPoint> generatePoints() {
		Set<EikMeshPoint> gridPoints = generateGridPoints();
		//Set<MeshPoint> fixPoints = generateFixPoints();

		// point density function 1 / (desiredLen^2)
		Function<IPoint, Double> pointDensityFunc = vertex -> 1 / (relativeDesiredEdgeLengthFunc.apply(vertex) * relativeDesiredEdgeLengthFunc.apply(vertex));

		// bound point density
		double max = gridPoints.stream()
				.mapToDouble(vertex -> pointDensityFunc.apply(vertex))
				.max()
				.getAsDouble();

		/*
		 * Reject points with a certain probability which depends on the relative desired edge length.
		 * The probability for the rejection increases for large desired edge length!
		 */
		Set<EikMeshPoint> generatedPoints = gridPoints.stream()
				.filter(vertex -> random.nextDouble() < pointDensityFunc.apply(vertex) / max)
				.collect(Collectors.toSet());

		fixPoints = getFixPoints();

		// remove points with equal coords but not fix
		generatedPoints.removeAll(fixPoints);

		// add fix points
		generatedPoints.addAll(fixPoints);

		return generatedPoints;
	}

	/**
	 * Generates the starting points of the algorithm.
	 * @param
	 * @return
	 */
	private Set<EikMeshPoint> generateGridPoints() {
		int elementsInCol = (int) Math.ceil((regionBoundingBox.getWidth()) / initialEdgeLen + 1);
		int elementsInRow = (int) Math.ceil((regionBoundingBox.getHeight()) / (initialEdgeLen * Math.sqrt(3)/2));
		double startX = regionBoundingBox.getX();
		double startY = regionBoundingBox.getY();
		Set<EikMeshPoint> generatedPoints = new HashSet<>(elementsInRow * elementsInCol);
		double sqrt3 = Math.sqrt(3);

		for(int j = 0; j < elementsInRow; j++) {
			for(int i = 0; i < elementsInCol; i++) {
				EikMeshPoint point;
				if( j != 0 && j%2 != 0) {
					point = new EikMeshPoint(startX+i* initialEdgeLen + initialEdgeLen /2, startY+j* initialEdgeLen *sqrt3/2, false);
				} else {
					point = new EikMeshPoint(startX+i* initialEdgeLen, startY+j* initialEdgeLen *sqrt3/2, false);
				}

				//p -> fd.apply(p) < geps
				if(distanceFunc.apply(point) < -geps) {
					generatedPoints.add(point);
				}
			}
		}
		return generatedPoints;
	}

	private Set<EikMeshPoint> getFixPoints() {
		Set<EikMeshPoint> pointSet = new HashSet<>();
		for(VShape shape : obstacles) {
			pointSet.addAll(shape.getPath().stream().map(p ->  new EikMeshPoint(p.getX(), p.getY(), true)).collect(Collectors.toList()));
		}
		return pointSet;
	}


	/*
	Unterteilt eine Linie eines Objekts in Fixpunkte
	 */
	private List<EikMeshPoint> divLine(double x1, double y1, double x2, double y2, int segments) {
		ConstantLineIterator lineIterator = new ConstantLineIterator(new VLine(x1, y1, x2, y2), initialEdgeLen);
		List<EikMeshPoint> points = new ArrayList<>();
		while (lineIterator.hasNext()) {
			IPoint iPoint = lineIterator.next();
			points.add(new EikMeshPoint(iPoint.getX(), iPoint.getY(), true));
		}

		return points;
	}

	public Collection<EikMeshPoint> getPoints() {
		return points;
	}

	private VTriangle tripleToTriangle(final Triple<EikMeshPoint, EikMeshPoint, EikMeshPoint> triple) {
		return new VTriangle(triple.getLeft().toVPoint(), triple.getMiddle().toVPoint(), triple.getRight().toVPoint());
	}

	private Collection<VTriangle> getCurrentTriangles() {
		return triangles;
	}

	public Collection<VTriangle> getTriangles() {
		if(builder == null) {
			return new ArrayList();
		}
		synchronized (this) {
			return triangleT.stream().map(triple -> new VTriangle(new VPoint(triple.getLeft()), new VPoint(triple.getMiddle()), new VPoint(triple.getRight()))).collect(Collectors.toList());
		}
	}
}