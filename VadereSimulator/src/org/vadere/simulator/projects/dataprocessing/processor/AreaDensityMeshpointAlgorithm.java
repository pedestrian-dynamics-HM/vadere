package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;

/**
 * @author Daniel Lehmberg
 *
 * This algorithm computes the density for each point in an unstructured mesh grid. The mesh can be over any
 * measurement area (i.e. allows any shape).
 */
public class AreaDensityMeshpointAlgorithm extends AreaDensityAlgorithm {
    private VShape measurementArea;

    public AreaDensityMeshpointAlgorithm(final @NotNull MeasurementArea measurementArea, boolean dummyArgMesh) {
        super("areaMeshpoint");
        this.measurementArea = measurementArea.getShape();
        //TODO set mesh attribute
    }

    @Override
    public double getDensity(final SimulationState state) {

        // TODO: there are different ways:
        //  1) Consider **all** agents  that are in the state
        //  2) Consider only agents that are **close to** from the points
        //  3) Consider only agents that are **in** the measurement area

        // Option 2) is implemented initially.

        // TODO: think about: how should an obstacle be treated? It also imposes a "density feeling"

        Collection<Pedestrian> consideredAgents = findNearbyPedestrians(state);

        // TODO: compute the density using the nearby agents
        return computeDensity(consideredAgents);
    }

    private Collection<Pedestrian> findNearbyPedestrians(final SimulationState state){
        // TODO: find subset of pedestrians that are inside, or close to the points defined by the mesh
        return null;

    }

    private double computeDensity(Collection<Pedestrian> agents){
        //TODO: 1) loop over each point in the mesh
        //TODO: 2) compute the density at this point

        // TODO: NOTE: this is not correct at the moment, what I want is a Collection<double> (for each point), this
        //  is just for the sake to have a dummy.
        return 0;
    }


}
