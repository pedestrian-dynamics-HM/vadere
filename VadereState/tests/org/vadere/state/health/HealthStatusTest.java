package org.vadere.state.health;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class HealthStatusTest {

    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;
    private HealthStatus healthStatus;
    private final static double infectionStatusInterval = 60.0;
    private final static double exposedPathogenLoadThreshold = 1000;

    @Before
    public void setUp() {
        healthStatus = new HealthStatus(InfectionStatus.SUSCEPTIBLE, 0, 0,
        null, 0, true, 1e4, 5e-4,
                exposedPathogenLoadThreshold, infectionStatusInterval, infectionStatusInterval, infectionStatusInterval);
    }

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

    @Test
    public void throwIfHealthStatusExposedNotReached() {
        healthStatus.setInfectionStatus(InfectionStatus.SUSCEPTIBLE);
        healthStatus.setPathogenAbsorbedLoad(exposedPathogenLoadThreshold);

        healthStatus.updateInfectionStatus(1);
        Assert.assertEquals(InfectionStatus.EXPOSED, healthStatus.getInfectionStatus());
    }

    @Test
    public void throwIfHealthStatusInfectiousNotReached() {
        healthStatus.setInfectionStatus(InfectionStatus.EXPOSED);
        healthStatus.updateInfectionStatus(infectionStatusInterval);
        Assert.assertEquals(InfectionStatus.INFECTIOUS, healthStatus.getInfectionStatus());
    }

    @Test
    public void throwIfHealthStatusRecoveredNotReached() {
        healthStatus.setInfectionStatus(InfectionStatus.INFECTIOUS);
        healthStatus.updateInfectionStatus(infectionStatusInterval);
        Assert.assertEquals(InfectionStatus.RECOVERED, healthStatus.getInfectionStatus());
    }

    @Test
    public void throwIfHealthStatusSusceptibleNotReached() {
        healthStatus.setInfectionStatus(InfectionStatus.RECOVERED);
        healthStatus.updateInfectionStatus(infectionStatusInterval);
        Assert.assertEquals(InfectionStatus.SUSCEPTIBLE, healthStatus.getInfectionStatus());
    }

    @Test
    public void throwIfHealthStatusIsBreatheIn() {
        double periodLength = 4;
        double simTimeInSec = healthStatus.getRespiratoryTimeOffset() + periodLength / 2.0 + 1e-3;
        healthStatus.updateRespiratoryCycle(simTimeInSec, periodLength);

        assertFalse(healthStatus.isBreathingIn());
    }

    @Test
    public void throwIfHealthStatusAbsorbsWrongAmountOfPathogen() {
        double pathogenConcentration = 1;
        double rate = 1;
        healthStatus.setPathogenAbsorptionRate(rate);
        ArrayList<InfectionStatus> statuses = new ArrayList<>(Arrays.asList(InfectionStatus.SUSCEPTIBLE, InfectionStatus.EXPOSED));
        for (InfectionStatus status : statuses) {
            healthStatus.setInfectionStatus(status);
            double expectedAbsorbedLoad = healthStatus.getPathogenAbsorbedLoad() + rate * pathogenConcentration;

            healthStatus.absorbPathogen(pathogenConcentration);
            Assert.assertEquals(expectedAbsorbedLoad, healthStatus.getPathogenAbsorbedLoad(), ALLOWED_DOUBLE_TOLERANCE);
        }
    }

    @Test
    public void throwIfHealthStatusAbsorbsPathogenAlthoughInNotAbsorbingState() {
        double pathogenConcentration = 1;
        double rate = 1;
        healthStatus.setPathogenAbsorptionRate(rate);
        ArrayList<InfectionStatus> statuses = new ArrayList<>(Arrays.asList(InfectionStatus.INFECTIOUS, InfectionStatus.RECOVERED));
        for (InfectionStatus status : statuses) {
            healthStatus.setInfectionStatus(status);
            double expectedAbsorbedLoad = healthStatus.getPathogenAbsorbedLoad();

            healthStatus.absorbPathogen(pathogenConcentration);
            Assert.assertEquals(expectedAbsorbedLoad, healthStatus.getPathogenAbsorbedLoad(), ALLOWED_DOUBLE_TOLERANCE);
        }
    }
}
