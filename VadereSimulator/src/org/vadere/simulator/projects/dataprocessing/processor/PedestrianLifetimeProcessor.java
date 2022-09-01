package org.vadere.simulator.projects.dataprocessing.processor;

import org.jcodec.common.DictionaryCompressor;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;

import java.util.HashMap;

/**
 * This processor stores the time a pedestrian was living inside the scene.
 * @author Ludwig Jäck
 */
public class PedestrianLifetimeProcessor extends DataProcessor<PedestrianIdKey,Double> {
    HashMap<Integer,Double> birthtimes;
    public PedestrianLifetimeProcessor(){
        super("lifeTime");
        this.birthtimes = new HashMap<>();
    }

    @Override
    protected void doUpdate(SimulationState state) {
        var topography = state.getTopography();
        var pedestrians = topography.getPedestrianDynamicElements().getElements();
        var simTimeInSecs = state.getSimTimeInSec();

        for(var pedestrian : pedestrians){
            var pedestrianId = pedestrian.getId();
            var pedestrianIdKey = new PedestrianIdKey(pedestrianId);

            if(this.birthtimes.containsKey(pedestrian.getId())){
                var birthtime = this.birthtimes.get(pedestrianId);
                var lifetime = simTimeInSecs - birthtime;

                this.putValue(pedestrianIdKey,lifetime);
            } else {
                this.birthtimes.put(pedestrianId,simTimeInSecs);
                this.putValue(pedestrianIdKey,0.0);
            }
        }
    }
}
