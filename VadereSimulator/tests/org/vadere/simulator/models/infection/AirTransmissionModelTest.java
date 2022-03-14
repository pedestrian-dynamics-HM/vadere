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
    public void testUpdateCreatesAerosolCloudsAlthoughNotActive() {
        setAerosolCloudsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);
        Assert.assertEquals(0, topography.getAerosolClouds().size());
    }

    @Test
    public void testUpdateCreatesDropletsAlthoughNotActive() {
        setDropletsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);
        Assert.assertEquals(0, topography.getDroplets().size());
    }

    @Test
    public void testUpdateWhenAerosolCloudsActive() {
        setAerosolCloudsActive(true);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);

        Assert.assertTrue(topography.getAerosolClouds().size() > 0);
    }

    @Test
    public void testUpdateWhenDropletsActive() {
        setDropletsActive(true);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);

        Assert.assertTrue(topography.getDroplets().size() > 0);
    }

    @Test
    public void testUpdateHealthStatusWhenAerosolCloudsAndDropletsNotActive() {
        setAerosolCloudsActive(false);
        setDropletsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElements().stream().filter(p -> !p.isInfectious()).findFirst().get();
        AirTransmissionModelHealthStatus actualStatus = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus();
        AirTransmissionModelHealthStatus expectedStatus = actualStatus.clone();

        runUpdate(simTime);

        Assert.assertEquals(expectedStatus, actualStatus);
    }

    @Test
    public void testUpdateHealthStatusWhenAerosolCloudsActive() {
        setAerosolCloudsActive(true);
        setDropletsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElements().stream().filter(p -> !p.isInfectious()).findFirst().get();
        AirTransmissionModelHealthStatus actualStatus = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus();
        AirTransmissionModelHealthStatus unexpectedStatus = actualStatus.clone();

        runUpdate(simTime);

        Assert.assertNotEquals(unexpectedStatus, actualStatus);
    }

    @Test
    public void testUpdateHealthStatusWhenDropletsActive() {
        setAerosolCloudsActive(false);
        setDropletsActive(true);
        airTransmissionModel.attrAirTransmissionModel.setDropletsAngleOfSpreadInDeg(359.999); // make sure that non-infectious pedestrian is really caught by droplets
        double simTime = getUpdateSimTime();
        initUpdate(simTime);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElements().stream().filter(p -> !p.isInfectious()).findFirst().get();
        AirTransmissionModelHealthStatus actualStatus = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus();
        AirTransmissionModelHealthStatus unexpectedStatus = actualStatus.clone();

        runUpdate(simTime);

        Assert.assertNotEquals(unexpectedStatus, actualStatus);
    }

    private double getUpdateSimTime() {
        double bufferTime = 2 * airTransmissionModel.attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod();
        double simDuration = bufferTime + Math.max(1 / airTransmissionModel.attrAirTransmissionModel.getDropletsEmissionFrequency(), airTransmissionModel.attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod());

        return simDuration;
    }

    private void initUpdate(double simTime) {

        // this is only necessary to assure that aerosol clouds or droplets remain until the simEndTime once they are inserted into the topography
        airTransmissionModel.attrAirTransmissionModel.setAerosolCloudHalfLife(simTime + 1);
        airTransmissionModel.attrAirTransmissionModel.setDropletsLifeTime(simTime + 1);

        Pedestrian pedestrian1 = createPedestrian();
        pedestrian1.setInfectious(true);
        VPoint pos1 = new VPoint(2,2);
        pedestrian1.setPosition(pos1);
        pedestrian1.setId(1);
        topography.addElement(pedestrian1);

        double distance = 0.5 * Math.min(airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialRadius(), airTransmissionModel.attrAirTransmissionModel.getDropletsDistanceOfSpread());
        Vector2D spacingBetweenPeds = new Vector2D(1, 1);
        spacingBetweenPeds.normalize(distance);

        Pedestrian pedestrian2 = createPedestrian();
        VPoint pos2 = pos1.add(spacingBetweenPeds);
        pedestrian2.setPosition(pos2);
        pedestrian2.setId(2);
        topography.addElement(pedestrian2);
    }

    private void runUpdate(double simEndTime) {
        double simTimeInSec;
        double simTimeStepLength = airTransmissionModel.simTimeStepLength;

        for (simTimeInSec = simStartTime; simTimeInSec < simEndTime; simTimeInSec += simTimeStepLength) {
            airTransmissionModel.update(simTimeInSec);
        }
    }

    @Test
    public void testUpdateExecuteAerosolCloudEmissionEvents() {
        setAerosolCloudsActive(true);
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
        setAerosolCloudsActive(true);

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
        setAerosolCloudsActive(true);
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
        setAerosolCloudsActive(true);
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

    private Pedestrian testUpdatePedestriansExposureToAerosolClouds(boolean pedestrianOutsideCloud) {
        createAerosolCloud(airTransmissionModel);

        double simTimeStepLength = airTransmissionModel.simTimeStepLength;
        int nInhalations = 2;
        double simEndTime = nInhalations * airTransmissionModel.attrAirTransmissionModel.getPedestrianRespiratoryCyclePeriod();

        Pedestrian pedestrian = createPedestrian();
        VPoint position = airTransmissionModel.topography.getAerosolClouds().stream().findFirst().get().getCenter();
        if (pedestrianOutsideCloud) {
            position = position.add(new VPoint(airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialRadius(), airTransmissionModel.attrAirTransmissionModel.getAerosolCloudInitialRadius()));
        }
            pedestrian.setPosition(position);
        topography.addElement(pedestrian);

        for (double simTimeInSec = simStartTime; simTimeInSec < simEndTime; simTimeInSec += simTimeStepLength) {
            airTransmissionModel.updatePedestriansExposureToAerosolClouds();
            airTransmissionModel.updatePedsHealthStatus(simTimeInSec);
        }

        return pedestrian;
    }

    @Test
    public void testUpdatePedestriansExposureWithinAerosolClouds() {
        Pedestrian pedestrian = testUpdatePedestriansExposureToAerosolClouds(false);

        Assert.assertTrue(pedestrian.getDegreeOfExposure() > 0);
    }

    @Test
    public void testUpdatePedestriansExposureOutsideAerosolClouds() {
        Pedestrian pedestrian = testUpdatePedestriansExposureToAerosolClouds(true);

        Assert.assertEquals(0, pedestrian.getDegreeOfExposure(), ALLOWED_DOUBLE_TOLERANCE);
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

    private void setAerosolCloudsActive(boolean active) {
        airTransmissionModel.attrAirTransmissionModel.setAerosolCloudsActive(active);
    }

    private void setDropletsActive(boolean active) {
        airTransmissionModel.attrAirTransmissionModel.setDropletsActive(active);
    }
}