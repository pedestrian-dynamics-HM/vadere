package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Agent;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AreaDensityVoronoiAlgorithm extends AreaDensityAlgorithm {
    private VRectangle measurementArea;
    private VRectangle voronoiArea;

    public AreaDensityVoronoiAlgorithm(final VRectangle measurementArea, final VRectangle voronoiArea) {
        super("areaVoronoi");

        this.measurementArea = measurementArea;
        this.voronoiArea = voronoiArea;
    }

    @Override
    public double getDensity(final SimulationState state) {
        VoronoiDiagram voronoiDiagram = new VoronoiDiagram(this.voronoiArea);

        // convert pedestrians to positions
        List<VPoint> pedestrianPositions = Agent.getPositions(state.getTopography().getElements(Agent.class));
        voronoiDiagram.computeVoronoiDiagram(pedestrianPositions);

        // compute everything
        List<org.vadere.util.voronoi.Face> faces = voronoiDiagram.getFaces();

        Map<Integer, Double> areaMap = new TreeMap<>();
        Map<Integer, VPoint> faceMap = new TreeMap<>();

        if (faces != null) {
            for (org.vadere.util.voronoi.Face face : faces) {
                areaMap.put(face.getId(), face.computeArea());
                faceMap.put(face.getId(), face.getSite());
            }
        }

        double area = 0.0;
        int pedCount = 0;

        // TODO: Possible optimization (do not test all faces)
        for (Integer site : faceMap.keySet()) {
            if (this.measurementArea.contains(faceMap.get(site))) {
                area += areaMap.get(site);
                pedCount++;
            }
        }

        return pedCount > 0 ? pedCount / area : 0;
    }
}
