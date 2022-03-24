package org.vadere.simulator.models.infection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesProximityExposureModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.BasicExposureModelHealthStatus;
import org.vadere.state.health.ExposureModelHealthStatus;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProximityExposureModelTest {
    private static final double ALLOWED_DOUBLE_TOLERANCE = 10e-6;
    private List<Attributes> attributesList;
    private ProximityExposureModel proximityExposureModel;
    private Topography topography;
    private Random rdm;
    private double simStartTime;

    @Before
    public void setUp() {
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesProximityExposureModel());
        proximityExposureModel = new ProximityExposureModel();
        topography = new Topography();
        rdm = new Random(0);
        simStartTime = 0.0;

        proximityExposureModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    @After
    public void after() {
        attributesList.clear();
    }

    //TODO test not complete yet
//    public void testRegisterToScenarioElementControllerEvents() {
//        int sourceId = 1;
//        Source source = new Source(new AttributesSource(sourceId, new VRectangle(1, 1, 1, 1)));
//        topography.addSource(source);
//
//        proximityExposureModel.attributesProximityExposureModel
//                .getExposureModelSourceParameters()
//                .add(new AttributesExposureModelSourceParameters(sourceId, true));
//
//        Simulation controllerProvider = new Simulation();
//
//        proximityExposureModel.registerToScenarioElementControllerEvents(controllerProvider);
//    }

    @Test
    public void testInitialize() {
        Assert.assertEquals(attributesList.get(0), proximityExposureModel.attributesProximityExposureModel);
    }

    @Test
    public void testUpdateNoInfectiousAgentsPresent() {
        double expectedDegreeOfExposure = new BasicExposureModelHealthStatus().getDegreeOfExposure();
        double actualDegreeOfExposure = testUpdate(false);

        Assert.assertEquals(expectedDegreeOfExposure, actualDegreeOfExposure, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdateAgentWithinRangeOfInfectiousAgent() {
        double expectedDegreeOfExposure = proximityExposureModel.MAX_DEG_OF_EXPOSURE;
        double actualDegreeOfExposure = testUpdate(true);

        Assert.assertEquals(expectedDegreeOfExposure, actualDegreeOfExposure, ALLOWED_DOUBLE_TOLERANCE);
    }

    public double testUpdate(boolean infectiousAgentPresent) {
        double maxExposureRadius = proximityExposureModel.attributesProximityExposureModel.getExposureRadius();
        VPoint pos1 = new VPoint(1, 1);
        Vector2D distance = new Vector2D(1, 0);
        distance = distance.normalize(maxExposureRadius * 0.5);
        VPoint pos2 = pos1.add(distance);

        int notInfectiousId = 2;

        createPedestrian(1, pos1, infectiousAgentPresent);
        createPedestrian(notInfectiousId, pos2, false);

        proximityExposureModel.update(simStartTime);

        return topography.getPedestrianDynamicElements().getElement(notInfectiousId).getDegreeOfExposure();
    }

    @Test
    public void testUpdatePedestrianDegreeOfExposure() {
        Pedestrian pedestrian = createPedestrian();
        double expectedDegreeOfExposure = 1;
        proximityExposureModel.updatePedestrianDegreeOfExposure(pedestrian, expectedDegreeOfExposure);

        Assert.assertEquals(expectedDegreeOfExposure, pedestrian.getDegreeOfExposure(), ALLOWED_DOUBLE_TOLERANCE);
    }

    public void testSourceControllerEvent() {
    }

    @Test
    public void testTopographyControllerEventDefinesInfectiousPedestrian() {
        int pedestrianId = 1;
        proximityExposureModel.attributesProximityExposureModel.addInfectiousPedestrianIdsNoSource(pedestrianId);
        Agent agent = new Pedestrian(new AttributesAgent(pedestrianId), rdm);

        Pedestrian pedestrian = proximityExposureModel.topographyControllerEvent(getTopographyController(new OptimalStepsModel()), simStartTime, agent);

        Assert.assertTrue(pedestrian.isInfectious());
    }

    @Test
    public void testTopographyControllerEventInstantiatesHealthStatus() {
        int pedestrianId = 1;
        proximityExposureModel.attributesProximityExposureModel.addInfectiousPedestrianIdsNoSource(pedestrianId);
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(pedestrianId), rdm);
        Pedestrian defaultPedestrian = pedestrian.clone();
        ExposureModelHealthStatus defaultHealthStatus = defaultPedestrian.getHealthStatus();

        pedestrian = proximityExposureModel.topographyControllerEvent(getTopographyController(new OptimalStepsModel()), simStartTime, pedestrian);
        ExposureModelHealthStatus instantiatedHealthStatus = pedestrian.getHealthStatus();

        Assert.assertNotEquals(defaultHealthStatus, instantiatedHealthStatus);
        Assert.assertSame(instantiatedHealthStatus.getClass(), BasicExposureModelHealthStatus.class);
    }

    private TopographyController getTopographyController(MainModel mainModel) {
        return new TopographyController(new Domain(topography), mainModel, rdm);
    }

    private Pedestrian createPedestrian() {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), rdm);
        pedestrian.setHealthStatus(new BasicExposureModelHealthStatus());
        return pedestrian;
    }

    private void createPedestrian(int id, VPoint position, boolean infectious) {
        Pedestrian pedestrian = createPedestrian();

        pedestrian.setId(id);

        double maxExposureRadius = proximityExposureModel.attributesProximityExposureModel.getExposureRadius();
        VPoint pos1 = new VPoint(1, 1);
        Vector2D distance = new Vector2D(Math.random(), Math.random());
        distance.normalize(maxExposureRadius * 0.5);
        VPoint pos2 = pos1.add(distance);

        pedestrian.setPosition(position);

        if (infectious) {
            pedestrian.setInfectious(true);
        } else {
            pedestrian.setInfectious(false);
        }

        topography.addElement(pedestrian);
    }
}