package org.vadere.state.scenario;

import java.util.ArrayList;
import java.util.List;

public class AirFlow {

    private final String scenarioPath;
    private final String scenarioHash;

    private double gridSize;
    private final double border;

    private double[][] x_velocity;
    private double[][] y_velocity;

    private double onPeriod;
    private double offPeriod;

    private List<Integer> blockingObstaclesIDs = new ArrayList<Integer>();

    public AirFlow(String scenarioPath, String scenarioHash, double border) {
        this.scenarioPath = scenarioPath;
        this.scenarioHash = scenarioHash;
        this.border = border;
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

    public String getScenarioPath() {
        return scenarioPath;
    }

    public String getScenarioHash() {
        return scenarioHash;
    }

    public double[] getFlowDirection(double simTime, double x, double y) {
        // Return velocity components at the nearest grid point

        if (x_velocity == null) {
            return new double[]{0, 0};
        }

        if (offPeriod > 0 && (simTime % (onPeriod + offPeriod) < offPeriod)) {
            return new double[]{0, 0};
        }

        int x_idx = (int) Math.round((x - border) / gridSize);
        int y_idx = (int) Math.round((y - border) / gridSize);

        // Clamp indices to valid grid range, keeping one cell buffer from edges, because edges are always zero
        x_idx = Math.max(1, Math.min(x_idx, x_velocity[0].length - 2));
        y_idx = Math.max(1, Math.min(y_idx, y_velocity.length - 2));

        return new double[]{
            x_velocity[y_idx][x_idx],
            y_velocity[y_idx][x_idx]
        };
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
}