package org.vadere.simulator.models.airflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.models.airflow.AttributesBounds;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;
import org.vadere.state.scenario.Topography;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        // Create a basic topography
        topography = new Topography();
        topography.setContextId("AirFlowModelTest");

        rdm = new Random(0);
        ctx = new VadereContext();
        ctx.put("scenarioPath", tempDir.toAbsolutePath().toString());
        VadereContext.add("AirFlowModelTest", ctx);
    }

    @AfterEach
    public void deleteAirflowFiles() {
        deleteFile(airFlowModel.airFlow.getScenarioPath() + "_test_hash" + "_Vx.txt");
        deleteFile(airFlowModel.airFlow.getScenarioPath() + "_test_hash" + "_Vy.txt");
    }

    @Test
    public void testInitialize() {
        ArrayList<AttributesInOutLet> inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("west", 1., 2.));
        ArrayList<AttributesInOutLet> outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("east", 4., 5.));
        AttributesAirFlowModel attributesAirFlowModel = new AttributesAirFlowModel(2., 0.1, 1., inlets, outlets, new ArrayList<>(), new AttributesBounds());
        attributesList.add(attributesAirFlowModel);

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
        String hash = "1234";
        airFlowModel.calculateAirFlow(hash);
        assertNull(airFlowModel.airFlow.getXVelocities(), "X velocities should be null before preLoop");
        assertNull(airFlowModel.airFlow.getYVelocities(), "Y velocities should be null before preLoop");
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0, 0));
        deleteFile(airFlowModel.airFlow.getScenarioPath() + "_" + hash + "_Vx.txt");
        deleteFile(airFlowModel.airFlow.getScenarioPath() + "_" + hash + "_Vy.txt");
    }

    @Test
    public void testSetupAirFlowWithExistingEmptyFile() {
        initializeModel(true, false);
        airFlowModel.calculateWrongAirFlow();
        assertNull(airFlowModel.airFlow.getXVelocities(), "X velocities should be null before preLoop");
        assertNull(airFlowModel.airFlow.getYVelocities(), "Y velocities should be null before preLoop");
        assertThrows(NullPointerException.class, () -> airFlowModel.preLoop(0));
    }

    @Test
    public void testPeriodicAirflow() {
        initializeModel(true, true);
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0, 0));
        double[] airFlow = airFlowModel.airFlow.getFlowDirection(0, 0, 0);
        assertFalse(Math.pow(airFlow[0], 2) + Math.pow(airFlow[1], 2) > 0);
        airFlow = airFlowModel.airFlow.getFlowDirection(0.5, 0, 0);
        assertFalse(Math.pow(airFlow[0], 2) + Math.pow(airFlow[1], 2) > 0);
        airFlow = airFlowModel.airFlow.getFlowDirection(1, 0, 0);
        assertTrue(Math.pow(airFlow[0], 2) + Math.pow(airFlow[1], 2) > 0);
        airFlow = airFlowModel.airFlow.getFlowDirection(1.5, 0, 0);
        assertTrue(Math.pow(airFlow[0], 2) + Math.pow(airFlow[1], 2) > 0);
        airFlow = airFlowModel.airFlow.getFlowDirection(2, 0, 0);
        assertFalse(Math.pow(airFlow[0], 2) + Math.pow(airFlow[1], 2) > 0);
        airFlow = airFlowModel.airFlow.getFlowDirection(2.5, 0, 0);
        assertFalse(Math.pow(airFlow[0], 2) + Math.pow(airFlow[1], 2) > 0);
    }

    private void initializeModel(boolean rightParameters, boolean periodic) {
        ArrayList<AttributesInOutLet> inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("west", 1., 2.));
        ArrayList<AttributesInOutLet> outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("east", 4., 5.));
        AttributesAirFlowModel attributesAirFlowModel;
        if (rightParameters) {
            attributesAirFlowModel = new AttributesAirFlowModel(2., 0.1, 1., inlets, outlets, new ArrayList<>(), new AttributesBounds());
        } else {
            attributesAirFlowModel = new AttributesAirFlowModel(1., 0.1, 1., inlets, outlets, new ArrayList<>(), new AttributesBounds());
        }
        if (periodic) {
            attributesAirFlowModel.setOffPeriod(1.0);
        }
        attributesList.clear();
        attributesList.add(attributesAirFlowModel);
        airFlowModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    private void deleteFile(String fileName) {
        try {
            Path path = Paths.get(fileName);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}