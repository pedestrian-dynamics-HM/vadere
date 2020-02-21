package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.math.random.EmpiricalDistributionImpl;
import org.apache.commons.math3.stat.Frequency;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaDensityHistProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.DynamicElementContainer;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Lehmberg
 */

@DataProcessorClass(label = "AreaDensityHistProcessor")
public class AreaDensityHistProcessor extends DataProcessor<TimestepKey, List<Double>>  {

    public AreaDensityHistProcessor() {
        super();

        setAttributes(new AttributesAreaDensityHistProcessor());
    }

    private AttributesAreaDensityHistProcessor getCastedAttributes(){
        return (AttributesAreaDensityHistProcessor) this.getAttributes();
    }

    public void preLoop(final SimulationState state) {

        // TODO: make checks here!

        int nrBins = this.getCastedAttributes().getNrBins();

        // Headers can only be set according to the user setting nrBins
        String[] headers = new String[nrBins];

        for(int i = 0; i < nrBins; ++i){
            headers[i] = "bin" + i;
        }
        this.setHeaders(headers);
        this.getData().clear();
    }

    @Override
    protected void doUpdate(final SimulationState state) {

        DynamicElementContainer<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements();

        // TODO: only insert elements that are in a measurement area (if required)
        Collection<Pedestrian> pedCollection = pedestrians.getElements();

        double[] directionPosition = pedCollection.stream().mapToDouble(ped -> ped.getPosition().x).toArray();
        EmpiricalDistributionImpl histogram = new EmpiricalDistributionImpl(getCastedAttributes().getNrBins());
        histogram.load(directionPosition);

        List<Double> binValues = histogram.getBinStats().stream().map(binStat -> binStat.getSum()).collect(Collectors.toList());
        this.putValue(new TimestepKey(state.getStep()), binValues);
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityHistProcessor());
        }
        return super.getAttributes();
    }
}
