package org.vadere.simulator.control.strategy.models;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

public interface IStrategyModel {

     /*
     * @param pedestrians The pedestrians to update
     */
    void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager);


    void initialize(double simTimeInSec);
}
