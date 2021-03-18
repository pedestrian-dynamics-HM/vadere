package org.vadere.state.health;

import org.junit.Test;

import static org.junit.Assert.*;

public class HealthStatusTest {

    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    @Test
    public void testEmitPathogen() {
        double logExpectedEmittedPathogen = 7;
        double expectedEmittedPathogen = Math.pow(10, logExpectedEmittedPathogen);
        HealthStatus healthStatus = new HealthStatus();

        healthStatus.setPathogenEmissionCapacity(7);

        double emittedPathogen = healthStatus.emitPathogen();

        assertEquals(logExpectedEmittedPathogen, healthStatus.getPathogenEmissionCapacity(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(expectedEmittedPathogen, emittedPathogen, ALLOWED_DOUBLE_TOLERANCE);
    }

}
