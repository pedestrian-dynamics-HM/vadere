package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.simulator.control.simulation.Simulation;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPositionKey;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.state.attributes.processor.AttributesVoronoiPolygonProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.VoronoiDiagram;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Manuel Hertle
 */
@DataProcessorClass()
public class VoronoiPolygonProcessor extends DataProcessor<TimestepPositionKey, VPolygon> implements UsesMeasurementArea {
	private MeasurementArea voronoiMeasurementArea;
	private List<VPolygon> obstacles;

	private static Logger logger = Logger.getLogger(VoronoiPolygonProcessor.class);

	public VoronoiPolygonProcessor() {
		super("polygon", "area");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		// This does not work currently, bcause of the mocking in the tests.
		// Collection<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements().getElements();
		List<Face> faces = this.generateFaces(state);
		Map<VPoint, VPolygon> cells = new HashMap<>();
		boolean substractionSuccess = true;

		for (Face face: faces) {
			VPolygon cell = face.toPolygon();
			VPolygon capCell = computeObstacleIntersection(cell);
			if (capCell == null) {
				logger.info("no voronoidiagramm for timestep: " + state.getStep() + "and area: " + voronoiMeasurementArea.getId());
				substractionSuccess = false;
				break;
			}
			cells.put(face.getSite(), capCell);
		}
		if (substractionSuccess) {
			for (Map.Entry<VPoint, VPolygon> entry : cells.entrySet()) {
				this.putValue(new TimestepPositionKey(state.getStep(), entry.getKey()), entry.getValue());
			}
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesVoronoiPolygonProcessor att = (AttributesVoronoiPolygonProcessor) this.getAttributes();
		obstacles = manager.getObstacles().stream().map(VPolygon::new).collect(Collectors.toList());
		voronoiMeasurementArea = manager.getMeasurementArea(att.getVoronoiMeasurementAreaId(), true);
	}

	@Override
	public String[] toStrings(TimestepPositionKey key) {
		if (this.hasValue(key)) {
			VPolygon p = this.getValue(key);
			String points = StateJsonConverter.serializeObject(p.getPoints());
			String area = StateJsonConverter.serializeObject(p.getArea());
			return new String[]{points, area};
		} else {
			return new String[]{"NA", "NA"};
		}
	}

	@Override
	public AttributesVoronoiPolygonProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesVoronoiPolygonProcessor());
		}
		return (AttributesVoronoiPolygonProcessor) super.getAttributes();
	}

	private List<Face> generateFaces(@NotNull final SimulationState state) {
		VoronoiDiagram voronoiDiagram = new VoronoiDiagram(this.voronoiMeasurementArea.asVRectangle());

		// convert pedestrians to positions
		List<VPoint> pedestrianPositions = Agent.getPositions(state.getTopography().getElements(Agent.class));
		voronoiDiagram.computeVoronoiDiagram(pedestrianPositions);

		// compute everything
		List<Face> faces = voronoiDiagram.getFaces();
		return faces == null ? Collections.emptyList() : faces;
	}

	private VPolygon computeObstacleIntersection(@NotNull final VPolygon cell) {
		LinkedList<VPolygon> intersectedObstacles = intersectObstacles(cell);

		try {
			if (!intersectedObstacles.isEmpty()) {
				intersectedObstacles.addFirst(cell);
				WeilerAtherton weilerAtherton = new WeilerAtherton(intersectedObstacles);
				Optional<VPolygon> capPolygon = weilerAtherton.subtraction();
				return capPolygon.orElse(null);
			} else {
				return cell;
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		return null;
	}

	@Override
	public int[] getReferencedMeasurementAreaId() {
		AttributesVoronoiPolygonProcessor att = (AttributesVoronoiPolygonProcessor) this.getAttributes();
		return new int[]{att.getVoronoiMeasurementAreaId()};
	}

	private LinkedList<VPolygon> intersectObstacles(VPolygon cell) {
		return obstacles.stream()
				.filter(obstacle -> obstacle.intersects(cell))
				.collect(Collectors.toCollection(LinkedList::new));
	}
}
