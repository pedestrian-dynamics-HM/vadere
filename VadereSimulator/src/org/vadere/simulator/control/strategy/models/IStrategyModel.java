package org.vadere.simulator.control.strategy.models;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.AttributesStrategyModel;
import org.vadere.state.scenario.Topography;

public interface IStrategyModel<V> {

     /*
     * @param pedestrians The pedestrians to update
     */


    void update(double simTimeInSec, Topography top, ProcessorManager processorManager);

    void build(AttributesStrategyModel attr);

    void initialize(double simTimeInSec);

    V getStrategyInfoForDataProcessor();

}
