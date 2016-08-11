package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesDensityVoronoiProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.VoronoiDiagram;

import com.google.gson.annotations.Expose;

/**
 * 
 * Computes the density over defined area for each time step.
 * 
 *
 */
public class AreaVoronoiProcessor extends AbstractProcessor {

	@Expose
	private Table table;

	@Expose
	private int lastStep;

	@Expose
	private Map<Integer, Double> areaMap;

	@Expose
	private Map<Integer, VPoint> faceMap;

	private AttributesDensityVoronoiProcessor attributes;

	private PedestrianVelocityProcessor pedestrianVelocityProcessor;

	public AreaVoronoiProcessor() {
		this(new AttributesDensityVoronoiProcessor(), new PedestrianVelocityProcessor());
	}

	public AreaVoronoiProcessor(final AttributesDensityVoronoiProcessor attributes,
			final PedestrianVelocityProcessor pedestrianVelocityProcessor) {
		this.areaMap = new HashMap<>();
		this.faceMap = new HashMap<>();
		this.attributes = attributes;
		this.pedestrianVelocityProcessor = pedestrianVelocityProcessor;
		this.pedestrianVelocityProcessor.addColumnName("speed");

		table = getTable();
		table.clear("step", "time", getDensityType(), "speed", "flow");
	}

	@Override
	public Table preLoop(SimulationState state) {
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Table table = getTable();
		table.clear();
		table.addRow();
		Row row = new Row();
		row.setEntry("step", state.getStep());
		row.setEntry("time", state.getSimTimeInSec());

		double density = getDensity(state);
		row.setEntry(getDensityType(), density);

		double speed = getSpeed(state);
		row.setEntry("speed", speed);

		row.setEntry("flow", density * speed);

		table.addColumnEntries(row);
		return table;
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	private double getDensity(final SimulationState state) {
		if (state.getStep() != lastStep) {
			cumputeAreas(state);
			lastStep = state.getStep();
		}

		VRectangle measurementArea = attributes.getMeasurementArea();

		double area = 0.0;
		double pedCount = 0.0;

		for (Integer site : faceMap.keySet()) {

			if (measurementArea.contains(faceMap.get(site))) {
				area += areaMap.get(site);
				pedCount++;
			}
		}

		if (pedCount == 0) {
			return 0;
		} else {
			return pedCount / area;
		}
	}

	private double getSpeed(final SimulationState state) {

		Table pedVelocityTable = pedestrianVelocityProcessor.postUpdate(state);
		VRectangle measurementArea = attributes.getMeasurementArea();

		ListIterator<Row> rowIteratorVelocity = pedVelocityTable.listMapIterator();

		double pedCount = 0;
		double sumSpeeds = 0;

		while (rowIteratorVelocity.hasNext()) {
			Row velocityRow = rowIteratorVelocity.next();
			VPoint position = new VPoint((Double) velocityRow.getEntry("x"), (Double) velocityRow.getEntry("y"));

			if (measurementArea.contains(position)) {
				double speed = (Double) velocityRow.getEntry("speed");
				sumSpeeds += speed;
				pedCount++;
			}
		}

		return sumSpeeds / pedCount;
	}

	private void cumputeAreas(final SimulationState state) {
		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(attributes.getVoronoiArea());

		// convert pedestrians to positions
		List<VPoint> pedestrianPositions = Agent.getPositions(state.getTopography().getElements(Agent.class));
		voronoiDiagram.computeVoronoiDiagram(pedestrianPositions);

		// compute everything
		List<org.vadere.util.voronoi.Face> faces = voronoiDiagram.getFaces();

		areaMap.clear();
		faceMap.clear();
		// TODO [priority=medium] [task=optimization] use TreeMap and a good comparator to speed this up!
		if (faces != null) {
			for (org.vadere.util.voronoi.Face face : faces) {
				areaMap.put(face.getId(), face.computeArea());
				faceMap.put(face.getId(), face.getSite());
			}
		}
	}

	@Override
	public AreaVoronoiProcessor clone() {
		return new AreaVoronoiProcessor(attributes, pedestrianVelocityProcessor.clone());
	}

	public String getDensityType() {
		return "voronoiAreaDensity";
	}

	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			AreaVoronoiProcessor tmp = (AreaVoronoiProcessor) obj;
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
