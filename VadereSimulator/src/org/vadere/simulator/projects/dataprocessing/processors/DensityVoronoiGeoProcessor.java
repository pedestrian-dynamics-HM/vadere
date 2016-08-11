package org.vadere.simulator.projects.dataprocessing.processors;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesDensityVoronoiProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.jts.VoronoiFactory;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

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
public class DensityVoronoiGeoProcessor extends DensityProcessor {

	@Expose
	private Table table;

	@Expose
	private int lastStep;

	@Expose
	private Map<Integer, Double> areaMap;

	@Expose
	private Map<Integer, VPoint> faceMap;

	@Expose
	private List<Coordinate> pedestrianCoordinates;

	@Expose
	private Geometry voronoiGeometry;

	@Expose
	private Polygon voronoiArea;

	@Expose
	private GeometryFactory geometryFactory;

	private AttributesDensityVoronoiProcessor attributes;

	public DensityVoronoiGeoProcessor() {
		this(new AttributesDensityVoronoiProcessor());
	}

	public DensityVoronoiGeoProcessor(final AttributesDensityVoronoiProcessor attributes) {
		this.areaMap = new HashMap<>();
		this.faceMap = new HashMap<>();
		this.attributes = attributes;
		this.pedestrianCoordinates = new ArrayList<>();
		this.geometryFactory = new GeometryFactory();

		table = getTable();

		VRectangle voronoiArea = attributes.getVoronoiArea();

		LinearRing voronoiAreaShell = geometryFactory.createLinearRing(new CoordinateArraySequence(new Coordinate[] {
				new Coordinate(voronoiArea.getX(), voronoiArea.getY()),
				new Coordinate(voronoiArea.getX(), voronoiArea.getY() + voronoiArea.getHeight()),
				new Coordinate(voronoiArea.getX() + voronoiArea.getWidth(),
						voronoiArea.getY() + voronoiArea.getHeight()),
				new Coordinate(voronoiArea.getX() + voronoiArea.getWidth(), voronoiArea.getY()),
				new Coordinate(voronoiArea.getX(), voronoiArea.getY())}));

		this.voronoiArea = geometryFactory.createPolygon(voronoiAreaShell, new LinearRing[0]);
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	protected double getDensity(final VPoint position, final SimulationState state) {

		VRectangle measurementArea = attributes.getMeasurementArea();
		double area = Double.NaN;

		if (state.getStep() != lastStep) {
			computeAreas(state);
			lastStep = state.getStep();
		}

		if (measurementArea.contains(position) && voronoiGeometry != null) {

			for (int i = 0; i < voronoiGeometry.getNumGeometries(); i++) {
				Geometry face = voronoiGeometry.getGeometryN(i);

				if (geometryFactory.createPoint(new Coordinate(position.x, position.y)).within(face)) {
					area = face.getArea();
				}
			}
		}

		return 1.0 / area;
	}

	private void computeAreas(final SimulationState state) {
		pedestrianCoordinates.clear();

		if (!state.getTopography().getElements(Agent.class).isEmpty()) {

			for (Agent pedestrian : state.getTopography().getElements(Agent.class)) {
				if (attributes.getVoronoiArea().contains(pedestrian.getPosition())) {
					pedestrianCoordinates
							.add(new Coordinate(pedestrian.getPosition().getX(), pedestrian.getPosition().getY()));
				}
			}

			VoronoiFactory voronoiFactory = new VoronoiFactory();
			Rectangle2D.Double bound = state.getTopography().getBounds();
			Envelope envelope = new Envelope(
					attributes.getVoronoiArea().getX(),
					attributes.getVoronoiArea().getX() + attributes.getVoronoiArea().getWidth(),
					attributes.getVoronoiArea().getY(),
					attributes.getVoronoiArea().getY() + attributes.getVoronoiArea().getHeight());
			voronoiGeometry = voronoiFactory.createVoronoiDiagram(pedestrianCoordinates, envelope);
			if (voronoiGeometry.intersects(voronoiArea)) {
				voronoiGeometry = voronoiGeometry.intersection(voronoiArea);
			}

		} else {
			voronoiGeometry = null;
		}
	}

	@Override
	public DensityVoronoiGeoProcessor clone() {
		return new DensityVoronoiGeoProcessor(attributes);
	}

	public String getDensityType() {
		return "voronoiGeoDensity";
	}

	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			DensityVoronoiGeoProcessor tmp = (DensityVoronoiGeoProcessor) obj;
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
