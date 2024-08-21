package org.vadere.simulator.models.airflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
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
        System.out.println(tempDir.toAbsolutePath());

        attributesList = new ArrayList<>();
        attributesList.add(new AttributesAirFlowModel());
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
        initializeModel();
        assertNotNull(airFlowModel.airFlow);
        assertNotNull(airFlowModel.attributesAirFlowModel);
    }

    @Test
    public void testInitializeWithMissingAttributes() {
        attributesList.clear();
        assertThrows(AttributesNotFoundException.class, this::initializeModel);
    }

    @Test
    public void testSetupAirFlow() {
        initializeModel();
        assertThrows(NullPointerException.class,() -> airFlowModel.airFlow.getFlowDirection(0, 0));
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0, 0));
    }

    private void initializeModel() {
        airFlowModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

}