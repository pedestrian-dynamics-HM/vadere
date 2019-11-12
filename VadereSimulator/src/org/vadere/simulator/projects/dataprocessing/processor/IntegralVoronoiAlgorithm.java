package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.WeilerAtherton;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Given a Simulation state this Algorithm computes the Voronoi density and Voronoi velocity
 * defined in zhang-2011 (doi:10.1088/1742-5468/2011/06/P06004) section 3.4 (Method D), equations (7, 8, 9)
 *
 * @author Benedikt Zoennchen
 *
 */
public class IntegralVoronoiAlgorithm extends AreaDensityAlgorithm implements IAreaVelocityAlgorithm {
    private VRectangle measurementArea;
    private VPolygon measurementAreaPolygon;
    private VRectangle voronoiArea;
    private final Function<TimestepPedestrianIdKey, Double> agentVelocityFunc;

    public IntegralVoronoiAlgorithm(@NotNull final Function<TimestepPedestrianIdKey, Double> agentVelocityFunc, @NotNull final MeasurementArea measurementArea, @NotNull final MeasurementArea voronoiMeasurementArea) {
        super("areaVoronoi");

        this.measurementArea = measurementArea.asVRectangle();
        this.measurementAreaPolygon = new VPolygon(measurementArea.getShape());
        this.voronoiArea = voronoiMeasurementArea.asVRectangle();
        this.agentVelocityFunc = agentVelocityFunc;
    }

    @Override
    public double getDensity(final SimulationState state) {
        List<Face> faces = generateFaces(state);

	    double area = 0.0;
        for (Face face : faces) {
            if (intersectMeasurementArea(face)) {
				VPolygon cell = face.toPolygon();

	            VPolygon capPolygon = computeIntersection2(cell);
	            area += capPolygon.getArea() / cell.getArea();
				assert capPolygon.getArea() <= cell.getArea();
            }
        }

        return area / measurementArea.getArea();
    }

    private VPolygon computeIntersection2(@NotNull final VPolygon cell) {
	    try {
		    WeilerAtherton weilerAtherton = new WeilerAtherton(Arrays.asList(cell, measurementAreaPolygon));
		    VPolygon capPolygon = weilerAtherton.cap().get();
		    return capPolygon;
	    } catch (Exception e) {
		    System.out.println(e.getMessage());
		    //VPolygon capPolygon = weilerAtherton.cap().get();
	    }
	    return null;
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

		double velocity = 0.0;
		for (Face face : faces) {

			if (intersectMeasurementArea(face)) {
				VPoint center = face.getSite();
				Agent ped = state.getTopography().getSpatialMap(Agent.class).getObjects(center, 0.2)
						.stream()
						.filter(agent -> center.distance(agent.getPosition()) < 0.01)
						.findAny().get();

				TimestepPedestrianIdKey key = new TimestepPedestrianIdKey(state.getStep(), ped.getId());
				velocity += (computeIntersection2(face.toPolygon()).getArea() * agentVelocityFunc.apply(key));
			}
		}

		return velocity / measurementArea.getArea();
	}

    private boolean intersectMeasurementArea(@NotNull final Face face) {
		return measurementArea.intersects(face.toPolygon());
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
