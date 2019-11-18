package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.HalfEdge;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class SumVoronoiAlgorithm extends AreaDensityAlgorithm implements IAreaVelocityAlgorithm {
	private VRectangle measurementArea;
	private VPolygon measurementAreaPolygon;
	private VRectangle voronoiArea;
	private final Function<TimestepPedestrianIdKey, Double> agentVelocityFunc;

	public SumVoronoiAlgorithm(@NotNull final Function<TimestepPedestrianIdKey, Double> agentVelocityFunc, @NotNull final MeasurementArea measurementArea, @NotNull final MeasurementArea voronoiArea) {
		super("areaVoronoi");

		this.measurementArea = measurementArea.asVRectangle();
		this.measurementAreaPolygon = new VPolygon(measurementArea.getShape());
		this.voronoiArea = voronoiArea.asVRectangle();
		this.agentVelocityFunc = agentVelocityFunc;
	}

	@Override
	public double getDensity(final SimulationState state) {
		List<Face> faces = generateFaces(state);

		double area = 0.0;
		int N = 0;
		for (Face face : faces) {
			if (measurementArea.contains(face.getSite())) {
				VPolygon cell = face.toPolygon();
				N++;
				area += cell.getArea();
			}
		}

		return area > 0 ?  N / area : 0;
	}

	private List<Face> generateFaces(@NotNull final SimulationState state) {
		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(this.voronoiArea);

		// convert pedestrians to positions
		List<VPoint> pedestrianPositions = Agent.getPositions(state.getTopography().getElements(Agent.class));
		voronoiDiagram.computeVoronoiDiagram(pedestrianPositions);

		// compute everything
		List<Face> faces = voronoiDiagram.getFaces();
		return faces == null ? Collections.emptyList() : faces;
	}

	@Override
	public double getVelocity(SimulationState state) {
		List<Face> faces = generateFaces(state);

		Map<Integer, Double> areaMap = new TreeMap<>();
		Map<Integer, Face> faceMap = new TreeMap<>();

		double velocity = 0.0;
		double area = 0.0;
		for (Face face : faces) {

			if (measurementArea.contains(face.getSite())) {
				VPoint center = face.getSite();
				Agent ped = state.getTopography().getSpatialMap(Agent.class).getObjects(center, 0.2)
						.stream()
						.filter(agent -> center.distance(agent.getPosition()) < 0.01)
						.findAny().get();

				double faceArea = face.toPolygon().getArea();
				area += faceArea;
				TimestepPedestrianIdKey key = new TimestepPedestrianIdKey(state.getStep(), ped.getId());
				velocity += (faceArea * agentVelocityFunc.apply(key));
			}
		}

		return velocity / area;
	}

	private VPolygon toPolygon(@NotNull final Face face) {
		List<VPoint> points = new ArrayList<>();
		HalfEdge start = face.getOuterComponent();
		HalfEdge next = start;

		do {
			next = next.getNext();
			points.add(next.getOrigin());
		} while (!start.equals(next));

		return GeometryUtils.toPolygon(points);
	}
}
