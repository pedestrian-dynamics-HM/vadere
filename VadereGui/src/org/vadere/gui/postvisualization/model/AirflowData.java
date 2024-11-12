package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.ColumnNames;
import org.vadere.state.scenario.AirFlow;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;


public class AirflowData {

    public static final String TABLE_NAME = "airflow";

    public final int idXCol;
    public final int idYCol;
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
    public AirflowData(@NotNull final Table dataFrame) {
        ColumnNames columnNames = ColumnNames.getInstance();

        idXCol = columnNames.getAirflowIdXCol(dataFrame);
        idYCol = columnNames.getAirflowIdYCol(dataFrame);
        xPosStartCol = columnNames.getAirflowXPosStartCol(dataFrame);
        xPosEndCol = columnNames.getAirflowXPosEndCol(dataFrame);
        yPosStartCol = columnNames.getAirflowYPosStartCol(dataFrame);
        yPosEndCol = columnNames.getAirflowYPosEndCol(dataFrame);
        xVelocityCol = columnNames.getAirflowXVelocityCol(dataFrame);
        yVelocityCol = columnNames.getAirflowYVelocityCol(dataFrame);

        this.data = dataFrame;
    }

    public void setAirflow(Table airflowData) {

        if (airflowData.isEmpty()) {
            airFlow = null;
            return;
        }

        Row lastRow = airflowData.row(airflowData.rowCount() - 1);
        double[][] xVelocity = new double[lastRow.getInt(idXCol) + 1][lastRow.getInt(idYCol) + 1];
        double[][] yVelocity = new double[lastRow.getInt(idXCol) + 1][lastRow.getInt(idYCol) + 1];

        for (Row row : airflowData) {
            xVelocity[row.getInt(idXCol)][row.getInt(idYCol)] = row.getDouble(xVelocityCol);
            yVelocity[row.getInt(idXCol)][row.getInt(idYCol)] = row.getDouble(yVelocityCol);
        }

        airFlow = new AirFlow("", "", 0);
        airFlow.setX_velocity(xVelocity);
        airFlow.setY_velocity(yVelocity);
        try {
            airFlow.setGridSize(lastRow.getDouble(xPosEndCol) - lastRow.getDouble(xPosStartCol));
        } catch (Exception e) {
            airFlow.setGridSize(lastRow.getInt(xPosEndCol) - lastRow.getInt(xPosStartCol));
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

    public AirFlow getAirflow() {
        return airFlow;
    }
}
