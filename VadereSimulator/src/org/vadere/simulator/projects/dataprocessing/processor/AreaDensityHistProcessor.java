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

        public IntegerArrayListCSV(int initialCapactiy){
            super(initialCapactiy);
        }

        @Override //Overwrites the toString to Vadere-CSV style
        public String toString() {
            StringBuilder csvString = new StringBuilder();

            final String csvSeparator = " ";

            for(Integer i : this){
                if(csvString.length() != 0){   // Insert for the previous number
                    csvString.append(csvSeparator);
                }
                csvString.append(i);
            }
            return csvString.toString();
        }
    }

    public AreaDensityHistProcessor() {
        super();
        setAttributes(new AttributesAreaDensityHistProcessor());
    }

    private AttributesAreaDensityHistProcessor getCastedAttributes(){
        return (AttributesAreaDensityHistProcessor) this.getAttributes();
    }

    private IntegerArrayListCSV computeHistogram(double[] data, double min, double max) {
        int nBins = this.getCastedAttributes().getNrBins();

        // pre-allocate:
        IntegerArrayListCSV result = new IntegerArrayListCSV(nBins);

        // set all values to zero initially
        for(int i = 0; i < nBins; ++i){
            result.add(0);
        }

        final double binSize = (max - min) / nBins;

        for (double d : data) {

            int bin = (int) ((d - min) / binSize);

            if(bin >= 0 && bin < nBins){
                // if bin < 0      --> data is smaller than min
                // if bin >= nBins --> data is greater than max
                result.set(bin, result.get(bin) + 1);
            }
        }
        return result;
    }

    @Override
    public void preLoop(final SimulationState state) {

        int nrBins = this.getCastedAttributes().getNrBins();
        String direction = this.getCastedAttributes().getDirection();

        if(nrBins < 0){
            throw new IllegalArgumentException("nr bins must be positive");
        }

        if( ! (direction.equals("x") || direction.equals("y"))){
            throw new IllegalArgumentException("parameter 'direction' must be either 'x' or 'y'");
        }

        String prefix = direction + "_bin";

        // Headers can only be be set according to the user setting nrBins
        String[] headers = new String[nrBins];

        for(int i = 0; i < nrBins; ++i){
            headers[i] = prefix + i;
        }
        this.setHeaders(headers);
        this.getData().clear();
    }

    @Override
    protected void doUpdate(final SimulationState state) {

        DynamicElementContainer<Pedestrian> pedestrians = state.getTopography().getPedestrianDynamicElements();

        // TODO: only insert elements that are in a measurement area (if required)
        Collection<Pedestrian> pedCollection = pedestrians.getElements();

        String direction = getCastedAttributes().getDirection();

        double[] directionPosition;
        ArrayList<Integer> binValues;
        double lowerBound, heigherBound;
        if(direction.equals("x")){
            directionPosition = pedCollection.stream().mapToDouble(ped -> ped.getPosition().x).toArray();
            lowerBound = state.getTopography().getBounds().x;
            heigherBound = lowerBound + state.getTopography().getBounds().width;
        }else{ // direction.equals("y") -- checked in pre-loop
            directionPosition = pedCollection.stream().mapToDouble(ped -> ped.getPosition().y).toArray();
            lowerBound = state.getTopography().getBounds().y;
            heigherBound = lowerBound + state.getTopography().getBounds().height;
        }
        binValues = this.computeHistogram(directionPosition, lowerBound, heigherBound);

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
