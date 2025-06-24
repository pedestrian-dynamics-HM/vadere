package org.vadere.state.scenario;

import org.vadere.state.attributes.models.airflow.AttributesInOutLet;

import java.util.ArrayList;
import java.util.List;

public class AirFlow {

    private final String scenarioPath;
    private String airflowHash;

    private double gridSize;
    private final double xmin;
    private final double ymin;
    private final double xmax;
    private final double ymax;

    private double[][] x_velocity;
    private double[][] y_velocity;

    private double onPeriod;
    private double offPeriod;

    private List<Integer> blockingObstaclesIDs = new ArrayList<Integer>();

    private List<AttributesInOutLet> outlets;

    public AirFlow(String scenarioPath, String airflowHash, double xmin, double ymin, double xmax, double ymax) {
        this.scenarioPath = scenarioPath;
        this.airflowHash = airflowHash;
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }

    public void setX_velocity(double[][] x_velocity) {
        this.x_velocity = x_velocity;
    }

    public void setY_velocity(double[][] y_velocity) {
        this.y_velocity = y_velocity;
    }

    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
    }

    public void setOutlets(ArrayList<AttributesInOutLet> outlets) {
        this.outlets = outlets;
    }

    public String getScenarioPath() {
        return scenarioPath;
    }

    public String getAirflowHash() {
        return airflowHash;
    }

    public void setAirflowHash(String airflowHash) {
        this.airflowHash = airflowHash;
    }

    public double[] getFlowDirection(double simTime, double x, double y) {
        // Return velocity components at the nearest grid point

        if (x_velocity == null) {
            return new double[]{0, 0};
        }

        if (offPeriod > 0 && (simTime % (onPeriod + offPeriod) < offPeriod)) {
            return new double[]{0, 0};
        }

        if (x < xmin || x > xmax || y < ymin || y > ymax) {
            return getOutOfBoundsFlowDirection(x, y);
        }

        int x_idx = (int) Math.round((x - xmin) / gridSize);
        int y_idx = (int) Math.round((y - ymin) / gridSize);

        // Clamp indices to valid grid range, keeping one cell buffer from edges, because edges are always zero
        x_idx = Math.max(1, Math.min(x_idx, x_velocity[0].length - 2));
        y_idx = Math.max(1, Math.min(y_idx, y_velocity.length - 2));

        return new double[]{
            x_velocity[y_idx][x_idx],
            y_velocity[y_idx][x_idx]
        };
    }

    private double[] getOutOfBoundsFlowDirection(double x, double y) {
        // check whether aerosol cloud is on outlet -> then move it away - otherwise it gets stuck
        double dist = gridSize/2;
        for (AttributesInOutLet outlet : outlets) {
            switch (outlet.getSide().toLowerCase()) {
                case "top":
                    if ((x >= (outlet.getStart() - dist)) && (x <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (y <= (ymax + gridSize*2))) {
                        return new double[]{0, 10000};
                    }
                    break;
                case "bottom":
                    if ((x >= (outlet.getStart() - dist)) && (x <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (y >= (ymin - gridSize*2))) {
                        return new double[]{0, -10000};
                    }
                    break;
                case "left":
                    if ((y >= (outlet.getStart() - dist)) && (y <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (x >= (xmin - gridSize*2))) {
                        return new double[]{0, -10000};
                    }
                    break;
                case "right":
                    if ((y >= (outlet.getStart() - dist)) && (y <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (x <= (xmax + gridSize*2))) {
                        return new double[]{0, 10000};
                    }
                    break;
            }
        }
        return new double[]{0, 0};
    }

    public double[][] getXVelocities() {
        return x_velocity;
    }

    public double[][] getYVelocities() {
        return y_velocity;
    }

    public double getGridSize() {
        return gridSize;
    }

    public void setPeriod(double onPeriod, double offPeriod) {
        this.onPeriod = onPeriod;
        this.offPeriod = offPeriod;
    }

    public void setBlockingObstaclesIDs(List<Integer> blockingObstaclesIDs) {
        this.blockingObstaclesIDs = blockingObstaclesIDs;
    }

    public List<Integer> getBlockingObstaclesIDs() {
        return blockingObstaclesIDs;
    }

    public double getXmin() { return xmin; }
    public double getXmax() { return xmax; }
    public double getYmin() { return ymin; }
    public double getYmax() { return ymax; }
}