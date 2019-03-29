package org.vadere.simulator.projects.dataprocessing.flags;


/**
 * Return all id's referencing a {@link org.vadere.state.scenario.MeasurementArea}. These
 * are used to check if the specified {@link org.vadere.state.scenario.MeasurementArea}
 * exist in the simulation.
 */
public interface UsesMeasurementArea extends ProcessorFlag {

    int[] getReferencedMeasurementAreaId();

}
