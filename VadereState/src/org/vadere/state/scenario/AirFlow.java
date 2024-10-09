package org.vadere.state.scenario;

public class AirFlow {

    private final String scenarioPath;
    private final String scenarioHash;

    private double gridSize;

    private double[][] x_velocity;
    private double[][] y_velocity;

    public AirFlow(String scenarioPath, String scenarioHash) {
        this.scenarioPath = scenarioPath;
        this.scenarioHash = scenarioHash;
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

    public double[] getFlowDirection(double x, double y) {
        double[] result = new double[2];

        int x_idx = (int) (x / gridSize);
        if (x_idx < 0)
            x_idx = 0;
        else if (x_idx >= x_velocity.length)
            x_idx = x_velocity.length - 1;

        int y_idx = (int) (y / gridSize);
        if (y_idx < 0)
            y_idx = 0;
        else if (y_idx >= y_velocity.length)
            y_idx = y_velocity.length - 1;

        result[0] = x_velocity[y_idx][x_idx];
        result[1] = y_velocity[y_idx][x_idx];
        return result;
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
}