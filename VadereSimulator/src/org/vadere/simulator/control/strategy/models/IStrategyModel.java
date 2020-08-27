package org.vadere.simulator.control.strategy.models;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesStrategyModel;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.LinkedList;

public interface IStrategyModel {

     /*
     * @param pedestrians The pedestrians to update
     */


    void update(double simTimeInSec, Collection<Pedestrian> pedestrians, ProcessorManager processorManager);

    void build(AttributesStrategyModel attr);

    void initialize(double simTimeInSec);
}
