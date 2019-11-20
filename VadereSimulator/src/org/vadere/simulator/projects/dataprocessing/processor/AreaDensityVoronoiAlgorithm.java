package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.util.Collections;
import java.util.List;

/**
 * Given a Simulation state this Algorithm computes the Voronoi density defined in zoennchen-2013 section 3, equation 3.7.
 *
 * @author Benedikt Zoennchen
 *
 */
public class AreaDensityVoronoiAlgorithm extends AreaDensityAlgorithm {
    private VRectangle measurementArea;
    private VRectangle voronoiArea;

    public AreaDensityVoronoiAlgorithm(final MeasurementArea measurementArea, final MeasurementArea voronoiArea) {
        super("areaVoronoi");

        this.measurementArea = measurementArea.asVRectangle();
        this.voronoiArea = voronoiArea.asVRectangle();
    }

    @Override
    public double getDensity(final SimulationState state) {

        // compute everything
        List<Face> faces = generateFaces(state);

        double area = 0.0;
        int pedCount = 0;

        for (Face face : faces) {
            if (this.measurementArea.contains(face.getSite())) {
                area += face.computeArea();
                pedCount++;
            }
        }
        return pedCount > 0 ? pedCount / area : 0;
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
}
