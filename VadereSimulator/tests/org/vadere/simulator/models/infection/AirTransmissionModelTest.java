package org.vadere.simulator.models.infection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.AirTransmissionModelHealthStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AirTransmissionModelTest {
    private static final double ALLOWED_DOUBLE_TOLERANCE = 10e-3;
    private static final double SIM_TIME_STEP_LENGTH = 0.4;

    List<Attributes> attributesList;
    AirTransmissionModel airTransmissionModel;
    Topography topography;
    VadereContext ctx;
    Random rdm;
    double simStartTime;

    @Before
    public void setUp() {
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesAirTransmissionModel());
        airTransmissionModel = new AirTransmissionModel();
        topography = new Topography();
        topography.setContextId("testId");
        rdm = new Random(0);
        ctx = new VadereContext();
        ctx.put(AirTransmissionModel.simStepLength, SIM_TIME_STEP_LENGTH);
        VadereContext.add(topography.getContextId(), ctx);
        simStartTime = 0.0;

        initializeTransmissionModel();
    }

    @After
    public void after() {
        attributesList.clear();
    }

    @Test
    public void testInitializeFindsAttributesList() {
        Assert.assertEquals(attributesList.get(0), airTransmissionModel.attrAirTransmissionModel);
    }

    @Test
    public void testInitializeGetsSimTimeStepLength() {
        Assert.assertEquals(ctx.get(AirTransmissionModel.simStepLength), airTransmissionModel.simTimeStepLength);
    }

    @Test
    public void testRegisterToScenarioElementControllerEvents() {
        // TODO ...
    }

    @Test
    public void testUpdate() {
        // TODO ...
    }

    @Test
    public void testUpdateExecuteAerosolCloudEmissionEvents() {
        setAerosolCloudsActive();
        Pedestrian pedestrian = createPedestrian();
        pedestrian.setInfectious(true);
        topography.addElement(pedestrian);

        double simEndTime = airTransmissionModel.attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod();
        for (double simTimeInSec = simStartTime; simTimeInSec <= simEndTime; simTimeInSec += airTransmissionModel.simTimeStepLength) {
            airTransmissionModel.executeAerosolCloudEmissionEvents(simTimeInSec);

            // the tested method requires that the pedestrian's health status is updated as well
            airTransmissionModel.updatePedsHealthStatus(simTimeInSec);
        }

        Assert.assertTrue(topography.getAerosolClouds().size()>0);
    }

    @Test
    public void testUpdateAerosolClouds() {
        // maybe not necessary
    }

    @Test
    public void testUpdateAerosolCloudsPathogenLoad() {
        setAerosolCloudsActive();

        double expectedPathogenLoad = airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad();
        double simStepWidth = airTransmissionModel.attrAirTransmissionModel.getAerosolCloudHalfLife();
        double simTimeInSec = simStartTime;
        int nSimSteps = 10;

        createAerosolCloud(airTransmissionModel);

        double[] modelPathogenLoads = new double[nSimSteps];
        double[] expectedPathogenLoads = new double[nSimSteps];
        for (int i = 0; i < nSimSteps; i++) {
            airTransmissionModel.updateAerosolCloudsPathogenLoad(simTimeInSec);

            modelPathogenLoads[i] = (topography.getAerosolClouds().stream().filter(a -> a.getId() == 1).findFirst().get().getCurrentPathogenLoad());
            expectedPathogenLoads[i] = (expectedPathogenLoad);

            simTimeInSec += simStepWidth;
            expectedPathogenLoad /= 2;
        }

        Assert.assertArrayEquals(expectedPathogenLoads, modelPathogenLoads, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdateAerosolCloudsExtentDueToDispersion() {
        setAerosolCloudsActive();
        double dispersionFactor = 0.001; // dispersion in meter / simStep
        airTransmissionModel.attrAirTransmissionModel.setAirDispersionFactor(dispersionFactor); // defines time-dependent dispersion
        airTransmissionModel.attrAirTransmissionModel.setPedestrianDispersionWeight(0.0); // ped movement has no effect

        int nSimSteps = 100;
        double simEndTime = nSimSteps * airTransmissionModel.simTimeStepLength;

        createAerosolCloud(airTransmissionModel);

        double radius = calculateAerosolCloudRadius(airTransmissionModel, nSimSteps);
        double expectedRadius = airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialRadius() + simEndTime * dispersionFactor;

        Assert.assertEquals(expectedRadius, radius, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdateAerosolCloudsExtentDueToDispersionIndependentFromSimStepLength() {
        setAerosolCloudsActive();
        double dispersionFactor = 0.001;
        airTransmissionModel.attrAirTransmissionModel.setAirDispersionFactor(dispersionFactor); // defines time-dependent dispersion
        airTransmissionModel.attrAirTransmissionModel.setPedestrianDispersionWeight(0.0); // ped movement has no effect
        createAerosolCloud(airTransmissionModel);
        double simTimeStepLength = airTransmissionModel.simTimeStepLength;
        int nSimSteps = 2500;

        double simEndTime = nSimSteps * simTimeStepLength;

        int nSimSteps2 = 2000;
        double simTimeStepLength2 = simEndTime / nSimSteps2;

        // second AirTransmissionModel with different simTimeStepLength
        AirTransmissionModel airTransmissionModel2 = new AirTransmissionModel();
        Topography topography2 = new Topography();
        topography2.setContextId("testId2");
        rdm = new Random(0);
        VadereContext ctx2 = new VadereContext();
        ctx2.put(AirTransmissionModel.simStepLength, simTimeStepLength2); // chosen arbitrarily, not too high
        VadereContext.add(topography2.getContextId(), ctx2);
        airTransmissionModel2.initialize(attributesList, new Domain(topography2), null, rdm);
        airTransmissionModel2.attrAirTransmissionModel.setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel2);

        double radius = calculateAerosolCloudRadius(airTransmissionModel, nSimSteps);
        double radius2 = calculateAerosolCloudRadius(airTransmissionModel2, nSimSteps2);

        Assert.assertEquals(radius, radius2, ALLOWED_DOUBLE_TOLERANCE);
    }

    private double calculateAerosolCloudRadius(AirTransmissionModel airTransmissionModel, int nSimSteps) {
        for (int i = 1; i <= nSimSteps; i++) {
            airTransmissionModel.updateAerosolCloudsExtent();
        }
        return airTransmissionModel.topography.getAerosolClouds().stream().findFirst().get().getRadius();
    }


    @Test
    public void testUpdateAerosolCloudsExtentAgentMovement() {
        createAerosolCloud(airTransmissionModel);

        double simTimeStepLength = airTransmissionModel.simTimeStepLength;
        int nSimSteps = 2; // arbitrarily chosen

        Pedestrian pedestrian = createPedestrian();
        VPoint positionWithinCloud = airTransmissionModel.topography.getAerosolClouds().stream().findFirst().get().getCenter();
        pedestrian.setPosition(positionWithinCloud);
        Vector2D velocity = new Vector2D (10, 10);
        pedestrian.setVelocity(velocity);
        topography.addElement(pedestrian);

        double radius = calculateAerosolCloudRadius(airTransmissionModel, nSimSteps);
        double expectedRadius = airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialRadius() + nSimSteps * velocity.getLength() * simTimeStepLength * airTransmissionModel.attrAirTransmissionModel.getAerosolCloudPedestrianDispersionWeight();

        Assert.assertEquals(expectedRadius, radius, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testDeleteExpiredAerosolClouds() {
        createAerosolCloud(airTransmissionModel);
        AerosolCloud aerosolCloud = topography.getAerosolClouds().stream().findFirst().get();
        double negligiblePathogenConcentr = (AirTransmissionModel.minimumPercentage * 0.9) * aerosolCloud.getPathogenConcentration();
        aerosolCloud.setCurrentPathogenLoad(negligiblePathogenConcentr);

        airTransmissionModel.deleteExpiredAerosolClouds();

        Assert.assertTrue(topography.getAerosolClouds().stream().collect(Collectors.toSet()).isEmpty());
    }

    @Test
    public void testUpdatePedestriansExposureToAerosolClouds() {

    }

    @Test
    public void testExecuteDropletEmissionEvents() {

    }

    @Test
    public void testUpdateDroplets() {

    }

    @Test
    public void testUpdatePedestriansExposureToDroplets() {

    }

    @Test
    public void testUpdatePedsHealthStatus() {

    }

    private void initializeTransmissionModel() {
        airTransmissionModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    private Pedestrian createPedestrian() {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), rdm);
        pedestrian.setHealthStatus(new AirTransmissionModelHealthStatus());
        return pedestrian;
    }

    private void createAerosolCloud(AirTransmissionModel airTransmissionModel) {
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(1,
                airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialRadius(),
                new VPoint(5, 5), // position is not important for only a few tests
                simStartTime,
                airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialPathogenLoad()));
        airTransmissionModel.topography.addAerosolCloud(aerosolCloud);
    }

    private void setAerosolCloudsActive() {
        airTransmissionModel.attrAirTransmissionModel.setAerosolCloudsActive(true);
    }

    private void setDropletsActive() {
        airTransmissionModel.attrAirTransmissionModel.setDropletsActive(true);
    }
}