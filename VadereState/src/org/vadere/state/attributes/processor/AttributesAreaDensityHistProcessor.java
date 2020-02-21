package org.vadere.state.attributes.processor;

/**
 * @author Daniel Lehmberg
 */

//TODO: later on, maybe do as AttributesAreaProcessor (having a measurement area!=

public class AttributesAreaDensityHistProcessor extends AttributesProcessor {

    private int nrBins;
    private String direction;

    public int getNrBins() {
        return nrBins;
    }

    public String getDirection() {
        return direction;
    }

    public void setNrBins(int nrBins) {
        checkSealed();
        this.nrBins = nrBins;
    }

    public void setDirection(String direction) {
        checkSealed();
        this.direction = direction;
    }
}
