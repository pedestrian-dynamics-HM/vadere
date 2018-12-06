package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.WeilerAtherton;
import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.HalfEdge;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    public IntegralVoronoiAlgorithm(@NotNull final VRectangle measurementArea, @NotNull final VRectangle voronoiArea) {
        super("areaVoronoi");

        this.measurementArea = measurementArea;
        this.measurementAreaPolygon = new VPolygon(measurementArea);
        this.voronoiArea = voronoiArea;
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

				velocity += (computeIntersection2(face.toPolygon()).getArea() * ped.getVelocity().getLength());
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
