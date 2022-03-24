package org.vadere.simulator.models.infection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesProximityExposureModel;
import org.vadere.state.attributes.models.infection.AttributesThresholdResponseModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.BasicExposureModelHealthStatus;
import org.vadere.state.health.DoseResponseModelInfectionStatus;
import org.vadere.state.health.ThresholdResponseModelInfectionStatus;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThresholdResponseModelTest {
    private List<Attributes> attributesList;
    private ThresholdResponseModel thresholdResponseModel;
    private Topography topography;
    private Random rdm;
    private double simStartTime;

    @Before
    public void setUp() {
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesProximityExposureModel()); // alternatively use any other class extending AbstractExposureModel
        attributesList.add(new AttributesThresholdResponseModel());
        thresholdResponseModel = new ThresholdResponseModel();
        topography = new Topography();
        rdm = new Random(0);
        simStartTime = 0.0;

        thresholdResponseModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    @After
    public void tearDown(){
        attributesList.clear();
    }

    @Test
    public void registerToScenarioElementControllerEvents() {
    }

    @Test(expected = RuntimeException.class)
    public void testInitializeThrowsErrorIfNoExposureModelDefined() {
        attributesList.clear();
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesThresholdResponseModel());

        thresholdResponseModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    @Test
    public void testInitializeFindsAttributes() {
        Assert.assertTrue(attributesList.contains(thresholdResponseModel.attributesThresholdResponseModel));
    }

    @Test
    public void testRegisterToScenarioElementControllerEvents() {
    }

    @Test
    public void sourceControllerEvent() {
    }

    @Test
    public void testUpdateMinProbabilityOfInfection() {
        double probabilityOfInfection = testUpdate(0.9);

        Assert.assertEquals(0, probabilityOfInfection, 0.0);
    }

    @Test
    public void testUpdateMaxProbabilityOfInfection() {
        double probabilityOfInfection = testUpdate(1); // argument >= 1

        Assert.assertEquals(1, probabilityOfInfection, 0.0);
    }

    private double testUpdate(double percentageOfInfectionThreshold) {
        int pedId = 1;
        double exposureBelowThreshold = thresholdResponseModel.attributesThresholdResponseModel.getExposureToInfectedThreshold() * percentageOfInfectionThreshold;
        createPedestrian(pedId, exposureBelowThreshold);

        thresholdResponseModel.update(simStartTime);

        return topography.getPedestrianDynamicElements().getElement(pedId).getProbabilityOfInfection();
    }

    @Test
    public void topographyControllerEventDefinesInfectionStatus() {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), rdm);
        Pedestrian defaultPedestrian = pedestrian.clone();
        DoseResponseModelInfectionStatus defaultInfectionStatus = defaultPedestrian.getInfectionStatus();

        TopographyController controller = new TopographyController(new Domain(topography), new OptimalStepsModel(), rdm);

        pedestrian = thresholdResponseModel.topographyControllerEvent(controller, simStartTime, pedestrian);
        DoseResponseModelInfectionStatus instantiatedInfectionStatus = pedestrian.getInfectionStatus();

        Assert.assertNotEquals(defaultInfectionStatus, instantiatedInfectionStatus);
        Assert.assertSame(instantiatedInfectionStatus.getClass(), ThresholdResponseModelInfectionStatus.class);
    }

    private void createPedestrian(int id, double degreeOfExposure) {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(id), rdm);

        pedestrian.setHealthStatus(new BasicExposureModelHealthStatus());
        pedestrian.setDegreeOfExposure(degreeOfExposure);

        pedestrian.setInfectionStatus(new ThresholdResponseModelInfectionStatus());

        topography.addElement(pedestrian);
    }
}