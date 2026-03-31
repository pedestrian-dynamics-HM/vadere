package org.vadere.simulator.models.airflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.airflow.AttributesLinearAirFlowModel;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class LinearAirFlowModelTest {

    List<Attributes> attributesList;
    LinearAirFlowModel airFlowModel;
    Topography topography;
    VadereContext ctx;
    Random rdm;

    @BeforeEach
    public void setUp() {
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesLinearAirFlowModel());
        airFlowModel = new LinearAirFlowModel();
        topography = new Topography(new AttributesTopography(), null);
        topography.setContextId("AirFlowModelTest");
        rdm = new Random(0);
        ctx = new VadereContext();
        ctx.put("scenarioPath", "scenarioName");
        VadereContext.add(topography.getContextId(), ctx);
    }

    @Test
    public void testInitialize() {
        initializeModel();
        assertNotNull(airFlowModel.airFlow);
        assertNotNull(airFlowModel.attributesLinearAirFlowModel);
    }

    @Test
    public void testInitializeWithMissingAttributes() {
        attributesList.clear();
        assertThrows(AttributesNotFoundException.class, this::initializeModel);
    }

    @Test
    public void testSetupAirFlow() {
        initializeModel();
        airFlowModel.preLoop(0);
        assertNotNull(airFlowModel.airFlow.getFlowDirection(0,0, 0));
    }

    @Test
    public void testXYVelocities() {
        initializeModel();
        airFlowModel.preLoop(0);
        double[] result = airFlowModel.airFlow.getFlowDirection(0, 1, 1);
        assertEquals(Math.atan2(result[1], result[0]), airFlowModel.attributesLinearAirFlowModel.getAirflowDirection());
        assertEquals(Math.sqrt(Math.pow(result[0], 2) + Math.pow(result[1], 2)), airFlowModel.attributesLinearAirFlowModel.getAirflowSpeed());
    }

    private void initializeModel() {
        airFlowModel.initialize(attributesList, new Domain(topography), null, rdm);
    }
}
