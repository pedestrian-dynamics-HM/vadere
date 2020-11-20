package org.vadere.state.attributes.processor;

public class AttributesAreaDensityGridCountingProcessor extends AttributesProcessor {

    double cellSize = 5.0;

    public double getCellSize() {
        return cellSize;
    }

    public void setCellSize(double cellSize) {
        checkSealed();
        this.cellSize = cellSize;
    }
}
