package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.simulator.projects.io.ColumnNames;
import org.vadere.state.scenario.AirFlow;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;


public class AirFlowData {

    public static final String TABLE_NAME = "airFlow";

    public final int xIdCol;
    public final int yIdCol;
    public final int xPosStartCol;
    public final int xPosEndCol;
    public final int yPosStartCol;
    public final int yPosEndCol;
    public final int xVelocityCol;
    public final int yVelocityCol;

    private AirFlow airFlow;
    private Table data;


    /**
     * Default constructor.
     *
     * @param dataFrame the whole table containing all data points of all aerosol clouds for all times
     */
    public AirFlowData(@NotNull final Table dataFrame) {
        ColumnNames columnNames = ColumnNames.getInstance();

        xIdCol = columnNames.getAirflowIdXCol(dataFrame);
        yIdCol = columnNames.getAirflowIdYCol(dataFrame);

        xPosStartCol = columnNames.getAirflowXPosStartCol(dataFrame);
        xPosEndCol = columnNames.getAirflowXPosEndCol(dataFrame);

        yPosStartCol = columnNames.getAirflowYPosStartCol(dataFrame);
        yPosEndCol = columnNames.getAirflowYPosEndCol(dataFrame);

        xVelocityCol = columnNames.getAirflowXVelocityCol(dataFrame);
        yVelocityCol = columnNames.getAirflowYVelocityCol(dataFrame);

        this.data = dataFrame;
    }

    public void setAirflow(Table airFlowData) {
        if (airFlowData.isEmpty()) {
            airFlow = null;
            return;
        }

        Row lastRow = airFlowData.row(airFlowData.rowCount() - 1);
        double[][] xVelocity = new double[lastRow.getInt(xIdCol) + 1][lastRow.getInt(yIdCol) + 1];
        double[][] yVelocity = new double[lastRow.getInt(xIdCol) + 1][lastRow.getInt(yIdCol) + 1];

        for (Row row : airFlowData) {
            xVelocity[row.getInt(xIdCol)][row.getInt(yIdCol)] = row.getDouble(xVelocityCol);
            yVelocity[row.getInt(xIdCol)][row.getInt(yIdCol)] = row.getDouble(yVelocityCol);
        }

        airFlow = new AirFlow("", "", 0, 0, 1000, 1000);
        airFlow.setX_velocity(xVelocity);
        airFlow.setY_velocity(yVelocity);
        try {
            airFlow.setRectangularGridCellSize(lastRow.getDouble(xPosEndCol) - lastRow.getDouble(xPosStartCol));
        } catch (Exception e) {
            airFlow.setRectangularGridCellSize(lastRow.getInt(xPosEndCol) - lastRow.getInt(xPosStartCol));
        }
    }

    public void initAirflow() {
        if (airFlow == null) {
            setAirflow(data);
        }
    }

    public boolean isEmpty() {
        return airFlow == null;
    }

    @Nullable
    public AirFlow getAirflow() {
        return airFlow;
    }
}
