package org.vadere.state.scenario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class AirFlowTest {

    private AirFlow airFlow;
    private static final double BORDER = 0.0;
    private static final double GRID_SIZE = 1.0;
    private static final double DELTA = 1e-10; // for double comparisons

    @BeforeEach
    public void setUp() {
        airFlow = new AirFlow("testScenario", "testHash", BORDER, BORDER, 1000, 1000);
        airFlow.setRectangularGridCellSize(GRID_SIZE);
    }

    @Test
    public void testGetFlowDirectionWithNullVelocities() {
        // When velocities are null, should return [0,0]
        double[] result = airFlow.getFlowDirection(0, 0, 0);
        assertArrayEquals(new double[]{0, 0}, result);
    }

    @Test
    public void testGetFlowDirectionWithValidVelocities() {
        double[][] xVel = new double[3][3];
        double[][] yVel = new double[3][3];
        xVel[1][1] = 2.0;
        yVel[1][1] = 1.0;
        
        airFlow.setX_velocity(xVel);
        airFlow.setY_velocity(yVel);

        double[] result = airFlow.getFlowDirection(1.0, 1.0, 1.0);
        assertArrayEquals(new double[]{2.0, 1.0}, result, DELTA);
    }

    @Test
    public void testGetFlowDirectionWithPeriodicBehavior() {
        double[][] xVel = new double[3][3];
        double[][] yVel = new double[3][3];
        xVel[1][1] = 2.0;
        yVel[1][1] = 1.0;
        
        airFlow.setX_velocity(xVel);
        airFlow.setY_velocity(yVel);
        airFlow.setPeriod(1.0, 1.0);

        // Test during off period
        double[] resultOff = airFlow.getFlowDirection(0.5, 1.0, 1.0);
        assertArrayEquals(new double[]{0, 0}, resultOff, DELTA);

        // Test during on period
        double[] resultOn = airFlow.getFlowDirection(1.5, 1.0, 1.0);
        assertArrayEquals(new double[]{2.0, 1.0}, resultOn, DELTA);
    }

    @Test
    public void testGetFlowDirectionGridPointMapping() {
        // Create 4x4 velocity arrays with different values in each cell
        double[][] xVel = new double[4][4];
        double[][] yVel = new double[4][4];
        
        // Fill arrays with distinct values for each cell
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                xVel[i][j] = i + j * 0.5;
                yVel[i][j] = i * 0.5 + j;
            }
        }
        
        airFlow.setX_velocity(xVel);
        airFlow.setY_velocity(yVel);
        
        // Test points that should map to specific grid cells
        // Format: {x coordinate, y coordinate, expected x-velocity, expected y-velocity}
        double[][] testPoints = {
            {1.1, 1.1, xVel[1][1], yVel[1][1]},
            {0.8, 0.9, xVel[1][1], yVel[1][1]},

            {2.1, 1.2, xVel[1][2], yVel[1][2]},
            {1.8, 0.8, xVel[1][2], yVel[1][2]},

            {1.2, 2.1, xVel[2][1], yVel[2][1]},
            {0.9, 1.8, xVel[2][1], yVel[2][1]},

            {2.1, 2.1, xVel[2][2], yVel[2][2]},
            {1.8, 1.9, xVel[2][2], yVel[2][2]}
        };
        
        for (double[] point : testPoints) {
            double[] result = airFlow.getFlowDirection(0, point[0], point[1]);
            assertArrayEquals(new double[]{point[2], point[3]}, result, DELTA,
                    String.format("Point (%f, %f) mapped to incorrect velocity", point[0], point[1]));
        }
    }

    @Test
    public void testGetFlowDirectionBoundaryHandling() {
        double[][] xVel = new double[3][3];
        double[][] yVel = new double[3][3];
        xVel[1][1] = 2.0;
        yVel[1][1] = 1.0;
        
        airFlow.setX_velocity(xVel);
        airFlow.setY_velocity(yVel);

        // Test points far outside the grid - should clamp to valid indices
        double[] farPoint = airFlow.getFlowDirection(0, 100.0, 100.0);
        assertArrayEquals(new double[]{2.0, 1.0}, farPoint, DELTA);
        
        double[] negativePoint = airFlow.getFlowDirection(0, -100.0, -100.0);
        assertArrayEquals(new double[]{0.0, 0.0}, negativePoint, DELTA);

        double[] cornerPoint = airFlow.getFlowDirection(0, 0.0, 0.0);
        assertArrayEquals(new double[]{2.0, 1.0}, cornerPoint, DELTA);
    }
} 