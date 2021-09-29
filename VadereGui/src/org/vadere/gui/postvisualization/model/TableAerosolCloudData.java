package org.vadere.gui.postvisualization.model;

import org.jcodec.common.DictionaryCompressor;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.ColumnNames;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.vadere.state.scenario.AerosolCloud.createTransformedAerosolCloudShape;

/**
 * The {@link TableAerosolCloudData}
 */
public class TableAerosolCloudData {

    private final int sampleStepWidth;

    private final List<Integer> sampleSteps;

    private final Table cloudDataFrame;

    private Table currentSlice;


    // columns, TODO: this is hard coded!
    public final int timeStepCol;
    public final int cloudIdCol;
    public final int pathogenLoadCol;
    public final int areaCol;
    public final int vertex1XCol;
    public final int vertex1YCol;
    public final int vertex2XCol;
    public final int vertex2YCol;

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
        areaCol = columnNames.getAerosolCloudAreaCol(dataFrame);
        vertex1XCol = columnNames.getAerosolCloudVertex1XCol(dataFrame);
        vertex1YCol = columnNames.getAerosolCloudVertex1YCol(dataFrame);
        vertex2XCol = columnNames.getAerosolCloudVertex2XCol(dataFrame);
        vertex2YCol = columnNames.getAerosolCloudVertex2YCol(dataFrame);

        this.cloudDataFrame = dataFrame;
        this.currentSlice = cloudDataFrame;
        this.sampleStepWidth = getTimeStepSampleStepWidth();
        this.sampleSteps = getSampleSteps();
    }

    protected AerosolCloud toAerosolCloud(@NotNull final Row row) {
        int cloudId = row.getInt(cloudIdCol);
        double currentPathogenLoad = row.getDouble(pathogenLoadCol);
        double area = row.getDouble(areaCol);
        double vertex1X = row.getDouble(vertex1XCol);
        double vertex1Y = row.getDouble(vertex1YCol);
        double vertex2X = row.getDouble(vertex2XCol);
        double vertex2Y = row.getDouble(vertex2YCol);
        VPoint vertex1 = new VPoint(vertex1X, vertex1Y);
        VPoint vertex2 = new VPoint(vertex2X, vertex2Y);
        VShape shape = createTransformedAerosolCloudShape(vertex1, vertex2, area);

        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(cloudId, shape, currentPathogenLoad));

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
