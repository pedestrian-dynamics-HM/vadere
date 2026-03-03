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

import java.io.File;
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
        File cacheDir = new File(new File(airFlowModel.airFlow.getScenarioPath()).getParent(), "cache");
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            cacheDir.delete();
        }
    }

    @Test
    public void testInitialize() {
        ArrayList<AttributesInOutLet> inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("west", 1., 2.));
        ArrayList<AttributesInOutLet> outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("east", 4., 5.));
        AttributesAirFlowModel attributesAirFlowModel = new AttributesAirFlowModel(2., 0.1, 1., 4000, inlets, outlets, new ArrayList<>(), new AttributesBounds());
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

    @Test
    public void testFlowDirectionOutsideBoundsReturnsZero() {
        initializeModel(true, false);
        airFlowModel.preLoop(0);

        double[] flow = airFlowModel.airFlow.getFlowDirection(0, -1, -1);
        assertEquals(0, flow[0], "X velocity outside bounds should be 0");
        assertEquals(0, flow[1], "Y velocity outside bounds should be 0");

        flow = airFlowModel.airFlow.getFlowDirection(0, 3, 3);
        assertEquals(0, flow[0], "X velocity outside bounds should be 0");
        assertEquals(0, flow[1], "Y velocity outside bounds should be 0");
    }

    @Test
    public void testFlowDirectionInsideBoundsReturnsNonZero() {
        initializeModel(true, false);
        airFlowModel.preLoop(0);
        double[] flow = airFlowModel.airFlow.getFlowDirection(0, 1, 1);
        assertTrue(flow[0] != 0 || flow[1] != 0,
                "Airflow at interior point should have non-zero velocity");
    }

    @Test
    public void testFlowDirectionBeforePreLoopReturnsZero() {
        initializeModel(true, false);
        double[] flow = airFlowModel.airFlow.getFlowDirection(0, 1, 1);
        assertEquals(0, flow[0], "X velocity before preLoop should be 0");
        assertEquals(0, flow[1], "Y velocity before preLoop should be 0");
    }

    @Test
    public void testShouldRemoveAerosolCloudMovingOutside() {
        initializeModel(true, false);
        airFlowModel.preLoop(0);

        // Start inside (1,1), shift takes it outside (1+2=3, 1+2=3)
        assertTrue(airFlowModel.airFlow.shouldRemoveAerosolCloud(1, 1, 2, 2),
                "Cloud moving from inside to outside bounds should be removed");
    }

    @Test
    public void testShouldNotRemoveAerosolCloudStayingInside() {
        initializeModel(true, false);
        airFlowModel.preLoop(0);

        // Start inside (1,1), small shift keeps it inside
        assertFalse(airFlowModel.airFlow.shouldRemoveAerosolCloud(1, 1, 0.1, 0.1),
                "Cloud staying inside bounds should not be removed");
    }

    @Test
    public void testShouldNotRemoveAerosolCloudAlreadyOutside() {
        initializeModel(true, false);
        airFlowModel.preLoop(0);

        // Start outside (5,5), still outside after shift
        assertFalse(airFlowModel.airFlow.shouldRemoveAerosolCloud(5, 5, 1, 1),
                "Cloud already outside bounds should not be flagged for removal");
    }

    @Test
    public void testBlockingObstaclesIDs() {
        initializeModel(true, false);
        airFlowModel.preLoop(0);

        List<Integer> ids = airFlowModel.airFlow.getBlockingObstaclesIDs();
        assertNotNull(ids, "Blocking obstacles list should not be null");
        assertTrue(ids.isEmpty(), "Default blocking obstacles list should be empty");
    }


    private void initializeModel(boolean rightParameters, boolean periodic) {
        ArrayList<AttributesInOutLet> inlets = new ArrayList<>();
        inlets.add(new AttributesInOutLet("west", 1., 2.));
        ArrayList<AttributesInOutLet> outlets = new ArrayList<>();
        outlets.add(new AttributesInOutLet("east", 4., 5.));
        AttributesAirFlowModel attributesAirFlowModel;
        if (rightParameters) {
            attributesAirFlowModel = new AttributesAirFlowModel(2., 0.1, 1., 4000, inlets, outlets, new ArrayList<>(), new AttributesBounds());
        } else {
            attributesAirFlowModel = new AttributesAirFlowModel(1., 0.1, 1., 4000, inlets, outlets, new ArrayList<>(), new AttributesBounds());
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