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

    public AirFlow(String scenarioPath, String airflowHash, double xmin, double ymin, double xmax, double ymax, ArrayList<AttributesInOutLet> outlets) {
        this.scenarioPath = scenarioPath;
        this.airflowHash = airflowHash;
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
        this.outlets = outlets;
    }

    public void setX_velocity(double[][] x_velocity) {
        this.x_velocity = x_velocity;
    }

    public void setY_velocity(double[][] y_velocity) {
        this.y_velocity = y_velocity;
        //applyOutletFlowCorrection();
    }

    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;
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

    private void applyOutletFlowCorrection() {
        double dist = 0.5;
        // TODO set inletVelocity
        double inletVelocity = 0.3;
        for (AttributesInOutLet outlet : outlets) {
            double dirX = 0;
            double dirY = 0;
            double outletAreaXStart = 0;
            double outletAreaXEnd = 0;
            double outletAreaYStart = 0;
            double outletAreaYEnd = 0;
            switch (outlet.getSide().toLowerCase()) {
                case "top":
                    dirY = 1.0;
                    outletAreaXStart = outlet.getStart() - dist;
                    outletAreaXEnd = outlet.getStart() + outlet.getWidth() + dist;
                    outletAreaYStart = ymax - dist;
                    outletAreaYEnd = ymax;
                    break;
                case "bottom":
                    dirY = -1.0;
                    outletAreaXStart = outlet.getStart() - dist;
                    outletAreaXEnd = outlet.getStart() + outlet.getWidth() + dist;
                    outletAreaYStart = ymin;
                    outletAreaYEnd = ymin + dist;
                    break;
                case "left":
                    dirX = -1.0;
                    outletAreaXStart = xmin;
                    outletAreaXEnd = xmin + dist;
                    outletAreaYStart = outlet.getStart() - dist;
                    outletAreaYEnd = outlet.getStart() + outlet.getWidth() + dist;
                    break;
                case "right":
                    dirX = 1.0;
                    outletAreaXStart = xmax - dist;
                    outletAreaXEnd = xmax;
                    outletAreaYStart = outlet.getStart() - dist;
                    outletAreaYEnd = outlet.getStart() + outlet.getWidth() + dist;
                    break;
            }
            for (int y_idx = 0; y_idx < y_velocity.length; y_idx++) {
                for (int x_idx = 0; x_idx < x_velocity[0].length; x_idx++) {
                    double current_x = xmin + x_idx * gridSize + gridSize / 2.0;
                    double current_y = ymin + y_idx * gridSize + gridSize / 2.0;

                    if ((current_x >= outletAreaXStart) && (current_x <= outletAreaXEnd) &&
                            (current_y >= outletAreaYStart) && (current_y >= outletAreaYEnd)) {
                        this.x_velocity[y_idx][x_idx] = dirX * inletVelocity; //this.x_velocity[y_idx][x_idx] + dirX * inletVelocity;
                        this.y_velocity[y_idx][x_idx] = dirY * inletVelocity; //this.y_velocity[y_idx][x_idx] + dirY * inletVelocity;
                    }
                }
            }
        }
    }



    public double[] getFlowDirection(double simTime, double x, double y) {
        // Return velocity components at the nearest grid point

        if (x_velocity == null) {
            return new double[]{0, 0};
        }

        if (offPeriod > 0 && (simTime % (onPeriod + offPeriod) < offPeriod)) {
            return new double[]{0, 0};
        }

        // Check bounds
        // TODO inletVelocity and gridSIZE for threshold
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
                            && (y <= (ymax + gridSize))) {
                        return new double[]{0, 1000};
                    }
                    break;
                case "bottom":
                    if ((x >= (outlet.getStart() - dist)) && (x <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (y >= (ymin - gridSize))) {
                        return new double[]{0, -1000};
                    }
                    break;
                case "left":
                    if ((y >= (outlet.getStart() - dist)) && (y <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (x >= (xmin - gridSize))) {
                        return new double[]{0, -1000};
                    }
                    break;
                case "right":
                    if ((y >= (outlet.getStart() - dist)) && (y <= (outlet.getStart() + outlet.getWidth() + dist))
                            && (x <= (xmax + gridSize))) {
                        return new double[]{0, 1000};
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