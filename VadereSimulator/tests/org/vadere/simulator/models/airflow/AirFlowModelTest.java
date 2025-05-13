package org.vadere.simulator.models.airflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.Topography;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AirFlowModelTest {

    @TempDir
    static Path tempDir;

    List<Attributes> attributesList;
    AirFlowModelTester airFlowModel;
    Topography topography;
    VadereContext ctx;
    Random rdm;

    @BeforeEach
    public void setUp() {
        attributesList = new ArrayList<>();
        airFlowModel = new AirFlowModelTester();
        topography = new Topography(new AttributesTopography(), null);
        topography.setContextId("AirFlowModelTest");
        topography.initAirFlow(tempDir.toAbsolutePath() + "/scenarioName", "scenarioHash");
        rdm = new Random(0);
        ctx = new VadereContext();
        VadereContext.add(topography.getContextId(), ctx);
    }

    @Test
    public void testInitialize() {
        initializeModel(true, false);
        assertNotNull(airFlowModel.airFlow);
        assertNotNull(airFlowModel.attributesAirFlowModel);
    }

    @Test
    public void testSetupAirFlow() {
        initializeModel(true, false);
        assertNull(airFlowModel.airFlow.getXVelocities(), "X velocities should be null before preLoop");
        assertNull(airFlowModel.airFlow.getYVelocities(), "Y velocities should be null before preLoop");
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0, 0));
    }

    @Test
    public void testSetupAirFlowWithExistingFile() {
        initializeModel(true, false);
        airFlowModel.calculateAirFlow("1234");
        assertNull(airFlowModel.airFlow.getXVelocities(), "X velocities should be null before preLoop");
        assertNull(airFlowModel.airFlow.getYVelocities(), "Y velocities should be null before preLoop");
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0, 0));
    }

    @Test
    public void testSetupAirFlowWithExistingFileAndRecalculation() {
        initializeModel(true, false);
        airFlowModel.calculateWrongAirFlow();
        assertNull(airFlowModel.airFlow.getXVelocities(), "X velocities should be null before preLoop");
        assertNull(airFlowModel.airFlow.getYVelocities(), "Y velocities should be null before preLoop");
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0, 0));
    }

    @Test
    public void testSetupAirFlowWithCalculationWithWrongParameters() {
        initializeModel(false, false);
        assertThrows(IllegalArgumentException.class,() -> airFlowModel.preLoop(0));
    }

    @Test
    public void testPeriodicAirflow() {
        initializeModel(true, true);
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0, 0));
        double[] airflow = airFlowModel.airFlow.getFlowDirection(0, 0, 0);
        assertFalse(Math.pow(airflow[0], 2) + Math.pow(airflow[1], 2) > 0);
        airflow = airFlowModel.airFlow.getFlowDirection(0.5, 0, 0);
        assertFalse(Math.pow(airflow[0], 2) + Math.pow(airflow[1], 2) > 0);
        airflow = airFlowModel.airFlow.getFlowDirection(1, 0, 0);
        assertTrue(Math.pow(airflow[0], 2) + Math.pow(airflow[1], 2) > 0);
        airflow = airFlowModel.airFlow.getFlowDirection(1.5, 0, 0);
        assertTrue(Math.pow(airflow[0], 2) + Math.pow(airflow[1], 2) > 0);
        airflow = airFlowModel.airFlow.getFlowDirection(2, 0, 0);
        assertFalse(Math.pow(airflow[0], 2) + Math.pow(airflow[1], 2) > 0);
        airflow = airFlowModel.airFlow.getFlowDirection(2.5, 0, 0);
        assertFalse(Math.pow(airflow[0], 2) + Math.pow(airflow[1], 2) > 0);
    }

    private void initializeModel(boolean rightParameters, boolean periodic) {
        ArrayList<AttributesInOutLet> inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("left", 1., 2.));
        ArrayList<AttributesInOutLet> outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("right", 4., 5.));
        AttributesAirFlowModel attributesAirFlowModel;
        if (rightParameters) {
            attributesAirFlowModel = new AttributesAirFlowModel(2., 0.1, 1., inlets, outlets, new ArrayList<>());

        } else {
            attributesAirFlowModel = new AttributesAirFlowModel(1., 0.1, 1., inlets, outlets, new ArrayList<>());
        }
        if (periodic) {
            attributesAirFlowModel.setOffPeriod(1.0);
        }
        attributesList.add(attributesAirFlowModel);
        airFlowModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    @Test
    public void testGetAttributesString() {
        ArrayList<AttributesInOutLet> inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("left", 1., 2.));
        inlets.add(new AttributesInOutLet("top", 0.2, 2.4));
        ArrayList<AttributesInOutLet> outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("right", 4., 5.));
        outlets.add(new AttributesInOutLet("bottom", 1.3, 2.3));
        ArrayList<Integer> notBlockingObstacles = new ArrayList<>();
        notBlockingObstacles.add(1);
        notBlockingObstacles.add(2);
        notBlockingObstacles.add(3);
        AttributesAirFlowModel attributesAirFlowModel = new AttributesAirFlowModel(0.5, 0.2, 0.1, inlets, outlets, notBlockingObstacles);

        String actualAttributesString = AirFlowModel.getAttributesString(attributesAirFlowModel);
        assertEquals("0.5-0.2-0.1-left[1.0,2.0]top[0.2,2.4]-right[4.0,5.0]bottom[1.3,2.3]-[1, 2, 3]", actualAttributesString);
    }
}