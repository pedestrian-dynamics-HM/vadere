package org.vadere.simulator.models.potential.timeCostFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vadere.state.attributes.models.AttributesTimeCost;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.voronoi.RectangleLimits;
import org.vadere.util.voronoi.VoronoiDiagram;

/**
 * 
 * This class is no longer in use.
 * 
 * The TimeCostDensityVoronoi measures the density by generating an
 * jts-diagram for the whole area of the scenario. This has a complexity of
 * O(N*log(N)) for each computation. We count the pedestrians and the area size
 * of all jts-regions in an specific area. D = N * (min possilbe jts
 * area) /(sum of all jts cell areas) for those jts cell who's site is
 * inside the measurement area (a square).
 * 
 * This is only working if no obstacles inside the area of the
 * jts-diagram!!!
 * 
 * 
 */
@Deprecated
public class TimeCostDensityVoronoi implements ITimeCostFunction {
	/** the jts-diagram which has to be recomputed in every step. */
	private final VoronoiDiagram voronoiDiagram;

	/** the width and height of the measurement area (a square). */
	private final double measurementAreaRadius;

	/** the cached voronoiAreas, to avoid recomputing. */
	private Map<Integer, Double> voronoiAreas;

	/** the weight of the density. */
	private double sameTargetWeight;

	private double otherTargetWeight;

	/** S_p (as supposed in Seitz paper). */
	private final double scaleFactor;

	private final Topography floor;

	/** the time cost function for the decorator-pattern. */
	private ITimeCostFunction timeCostFunction;

	public TimeCostDensityVoronoi(final ITimeCostFunction timeCostFunction,
			final AttributesTimeCost attributes, final AttributesAgent attributesPedestrian, final int floorId,
			final Topography floor) {
		this.timeCostFunction = timeCostFunction;
		this.floor = floor;
		this.sameTargetWeight = attributes
				.getPedestrianSameTargetDensityWeight();
		this.otherTargetWeight = attributes
				.getPedestrianOtherTargetDensityWeight();
		this.voronoiDiagram = new VoronoiDiagram(new RectangleLimits(0, 0,
				floor.getBounds().getWidth(), floor.getBounds().getHeight()));
		this.measurementAreaRadius = 1.5;
		this.voronoiAreas = new HashMap<>();
		this.scaleFactor = attributesPedestrian.getRadius() * 2
				* attributesPedestrian.getRadius() * 2 * Math.sqrt(3) * 0.5;
	}

	@Override
	public double costAt(final IPoint p) {
		double density = 0.0;
		int numberPedestriansInPolygon = 0;
		double pedestrianSpaceSum = 0;

		// Collection<Body> pedBodies =
		// floor.getBodies(Pedestrian.class).values();
		List<org.vadere.util.voronoi.Face> faces = null;

		// the area
		VRectangle measurementArea = new VRectangle(p.getX() - measurementAreaRadius
				/ 2, p.getY() - measurementAreaRadius / 2, measurementAreaRadius,
				measurementAreaRadius);

		// voronoiDiagram.computeVoronoiDiagram( pedBodies );
		faces = voronoiDiagram.getFaces();

		if (faces != null) {
			for (org.vadere.util.voronoi.Face face : faces) {
				if (measurementArea.contains(face.getSite())) {
					numberPedestriansInPolygon++;
					Double area = voronoiAreas.get(face.getId());
					if (area == null) {
						area = face.computeArea();
						voronoiAreas.put(face.getId(), new Double(area));
					}
					pedestrianSpaceSum += area;
				}

			}

			if (numberPedestriansInPolygon != 0) {
				density = scaleFactor * numberPedestriansInPolygon
						/ pedestrianSpaceSum;
			}
		}

		return timeCostFunction.costAt(p) + density * sameTargetWeight;
	}

	@Override
	public void update() {
		voronoiAreas.clear();
		List<VPoint> pedPositions = Agent.getPositions(floor.getElements(Agent.class));
		this.voronoiDiagram.computeVoronoiDiagram(pedPositions);
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}
}
