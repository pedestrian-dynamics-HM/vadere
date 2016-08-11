package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesDensityVoronoiProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.VoronoiDiagram;

import com.google.gson.annotations.Expose;

/**
 * Adds the current timeStep (step), the current time, position (x, y-coordinates) of each position
 * in the position list of this Processor and additional adds the density on these position.
 * The density is calculated by using the the jts diagram.
 * 
 * <p>
 * <b>Added column names</b>: step {@link Integer}, time {@link Double}, x {@link Double}, y
 * {@link Double}, voronoiDensity {@link Double}
 * </p>
 *
 *
 */
public class DensityVoronoiProcessor extends DensityProcessor {

	@Expose
	private Table table;

	@Expose
	private int lastStep;

	@Expose
	private Map<Integer, Double> areaMap;

	@Expose
	private Map<Integer, VPoint> siteMap;

	private AttributesDensityVoronoiProcessor attributes;

	public DensityVoronoiProcessor() {
		this(new AttributesDensityVoronoiProcessor());
	}

	public DensityVoronoiProcessor(final AttributesDensityVoronoiProcessor attributes) {
		this.areaMap = new HashMap<>();
		this.siteMap = new HashMap<>();
		this.attributes = attributes;

		table = getTable();
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	protected double getDensity(final VPoint position, final SimulationState state) {

		VRectangle measurementArea = attributes.getMeasurementArea();
		double area = Double.NaN;

		if (measurementArea.contains(position)) {
			if (state.getStep() != lastStep) {
				computeAreas(state);
				lastStep = state.getStep();
			}

			double minDistance = Double.MAX_VALUE;

			// select closest site and its corresponding face area,
			// which is the correct local voronoi density
			for (Integer site : siteMap.keySet()) {

				double tmpDistance = siteMap.get(site).distance(position);

				if (tmpDistance < minDistance) {
					area = areaMap.get(site);
					minDistance = tmpDistance;
				}
			}
		}

		return 1.0 / area;
	}

	private void computeAreas(final SimulationState state) {
		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(attributes.getVoronoiArea());

		// convert pedestrians to positions
		List<VPoint> pedestrianPositions = Agent.getPositions(state.getTopography().getElements(Agent.class));
		voronoiDiagram.computeVoronoiDiagram(pedestrianPositions);

		// compute everything
		List<org.vadere.util.voronoi.Face> faces = voronoiDiagram.getFaces();

		areaMap.clear();
		siteMap.clear();
		// TODO [priority=medium] [task=optimization] use TreeMap and a good comparator to speed this up!
		if (faces != null) {
			for (org.vadere.util.voronoi.Face face : faces) {
				areaMap.put(face.getId(), face.computeArea());
				siteMap.put(face.getId(), face.getSite());
			}
		}
	}

	@Override
	public DensityVoronoiProcessor clone() {
		return new DensityVoronoiProcessor(attributes);
	}

	public String getDensityType() {
		return "voronoiDensity";
	}

	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			DensityVoronoiProcessor tmp = (DensityVoronoiProcessor) obj;
			return tmp.attributes.equals(attributes);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + attributes.hashCode();
		return result;
	}
}
