package org.vadere.util.importSumo.processors.inverseSpace;

import javax.annotation.Nullable;

public class SumoInvertSettings {
    private final double cellSize;
    private final Integer maxCellsToCombinePerAxis;

    @Nullable
    private final Double minResultPolygonDiameter;
    @Nullable
    private final Double minPolygonSize;

    public SumoInvertSettings(double cellSize) {
        this(cellSize, null);
    }

    public SumoInvertSettings(double cellSize, @Nullable Integer maxCellsToCombinePerAxis) {
        this(cellSize, maxCellsToCombinePerAxis, null, null);
    }

    public SumoInvertSettings(double cellSize, @Nullable Integer maxCellsToCombinePerAxis, @Nullable Double minResultPolygonDiameter, @Nullable Double minPolygonSize) {
        this.cellSize = cellSize;
        this.maxCellsToCombinePerAxis = maxCellsToCombinePerAxis;
        this.minResultPolygonDiameter = minResultPolygonDiameter;
        this.minPolygonSize = minPolygonSize;
    }

    public double getCellSize() {
        return cellSize;
    }

    @Nullable
    public Integer getMaxCellsToCombinePerAxis() {
        return maxCellsToCombinePerAxis;
    }

    @Nullable
    public Double getMinResultPolygonDiameter() {
        return minResultPolygonDiameter;
    }

    @Nullable
    public Double getMinPolygonSize() {
        return minPolygonSize;
    }
}
