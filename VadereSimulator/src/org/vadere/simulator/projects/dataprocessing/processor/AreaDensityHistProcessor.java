package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaDensityHistProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.DynamicElementContainer;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Lehmberg
 */

@DataProcessorClass(label = "AreaDensityHistProcessor")
public class AreaDensityHistProcessor extends DataProcessor<TimestepKey, ArrayList<Integer>>  {

    static class IntegerArrayListCSV extends ArrayList<Integer>{
        @Override
        public String toString() {
            String row = "";
            for(Integer i : this){}
            return row;
        }
    }

    public AreaDensityHistProcessor() {
        super();

        setAttributes(new AttributesAreaDensityHistProcessor());
    }

    private AttributesAreaDensityHistProcessor getCastedAttributes(){
        return (AttributesAreaDensityHistProcessor) this.getAttributes();
    }

    private ArrayList<Integer> computeHistogram(double[] data, double min, double max) {
        int nBins = this.getCastedAttributes().getNrBins();

        // pre-allocate:
        ArrayList<Integer> result = new ArrayList<>(nBins);

        // set all values to zero
        for(int i = 0; i < nBins; ++i){
            result.add(0);
        }

        final double binSize = (max - min) / nBins;

        for (double d : data) {
            int bin = (int) ((d - min) / binSize);
            if (bin < 0) { /* this data is smaller than min */ }
            else if (bin >= nBins) { /* this data point is bigger than max */ }
            else {
                result.set(bin, result.get(bin) + 1);
            }
        }
        return result;
    }


    @Override
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

        ArrayList<Integer> binValues = this.computeHistogram(directionPosition, 0, state.getTopography().getBounds().width);
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
