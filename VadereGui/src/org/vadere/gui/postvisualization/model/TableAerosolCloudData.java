package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.ColumnNames;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.util.geometry.shapes.VPoint;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The {@link TableAerosolCloudData}
 */
public class TableAerosolCloudData {

    private final int sampleStepWidth;

    private final List<Integer> sampleSteps;

    private final Table cloudDataFrame;

    private Table currentSlice;

    public static final String TABLE_NAME = "aerosolCloudData";

    // columns, TODO: this is hard coded!
    public final int timeStepCol;
    public final int cloudIdCol;
    public final int pathogenLoadCol;
    public final int radiusCol;
    public final int centerXCol;
    public final int centerYCol;

    /**
     * Default constructor.
     *
     * @param dataFrame the whole table containing all data points of all aerosol clouds for all times
     */
    public TableAerosolCloudData(@NotNull final Table dataFrame) {
        // get all ids of all columns
        // 1. mandatory columns:
        ColumnNames columnNames = ColumnNames.getInstance();
        timeStepCol = columnNames.getTimeStepCol(dataFrame);
        cloudIdCol = columnNames.getAerosolCloudIdCol(dataFrame);
        pathogenLoadCol = columnNames.getAerosolCloudPathogenLoadCol(dataFrame);
        radiusCol = columnNames.getAerosolCloudRadiusCol(dataFrame);
        centerXCol = columnNames.getAerosolCloudCenterXCol(dataFrame);
        centerYCol = columnNames.getAerosolCloudCenterYCol(dataFrame);

        this.cloudDataFrame = dataFrame;
        this.currentSlice = cloudDataFrame;
        if (!isEmpty()) {
            this.sampleStepWidth = getTimeStepSampleStepWidth();
            this.sampleSteps = getSampleSteps();
        } else {
            this.sampleStepWidth = 0;
            this.sampleSteps = List.of(0);
        }
    }

    public boolean isEmpty() {
        return cloudDataFrame.isEmpty();
    }

    protected AerosolCloud toAerosolCloud(@NotNull final Row row) {
        int cloudId = row.getInt(cloudIdCol);
        double currentPathogenLoad = row.getDouble(pathogenLoadCol);
        double radius = row.getDouble(radiusCol);
        double centerX = row.getDouble(centerXCol);
        double centerY = row.getDouble(centerYCol);
        VPoint center = new VPoint(centerX, centerY);

        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(cloudId, radius, center, currentPathogenLoad));

        return aerosolCloud;
    }

    public Collection<AerosolCloud> toAerosolCloudCollection(int timeStep) {
        Collection<AerosolCloud> clouds = new ArrayList<>();

        setCurrentSlice(timeStep);

        for (Row row : currentSlice) {
            AerosolCloud cloud = toAerosolCloud(row);
            clouds.add(cloud);
        }
        return clouds;
    }

    public void setCurrentSlice(int timeStep) {
        IntColumn timeSteps = getTimeStep(cloudDataFrame);
        List<Integer> sampleInterval = getCurrentSampleInterval(timeStep);

        if (!sampleInterval.isEmpty()) {
            currentSlice = cloudDataFrame.where(timeSteps.isGreaterThanOrEqualTo(sampleInterval.get(0))
                    .and(timeSteps.isLessThan(sampleInterval.get(1))));
        } else {
            currentSlice = cloudDataFrame.emptyCopy();
        }
    }

    private List<Integer> getCurrentSampleInterval(int timeStep) {
        int intervalStart = sampleSteps.stream().filter(i -> (i.intValue() >= timeStep && i.intValue() <= timeStep + sampleStepWidth)).findFirst().orElse(-1);
        if (intervalStart != -1) {
            return  Arrays.asList(intervalStart, intervalStart + sampleStepWidth);
        } else {
            return Collections.emptyList();
        }
    }

    private int getTimeStepSampleStepWidth() {
        int minStepWidth = 1;
        // Calculate delta time step for all time steps
        IntColumn timeSteps = getTimeStep(cloudDataFrame);
        Set<Integer> timeStepsDiff = IntStream.range(1, timeSteps.asList().size() - 2).mapToObj(index -> timeSteps.getInt(index) - timeSteps.getInt(index - 1)).collect(Collectors.toSet());
        // exclude value 0
        timeStepsDiff = timeStepsDiff.stream().filter(i -> i > 0).collect(Collectors.toSet());
        return Math.max(minStepWidth, Collections.min(timeStepsDiff));
    }

    private List<Integer> getSampleSteps() {
        int firstSampledStep = (int) getTimeStep(cloudDataFrame).min();
        int lastSampledStep = (int) getTimeStep(cloudDataFrame).max();
        List<Integer> sampleSteps = new ArrayList<>();
        for (int i = firstSampledStep; i <= lastSampledStep; i = i + sampleStepWidth) {
            sampleSteps.add(i);
        }
        return sampleSteps;
    }

    private IntColumn getTimeStep(@NotNull final Table table) {
        return table.intColumn(timeStepCol);
    }

    public IntColumn getTimeStep() {
        return getTimeStep(currentSlice);
    }
}
