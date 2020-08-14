package org.vadere.simulator.projects.migration;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenConstrainedDelaunayTriangulator;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.PlanarGraphGenerator;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@link GeometryCleaner} eliminates most of problematic obstacle definitions.
 * It eliminates all intersecting or co-linear lines i.e. if two obstacles intersect
 * or align (with a tolerance) they will be merged together.
 * For a user defined tolerance {@link GeometryCleaner#tol} points are identical if
 * they are closer than {@link GeometryCleaner#tol}.
 *
 * @author Benedikt Zoennchen
 */
public class GeometryCleaner {

	private final static Logger logger = Logger.getLogger(GeometryCleaner.class);
	private final VRectangle boundingBox;
	private final VPolygon bound;
	private final List<VPolygon> polygons;
	private final double tol;

	public GeometryCleaner(@NotNull final VPolygon bound, @NotNull final Collection<VPolygon> polygons, final double tol) {
		this.bound = bound;
		this.polygons = new ArrayList<>(polygons);
		this.tol = tol;
		List<VPoint> points = polygons.stream().flatMap(poly -> poly.getPath().stream()).collect(Collectors.toList());
		points.addAll(bound.getPath());
		this.boundingBox = GeometryUtils.boundRelative(points);
	}

	public GeometryCleaner(@NotNull final VRectangle bound, @NotNull final Collection<VPolygon> polygons, final double tol) {
		this(new VPolygon(bound), polygons, tol);
	}

	/**
	 * This algorithm merges points which are very close (dependent on {@link GeometryCleaner#tol}.
	 */
	private List<VPolygon> magnet() {
		List<VPolygon> clone = new ArrayList<>(polygons.size());
		IncrementalTriangulation<PVertex, PHalfEdge, PFace> dt = new IncrementalTriangulation<>(new PMesh(), boundingBox);

		for(int i = 0; i < polygons.size(); i++) {
			List<VPoint> points = polygons.get(i).getPoints();
			List<IPoint> clonePoints = new ArrayList<>(points.size());
			for(int j = 0; j < points.size(); j++) {
				IPoint point = points.get(j);
				PHalfEdge edge = dt.insert(point);
				IPoint insertedPoint = dt.getMesh().getPoint(edge);

				PVertex v = dt.getMesh().getVertex(edge);
				PVertex umin = null;
				double len = Double.MAX_VALUE;
				for(PVertex u : dt.getMesh().getAdjacentVertexIt(v)) {
					if(len > u.distance(v)) {
						umin = u;
						len = u.distance(v);
					}
				}

				if(!insertedPoint.equals(point)) {
					point = insertedPoint;
				}

				if(v.distanceSq(umin) < tol * tol) {
					point = new VPoint(umin.getX(), umin.getY());
				}

				clonePoints.add(point);
			}
			clone.add(GeometryUtils.toPolygon(clonePoints));
		}

		return clone;
	}

	/**
	 * Removes all intersecting and co-linear lines by splitting them into multiple line or
	 * by removing identical lines.
	 *
	 * @param polygons  polygons of the topography (obstacle shapes)
	 * @param bound     the bounding of the topography
	 *
	 * @return a list of non-intersecting lines
	 */
	private Collection<VLine> removeIntersectingLines(@NotNull final List<VPolygon> polygons, @NotNull final VPolygon bound) {
		List<VLine> lines = polygons.stream().flatMap(p -> p.getLinePath().stream()).collect(Collectors.toList());
		for(VLine line : bound.getLinePath()) {
			lines.add(line);
		}

		logger.debug("resolve line intersection of " + lines.size() + " lines.");
		PlanarGraphGenerator planarGraphGenerator = new PlanarGraphGenerator(lines, 0.01);
		Collection<VLine> allLines = planarGraphGenerator.generate();
		return allLines;
	}

	/**
	 * Computes all merged {@link VPolygon}s and the segment-bounding {@link VPolygon} based on a list of non-intersecting lines
	 * and the list of (non-merged) {@link VPolygon}. Note that this method does not support polygons with holes.
	 *
	 * @param lines     non-intersecting lines
	 * @param polygons  original (intersecting) polygons
	 *
	 * @return the segment-bounding polygon and a list of merged polygons
	 */
	private Pair<VPolygon, List<VPolygon>> mergePolygons(@NotNull final Collection<VLine> lines, @NotNull final List<VPolygon> polygons) {

		// 1. compute the contrained Delaunay triangulation (all non-intersecting lines are constrained)
		IncrementalTriangulation<PVertex, PHalfEdge, PFace> dt = new IncrementalTriangulation<>(new PMesh(), boundingBox);
		GenConstrainedDelaunayTriangulator<PVertex, PHalfEdge, PFace> cdt = new GenConstrainedDelaunayTriangulator<>(dt, lines, false);
		cdt.generate(false);
		var triangulation = cdt.getTriangulation();

		// 2. definition of a distance function which defines points inside an obstacle (or outside the topography bound)
		IDistanceFunction distanceFunction = IDistanceFunction.create(bound, polygons);

		// 3. compute the segment-bounding simple polygon by using the distance function
		Predicate<PFace> removePredicate = face -> distanceFunction.apply(triangulation.getMesh().toMidpoint(face)) > 0;
		triangulation.shrinkBorder(removePredicate, true);
		VPolygon boundingPolygon = GeometryUtils.toPolygon(triangulation.getMesh().getPoints(triangulation.getMesh().getBorder()));

		// 4. compute all holes. Each hole will be the union of intersecting polygons. A hole is generated by removing
		List<PFace> faces = triangulation.getMesh().getFaces();
		for(PFace face : faces) {
			if(!triangulation.getMesh().isBorder(face) && !triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distanceFunction.apply(triangulation.getMesh().toMidpoint(f)) > 0, true);
			}
		}

		// 5. convert all holes (merged polygons) back to VPolygon(s)
		List<VPolygon> result = new ArrayList<>();
		for (PFace hole : triangulation.getMesh().getHoles()) {
			VPolygon poly = GeometryUtils.toPolygon(triangulation.getMesh().streamVertices(hole).map(v -> triangulation.getMesh().toPoint(v)).collect(Collectors.toList()));
			result.add(poly);
		}

		//String str = cdt.getMesh().toPythonTriangulation(null);
		//System.out.println(str);
		//System.out.println(TexGraphGenerator.toTikz(triangulation.getMesh(),1.0f, true));

		return Pair.of(boundingPolygon, result);
	}

	/**
	 * Cleans the geometry of the topography by:
	 * 1. merging very close points
	 * 2. merging very close or intersecting polygons
	 * 2.1 elimination of intersecting lines
	 * 2.2 merging of intersecting polygons
	 *
	 * @return the segment-bounding polygon and a list of merged polygons
	 */
	public Pair<VPolygon, List<VPolygon>> clean() {
		logger.debug("start magnet algorithm");
		List<VPolygon> magnetResult = magnet();
		logger.debug("start planar graph generation");
		Collection<VLine> lines = removeIntersectingLines(magnetResult, bound);
		logger.debug("start polygon merging");
		Pair<VPolygon, List<VPolygon>> mergedPolygons = mergePolygons(lines, magnetResult);
		return mergedPolygons;
	}

	public VPolygon subtract(@NotNull final VPolygon subject, @NotNull final List<VPolygon> subtractors) {
		List<VPolygon> polygonList = new ArrayList<>();
		polygonList.add(subject);
		polygonList.addAll(subtractors);
		WeilerAtherton weilerAthertonAlg = new WeilerAtherton(polygonList);
		return weilerAthertonAlg.subtraction().get();
	}

	/**
	 * This method cuts out all polygons {@link GeometryCleaner#polygons} which partly intersects the {@link GeometryCleaner#bound}.
	 * Note that all polygons which are contained in the measurement will not be cut out and will be returned instead.
	 *
	 * @return the cut result and a list of {@link VPolygon} of polygons which are contained in the measurement area (i.e. holes).
	 */
	public Pair<VPolygon, List<VPolygon>> cutObstacles() {
		List<VPolygon> polygonList = new ArrayList<>();
		polygonList.add(bound);

		WeilerAtherton weilerAthertonAlg = new WeilerAtherton(polygons.stream().filter(poly -> bound.intersects(poly)).collect(Collectors.toList()));
		List<VPolygon> mergedPolygons = weilerAthertonAlg.cup();
		List<VPolygon> holes = mergedPolygons.stream().filter(poly -> bound.containsShape(poly)).collect(Collectors.toList());
		mergedPolygons.removeIf(poly -> bound.containsShape(poly));

		polygonList.addAll(mergedPolygons);
		weilerAthertonAlg = new WeilerAtherton(polygonList);
		return Pair.of(weilerAthertonAlg.subtraction().get(), holes);
	}

}
