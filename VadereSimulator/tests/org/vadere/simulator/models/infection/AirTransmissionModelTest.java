package org.vadere.simulator.models.infection;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.health.AirTransmissionModelHealthStatus;
import org.vadere.state.health.ExposureModelHealthStatus;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AirTransmissionModelTest {
    private static final double ALLOWED_DOUBLE_TOLERANCE = 10e-6;
    private static final double SIM_TIME_STEP_LENGTH = 0.4;

    List<Attributes> attributesList;
    AirTransmissionModel airTransmissionModel;
    Topography topography;
    VadereContext ctx;
    Random rdm;
    double simStartTime;

    @BeforeEach
    public void setUp() {
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesAirTransmissionModel());
        airTransmissionModel = new AirTransmissionModel();
        topography = new Topography();
        topography.setContextId("testId");

        rdm = new Random(0);
        ctx = new VadereContext();
        ctx.put(AirTransmissionModel.simStepLength, SIM_TIME_STEP_LENGTH);
        ctx.put("scenarioPath", "test");
        VadereContext.add(topography.getContextId(), ctx);
        simStartTime = 0.0;

        // Initialize AirFlow
        AirFlow airFlow = new AirFlow("test", "test_hash", 0.0, 0.0, 10, 10);
        airFlow.setRectangularGridCellSize(Double.POSITIVE_INFINITY);
        topography.setAirFlow(airFlow);

        initializeTransmissionModel();
    }

    @AfterEach
    public void after() {
        attributesList.clear();
    }

    @Test
    public void testInitializeFindsAttributesList() {
        assertEquals(attributesList.get(0), airTransmissionModel.getAttributes());
    }

    @Test
    public void testInitializeGetsSimTimeStepLength() {
        assertEquals(ctx.get(AirTransmissionModel.simStepLength), airTransmissionModel.simTimeStepLength);
    }

    @Test
    public void testTopographyControllerEventDefinesInfectiousPedestrian() {
        int pedestrianId = 1;
        airTransmissionModel.getAttributes().addInfectiousPedestrianIdsNoSource(pedestrianId);
        Agent agent = new Pedestrian(new AttributesAgent(pedestrianId), rdm);

        Pedestrian pedestrian = airTransmissionModel.topographyControllerEvent(getTopographyController(new OptimalStepsModel()), simStartTime, agent);

        assertTrue(pedestrian.isInfectious());
    }

    @Test
    public void testTopographyControllerEventInstantiatesHealthStatus() {
        int pedestrianId = 1;
        airTransmissionModel.getAttributes().addInfectiousPedestrianIdsNoSource(pedestrianId);
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(pedestrianId), rdm);
        Pedestrian defaultPedestrian = pedestrian.clone();
        ExposureModelHealthStatus defaultHealthState = defaultPedestrian.getHealthStatus();

        pedestrian = airTransmissionModel.topographyControllerEvent(getTopographyController(new OptimalStepsModel()), simStartTime, pedestrian);
        ExposureModelHealthStatus instantiatedHealthStatus = pedestrian.getHealthStatus();

        assertNotEquals(defaultHealthState, instantiatedHealthStatus);
        assertSame(instantiatedHealthStatus.getClass(), AirTransmissionModelHealthStatus.class);
    }

    private TopographyController getTopographyController(MainModel mainModel) {
        return new TopographyController(new Domain(topography), mainModel, rdm);
    }

    @Test
    public void testUpdateCreatesAerosolCloudsAlthoughNotActive() {
        setAerosolCloudsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);
        assertEquals(0, topography.getAerosolClouds().size());
    }

    @Test
    public void testUpdateCreatesDropletsAlthoughNotActive() {
        setDropletsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);
        assertEquals(0, topography.getDroplets().size());
    }

    @Test
    public void testUpdateWhenAerosolCloudsActive() {
        setAerosolCloudsActive(true);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);

        assertFalse(topography.getAerosolClouds().isEmpty());
    }

    @Test
    public void testUpdateWhenDropletsActive() {
        setDropletsActive(true);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);
        runUpdate(simTime);

        assertFalse(topography.getDroplets().isEmpty());
    }

    @Test
    public void testUpdateHealthStatusWhenAerosolCloudsAndDropletsNotActive() {
        setAerosolCloudsActive(false);
        setDropletsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElements().stream().filter(p -> !p.isInfectious()).findFirst().get();
        AirTransmissionModelHealthStatus actualStatus = pedestrian.getHealthStatus();
        AirTransmissionModelHealthStatus expectedStatus = actualStatus.clone();

        runUpdate(simTime);

        assertEquals(expectedStatus, actualStatus);
    }

    @Test
    public void testUpdateHealthStatusWhenAerosolCloudsActive() {
        setAerosolCloudsActive(true);
        setDropletsActive(false);
        double simTime = getUpdateSimTime();
        initUpdate(simTime);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElements().stream().filter(p -> !p.isInfectious()).findFirst().get();
        AirTransmissionModelHealthStatus actualStatus = pedestrian.getHealthStatus();
        AirTransmissionModelHealthStatus unexpectedStatus = actualStatus.clone();

        runUpdate(simTime);

        assertNotEquals(unexpectedStatus, actualStatus);
    }

    @Test
    public void testUpdateHealthStatusWhenDropletsActive() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        setAerosolCloudsActive(false);
        setDropletsActive(true);
        attrModel.setDropletsAngleOfSpreadInDeg(359.999); // make sure that non-infectious pedestrian is really caught by droplets
        double simTime = getUpdateSimTime();
        initUpdate(simTime);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElements().stream().filter(p -> !p.isInfectious()).findFirst().get();
        AirTransmissionModelHealthStatus actualStatus = pedestrian.getHealthStatus();
        AirTransmissionModelHealthStatus unexpectedStatus = actualStatus.clone();

        runUpdate(simTime);

        assertNotEquals(unexpectedStatus, actualStatus);
    }

    private double getUpdateSimTime() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        double bufferTime = 2 * attrModel.getPedestrianRespiratoryCyclePeriod();

        return bufferTime + Math.max(1 / attrModel.getDropletsEmissionFrequency(), attrModel.getPedestrianRespiratoryCyclePeriod());
    }

    private void initUpdate(double simTime) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();

        // this is only necessary to assure that aerosol clouds or droplets remain until the simEndTime once they are inserted into the topography
        attrModel.setAerosolCloudHalfLife(simTime + 1);
        attrModel.setDropletsLifeTime(simTime + 1);

        Pedestrian pedestrian1 = createPedestrian();
        pedestrian1.setInfectious(true);
        VPoint pos1 = new VPoint(2,2);
        pedestrian1.setPosition(pos1);
        pedestrian1.setId(1);
        topography.addElement(pedestrian1);

        double distance = 0.5 * Math.min(attrModel.getAerosolCloudInitialRadius(), attrModel.getDropletsDistanceOfSpread());
        Vector2D spacingBetweenPeds = new Vector2D(1, 1);
        spacingBetweenPeds = spacingBetweenPeds.normalize(distance);

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
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        setAerosolCloudsActive(true);
        Pedestrian pedestrian = createPedestrian();
        pedestrian.setInfectious(true);
        topography.addElement(pedestrian);

        double simEndTime = attrModel.getPedestrianRespiratoryCyclePeriod();
        for (double simTimeInSec = simStartTime; simTimeInSec <= simEndTime; simTimeInSec += airTransmissionModel.simTimeStepLength) {
            airTransmissionModel.executeAerosolCloudEmissionEvents(simTimeInSec);

            // the tested method requires that the pedestrian's health status is updated as well
            airTransmissionModel.updatePedestriansHealthStatus(simTimeInSec);
        }

        assertFalse(topography.getAerosolClouds().isEmpty());
    }

    @Test
    public void testUpdateAerosolCloudsPathogenLoad() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        attrModel.setAerosolCloudHalfLife(SIM_TIME_STEP_LENGTH);
        
        setAerosolCloudsActive(true);

        double expectedPathogenLoad = attrModel.getAerosolCloudInitialPathogenLoad();
        double simStepWidth = attrModel.getAerosolCloudHalfLife();
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
        assertArrayEquals(expectedPathogenLoads, modelPathogenLoads, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdateAerosolCloudsExtentDueToDispersion() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        setAerosolCloudsActive(true);
        double dispersionFactor = 0.001; // dispersion in meter / simStep
        attrModel.setAirDispersionFactor(dispersionFactor); // defines time-dependent dispersion
        attrModel.setPedestrianDispersionWeight(0.0); // ped movement has no effect

        int nSimSteps = 100;
        double simEndTime = nSimSteps * airTransmissionModel.simTimeStepLength;

        createAerosolCloud(airTransmissionModel);

        double radius = calculateAerosolCloudRadius(airTransmissionModel, nSimSteps);
        double expectedRadius = attrModel.getAerosolCloudInitialRadius() + simEndTime * dispersionFactor;

        assertEquals(expectedRadius, radius, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testUpdateAerosolCloudsExtentDueToDispersionIndependentFromSimStepLength() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        setAerosolCloudsActive(true);
        double dispersionFactor = 0.001;
        attrModel.setAirDispersionFactor(dispersionFactor); // defines time-dependent dispersion
        attrModel.setPedestrianDispersionWeight(0.0); // ped movement has no effect
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
        AttributesAirTransmissionModel attrModel2 = (AttributesAirTransmissionModel) airTransmissionModel2.getAttributes();

        attrModel2.setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel2);

        double radius = calculateAerosolCloudRadius(airTransmissionModel, nSimSteps);
        double radius2 = calculateAerosolCloudRadius(airTransmissionModel2, nSimSteps2);

        assertEquals(radius, radius2, ALLOWED_DOUBLE_TOLERANCE);
    }

    private double calculateAerosolCloudRadius(AirTransmissionModel airTransmissionModel, int nSimSteps) {
        for (int i = 1; i <= nSimSteps; i++) {
            airTransmissionModel.updateAerosolCloudsExtent();
        }
        return airTransmissionModel.topography.getAerosolClouds().stream().findFirst().get().getRadius();
    }

    @Test
    public void testUpdateAerosolCloudsExtentAgentMovement() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        attrModel.setAirDispersionFactor(0);

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
        double expectedRadius = attrModel.getAerosolCloudInitialRadius() + nSimSteps * velocity.getLength() * simTimeStepLength * attrModel.getAerosolCloudPedestrianDispersionWeight();

        assertEquals(expectedRadius, radius, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testDeleteExpiredAerosolClouds() {
        createAerosolCloud(airTransmissionModel);
        AerosolCloud aerosolCloud = topography.getAerosolClouds().stream().findFirst().get();
        double negligiblePathogenConcentr = (AirTransmissionModel.minimumPercentage * 0.9) * aerosolCloud.getPathogenConcentration();
        aerosolCloud.setCurrentPathogenLoad(negligiblePathogenConcentr);

        airTransmissionModel.deleteExpiredAerosolClouds();

        assertTrue(new HashSet<>(topography.getAerosolClouds()).isEmpty());
    }

    private Pedestrian testUpdatePedestriansExposureToAerosolClouds(boolean pedestrianOutsideCloud) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        createAerosolCloud(airTransmissionModel);

        double simTimeStepLength = airTransmissionModel.simTimeStepLength;
        int nInhalations = 2;
        double simEndTime = nInhalations * attrModel.getPedestrianRespiratoryCyclePeriod();

        Pedestrian pedestrian = createPedestrian();
        VPoint position = airTransmissionModel.topography.getAerosolClouds().stream().findFirst().get().getCenter();
        if (pedestrianOutsideCloud) {
            position = position.add(new VPoint(attrModel.getAerosolCloudInitialRadius(), attrModel.getAerosolCloudInitialRadius()));
        }
            pedestrian.setPosition(position);
        topography.addElement(pedestrian);

        for (double simTimeInSec = simStartTime; simTimeInSec < simEndTime; simTimeInSec += simTimeStepLength) {
            airTransmissionModel.updatePedestriansExposureToAerosolClouds();
            airTransmissionModel.updatePedestriansHealthStatus(simTimeInSec);
        }

        return pedestrian;
    }

    @Test
    public void testUpdatePedestriansExposureWithinAerosolClouds() {
        Pedestrian pedestrian = testUpdatePedestriansExposureToAerosolClouds(false);

        assertTrue(pedestrian.getDegreeOfExposure() > 0);
    }

    @Test
    public void testUpdatePedestriansExposureOutsideAerosolClouds() {
        Pedestrian pedestrian = testUpdatePedestriansExposureToAerosolClouds(true);

        assertEquals(0, pedestrian.getDegreeOfExposure(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testExecuteDropletEmissionEvents() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        double simEndTime = attrModel.getPedestrianRespiratoryCyclePeriod() + 1 / attrModel.getDropletsEmissionFrequency();

        attrModel.setDropletsLifeTime(simEndTime);

        Pedestrian pedestrian = createPedestrian();
        pedestrian.setId(1);
        pedestrian.setInfectious(true);
        VPoint position = new VPoint(1, 1);
        pedestrian.setPosition(position);
        topography.addElement(pedestrian);

        Random random = new Random(0);

        //TODO: could be tested for several walking directions not only one random walking direction
        Vector2D walkingDirection = new Vector2D(random.nextDouble(), random.nextDouble());
        walkingDirection = walkingDirection.normalize(AirTransmissionModel.MIN_PED_STEP_LENGTH);

        for (double simTimeInSec = simStartTime; simTimeInSec < simEndTime; simTimeInSec += airTransmissionModel.simTimeStepLength) {
            position = position.add(walkingDirection);
            pedestrian.setPosition(position);
            airTransmissionModel.executeDropletEmissionEvents(simTimeInSec);
        }

        Vector2D normDropletsDirection = topography.getDroplets().stream().findFirst().get().getDirection().normalize(1);
        Vector2D normExpectedDirection = walkingDirection.normalize(1);

        assertEquals(normExpectedDirection.x, normDropletsDirection.x, ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(normExpectedDirection.y, normDropletsDirection.y, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void testDeleteExpiredDropletsBeforeLifeTimeReached() {
        double lifeTime = 3;
        double simTimeInSec = simStartTime + lifeTime * 0.9; // any value < simStartTime + lifeTime
        initTestDeleteExpiredDroplets(lifeTime);

        airTransmissionModel.deleteExpiredDroplets(simTimeInSec);

        assertFalse(topography.getDroplets().isEmpty());
    }

    @Test
    public void testDeleteExpiredDropletsAfterLifeTimeReached() {
        double lifeTime = 3;
        double simTimeInSec = simStartTime + lifeTime * 1.1; // any value > simStartTime + lifeTime
        initTestDeleteExpiredDroplets(lifeTime);

        airTransmissionModel.deleteExpiredDroplets(simTimeInSec);

        assertEquals(0, topography.getDroplets().size());
    }

    public void initTestDeleteExpiredDroplets(double lifeTime) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        setDropletsActive(true);
        attrModel.setDropletsLifeTime(lifeTime);
        createDroplets(airTransmissionModel);
    }

    @Test
    public void testUpdatePedestriansExposureWithinDroplets() {
        Pedestrian pedestrian = testUpdatePedestriansExposureToDroplets(false);

        assertTrue(pedestrian.getDegreeOfExposure() > 0);
    }

    @Test
    public void testUpdatePedestriansExposureOutsideDroplets() {
        Pedestrian pedestrian = testUpdatePedestriansExposureToDroplets(true);

        assertEquals(0, pedestrian.getDegreeOfExposure());
    }

    private Pedestrian testUpdatePedestriansExposureToDroplets(boolean pedestrianOutsideDroplets) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        createDroplets(airTransmissionModel);
        Droplets droplets = airTransmissionModel.topography.getDroplets().stream().findFirst().get();
        Vector2D dropletsDirection = droplets.getDirection();
        VPoint dropletsOrigin = droplets.getOrigin();

        double simTimeStepLength = airTransmissionModel.simTimeStepLength;
        double simEndTime = 1 / attrModel.getDropletsEmissionFrequency() + 2 * attrModel.getPedestrianRespiratoryCyclePeriod();

        Pedestrian pedestrian = createPedestrian();
        VPoint position = dropletsOrigin.add(dropletsDirection.normalize(attrModel.getDropletsDistanceOfSpread() * 0.5));
        if (pedestrianOutsideDroplets) {
            position = dropletsOrigin.add(dropletsDirection.normalize(attrModel.getDropletsDistanceOfSpread() * 1.5));
        }
        pedestrian.setPosition(position);
        topography.addElement(pedestrian);

        for (double simTimeInSec = simStartTime; simTimeInSec < simEndTime; simTimeInSec += simTimeStepLength) {
            airTransmissionModel.updatePedestriansExposureToDroplets();
            airTransmissionModel.updatePedestriansHealthStatus(simTimeInSec);
        }

        return pedestrian;
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
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(1,
                attrModel.getAerosolCloudInitialRadius(),
                new VPoint(5, 5), // position is not important for only a few tests
                simStartTime,
                attrModel.getAerosolCloudInitialPathogenLoad()));
        airTransmissionModel.topography.addAerosolCloud(aerosolCloud);
    }

    private void createDroplets(AirTransmissionModel airTransmissionModel) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        Droplets droplets = new Droplets(new AttributesDroplets(1,
                simStartTime,
                attrModel.getDropletsPathogenLoad(),
                new VPoint(5, 5),
                new Vector2D(1, 1),
                attrModel.getDropletsDistanceOfSpread(),
                attrModel.getDropletsAngleOfSpreadInDeg()));
        airTransmissionModel.topography.addDroplets(droplets);
    }

    private void setAerosolCloudsActive(boolean active) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        attrModel.setAerosolCloudsActive(active);
    }

    private void setDropletsActive(boolean active) {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        
        attrModel.setDropletsActive(active);
    }

    @Test
    public void testUpdateAerosolCloudsLocationStepCounterAndCorrectShift() {
        // Test that clouds only move every MOVE_EVERY_N_STEPS steps
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        int dx = 1;
        int dy = 1;
        double[][] xVelocity = new double[][]{{0,0,0},{0,dx,0},{0,0,0}};
        double[][] yVelocity = new double[][]{{0,0,0},{0,dy,0},{0,0,0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        VPoint initialPosition = cloud.getCenter();

        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS - 1; i++)
            airTransmissionModel.updateAerosolCloudsLocation(0);
            assertEquals(initialPosition, cloud.getCenter());

        airTransmissionModel.updateAerosolCloudsLocation(0);
        assertEquals(initialPosition.x + dx * airTransmissionModel.simTimeStepLength * airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS, cloud.getCenter().x, "Aerosol cloud should be shifted correctly");
        assertEquals(initialPosition.y + dy * airTransmissionModel.simTimeStepLength * airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS, cloud.getCenter().y, "Aerosol cloud should be shifted correctly");
    }

    private void createAirflow(AirTransmissionModel airTransmissionModel, double[][] xVelocity, double[][] yVelocity) {
        VadereContext ctx = new VadereContext();
        ctx.put("scenarioPath", "test");
        VadereContext.add(topography.getContextId(), ctx);

        AirFlow airFlow = new AirFlow("test", "test_hash", 0.0, 0.0, 10, 10);
        airFlow.setX_velocity(xVelocity);
        airFlow.setY_velocity(yVelocity);
        airFlow.setRectangularGridCellSize(Double.POSITIVE_INFINITY);

        topography.setAirFlow(airFlow);
    }

    @Test
    public void testUpdateAerosolCloudsLocationWithDifferentSimTimeStepLength() {
        attributesList = new ArrayList<>();
        AttributesAirTransmissionModel attr = new AttributesAirTransmissionModel();
        attributesList.add(attr);
        airTransmissionModel = new AirTransmissionModel();
        topography = new Topography();
        topography.setContextId("testId");

        // Create a simple airflow field with constant x-velocity of 1.0
        AirFlow airFlow = new AirFlow("test", "test_hash", 0.0, 0.0, 10, 10);
        double[][] xVelocity = new double[][]{{1.0}};
        double[][] yVelocity = new double[][]{{0.0}};
        airFlow.setX_velocity(xVelocity);
        airFlow.setY_velocity(yVelocity);
        airFlow.setRectangularGridCellSize(Double.POSITIVE_INFINITY);
        topography.setAirFlow(airFlow);

        // Initialize model with time step 0.4
        ctx = new VadereContext();
        ctx.put(AirTransmissionModel.simStepLength, 0.4);
        ctx.put("scenarioPath", "test");
        VadereContext.add(topography.getContextId(), ctx);
        initializeTransmissionModel();

        // Create an aerosol cloud at position (0,0)
        AttributesAerosolCloud attributes = new AttributesAerosolCloud(1, 1.0, new VPoint(0.0, 0.0), 0.0, 10000.0);
        AerosolCloud cloud = new AerosolCloud(attributes);
        topography.addAerosolCloud(cloud);

        // Run two steps with simTimeStepLength 0.4
        airTransmissionModel.update(0.4);
        airTransmissionModel.update(0.8);
        double xPos1 = topography.getAerosolClouds().get(0).getCenter().x;
        double yPos1 = topography.getAerosolClouds().get(0).getCenter().y;

        // Reset and run one time step with simTimeStepLength 0.8
        topography.getAerosolClouds().clear();
        attributes = new AttributesAerosolCloud(1, 1.0, new VPoint(0.0, 0.0), 0.0, 10000.0);
        cloud = new AerosolCloud(attributes);
        topography.addAerosolCloud(cloud);
        ctx.put(AirTransmissionModel.simStepLength, 0.8);
        airTransmissionModel.update(0.8);
        double xPos2 = topography.getAerosolClouds().get(0).getCenter().x;
        double yPos2 = topography.getAerosolClouds().get(0).getCenter().y;

        // The positions should be equal
        assertEquals(xPos1, xPos2, 1e-10);
        assertEquals(yPos1, yPos2, 1e-10);
    }

    @Test
    public void testRemoveStuckAerosolCloudsNotStuck() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();

        // Cloud is not in any obstacle
        airTransmissionModel.removeStuckAerosolClouds();
        assertEquals(1, topography.getAerosolClouds().size(), "Aerosol cloud should not be removed yet");
        assertFalse(airTransmissionModel.aerosolCounter.containsKey(cloud), "Aerosol counter should not contain cloud");
    }

    @Test
    public void testRemoveStuckAerosolCloudsStuckButNotLongEnough() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        double[][] xVelocity = new double[][]{{0}};
        double[][] yVelocity = new double[][]{{0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        Obstacle blockingObstacle = createBlockingObstacleAtPosition(cloud.getCenter());
        topography.addObstacle(blockingObstacle);

        airTransmissionModel.removeStuckAerosolClouds();
        assertEquals(1, topography.getAerosolClouds().size(), "Aerosol cloud should not be removed yet");
        assertEquals(1, (int) airTransmissionModel.aerosolCounter.get(cloud), "Aerosol counter should contain cloud");
    }

    @Test
    public void testRemoveStuckAerosolCloudsStuckAndRemoved() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        double[][] xVelocity = new double[][]{{0}};
        double[][] yVelocity = new double[][]{{0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        Obstacle obstacle = createBlockingObstacleAtPosition(cloud.getCenter());
        topography.addObstacle(obstacle);

        for (int i = 0; i <= airTransmissionModel.STUCK_MAX; i++) {
            airTransmissionModel.removeStuckAerosolClouds();
        }

        assertTrue(topography.getAerosolClouds().isEmpty(), "Cloud should be removed after being stuck for more than STUCK_MAX steps");
        assertFalse(airTransmissionModel.aerosolCounter.containsKey(cloud), "Counter should be removed after cloud removal");
    }

    @Test
    public void testRemoveStuckAerosolCloudsBecomesUnstuck() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        double[][] xVelocity = new double[][]{{0}};
        double[][] yVelocity = new double[][]{{0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        Obstacle blockingObstacle = createBlockingObstacleAtPosition(cloud.getCenter());
        topography.addObstacle(blockingObstacle);

        // First check - should increment counter
        airTransmissionModel.removeStuckAerosolClouds();
        assertEquals(1, (int) airTransmissionModel.aerosolCounter.get(cloud), "Counter should be 1 after first stuck detection");

        // Remove obstacle - cloud becomes unstuck
        topography.getObstacles().remove(blockingObstacle);
        airTransmissionModel.removeStuckAerosolClouds();

        assertFalse(airTransmissionModel.aerosolCounter.containsKey(cloud), "Counter should be removed when cloud becomes unstuck");
        assertEquals(1, topography.getAerosolClouds().size(), "Cloud should remain when it becomes unstuck");
    }

    private Obstacle createBlockingObstacleAtPosition(VPoint position) {
        VShape shape = new VRectangle(position.x - 0.5, position.y - 0.5, 1, 1);
        Obstacle obstacle = new Obstacle(new AttributesObstacle(1, shape));
        topography.getAirFlow().setBlockingObstaclesIDs(List.of(1));
        return obstacle;
    }

    @Test
    public void testUpdateAerosolCloudsLocationNoAirFlow() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        topography.setAirFlow(null);

        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        VPoint initialPosition = cloud.getCenter();

        airTransmissionModel.updateAerosolCloudsLocation(0);

        assertEquals(initialPosition, cloud.getCenter(),
                "Cloud should not move when airFlow is null");
    }

    @Test
    public void testUpdateAerosolCloudsLocationZeroVelocity() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        double[][] xVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        double[][] yVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        VPoint initialPosition = cloud.getCenter();

        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(0);
        }

        assertEquals(initialPosition.x, cloud.getCenter().x, ALLOWED_DOUBLE_TOLERANCE,
                "Cloud x should not change with zero velocity");
        assertEquals(initialPosition.y, cloud.getCenter().y, ALLOWED_DOUBLE_TOLERANCE,
                "Cloud y should not change with zero velocity");
    }

    @Test
    public void testUpdateAerosolCloudsLocationRemovesCloudLeavingBounds() {
        setAerosolCloudsActive(true);

        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();
        AerosolCloud edgeCloud = new AerosolCloud(new AttributesAerosolCloud(99,
                attrModel.getAerosolCloudInitialRadius(),
                new VPoint(9.9, 5),
                simStartTime,
                attrModel.getAerosolCloudInitialPathogenLoad()));
        topography.addAerosolCloud(edgeCloud);

        double[][] xVelocity = new double[][]{{100, 100, 100}, {100, 100, 100}, {100, 100, 100}};
        double[][] yVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(0);
        }

        boolean cloudRemoved = topography.getAerosolClouds().stream().noneMatch(c -> c.getId() == 99);
        assertTrue(cloudRemoved, "Cloud pushed outside airflow bounds should be removed");
    }

    @Test
    public void testUpdateAerosolCloudsLocationMultipleClouds() {
        setAerosolCloudsActive(true);
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();

        AerosolCloud cloud1 = new AerosolCloud(new AttributesAerosolCloud(1,
                attrModel.getAerosolCloudInitialRadius(), new VPoint(3, 3), simStartTime,
                attrModel.getAerosolCloudInitialPathogenLoad()));
        AerosolCloud cloud2 = new AerosolCloud(new AttributesAerosolCloud(2,
                attrModel.getAerosolCloudInitialRadius(), new VPoint(7, 7), simStartTime,
                attrModel.getAerosolCloudInitialPathogenLoad()));
        topography.addAerosolCloud(cloud1);
        topography.addAerosolCloud(cloud2);

        double[][] xVelocity = new double[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
        double[][] yVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        VPoint initial1 = cloud1.getCenter();
        VPoint initial2 = cloud2.getCenter();

        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(0);
        }

        double expectedShift = 1 * airTransmissionModel.simTimeStepLength * airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS;
        assertEquals(initial1.x + expectedShift, cloud1.getCenter().x, ALLOWED_DOUBLE_TOLERANCE,
                "Cloud 1 should be shifted correctly");
        assertEquals(initial2.x + expectedShift, cloud2.getCenter().x, ALLOWED_DOUBLE_TOLERANCE,
                "Cloud 2 should be shifted correctly");
    }

    @Test
    public void testRemoveStuckAerosolCloudsSkipsWhenStepCounterNonZero() {
        // removeStuckAerosolClouds should only run when airFlowStepCounter == 0
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        double[][] xVelocity = new double[][]{{0}};
        double[][] yVelocity = new double[][]{{0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        Obstacle blockingObstacle = createBlockingObstacleAtPosition(cloud.getCenter());
        topography.addObstacle(blockingObstacle);

        airTransmissionModel.updateAerosolCloudsLocation(0);
        airTransmissionModel.removeStuckAerosolClouds();
        assertFalse(airTransmissionModel.aerosolCounter.containsKey(cloud),
                "removeStuckAerosolClouds should skip when airFlowStepCounter != 0");
    }

    @Test
    public void testRemoveStuckAerosolCloudsNonBlockingObstacleIgnored() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        double[][] xVelocity = new double[][]{{0}};
        double[][] yVelocity = new double[][]{{0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        VShape shape = new VRectangle(cloud.getCenter().x - 0.5, cloud.getCenter().y - 0.5, 1, 1);
        Obstacle nonBlockingObstacle = new Obstacle(new AttributesObstacle(99, shape));
        topography.addObstacle(nonBlockingObstacle);
        topography.getAirFlow().setBlockingObstaclesIDs(List.of(1));

        for (int i = 0; i <= airTransmissionModel.STUCK_MAX + 1; i++) {
            airTransmissionModel.removeStuckAerosolClouds();
        }

        assertEquals(1, topography.getAerosolClouds().size(),
                "Cloud should not be removed by non-blocking obstacle");
    }

    @Test
    public void testUpdateAerosolCloudsLocationResetCounterAfterMove() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);
        double[][] xVelocity = new double[][]{{0, 0, 0}, {0, 1, 0}, {0, 0, 0}};
        double[][] yVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        createAirflow(airTransmissionModel, xVelocity, yVelocity);

        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(0);
        }
        VPoint positionAfterFirstMove = cloud.getCenter();

        // Next MOVE_AEROSOLS_EVERY_N_STEPS-1 calls should NOT move the cloud
        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS - 1; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(0);
        }
        assertEquals(positionAfterFirstMove, cloud.getCenter(),
                "Cloud should not have moved before the counter resets again");

        airTransmissionModel.updateAerosolCloudsLocation(0);
        assertNotEquals(positionAfterFirstMove.x, cloud.getCenter().x,
                "Cloud should have moved after the counter resets and triggers again");
    }

    @Test
    public void testPeriodicAirflowDisablesCloudMovement() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);

        AirFlow airFlow = new AirFlow("test", "test_hash", 0.0, 0.0, 10, 10);
        double[][] xVelocity = new double[][]{{5, 5, 5}, {5, 5, 5}, {5, 5, 5}};
        double[][] yVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        airFlow.setX_velocity(xVelocity);
        airFlow.setY_velocity(yVelocity);
        airFlow.setRectangularGridCellSize(Double.POSITIVE_INFINITY);
        airFlow.setPeriod(1.0, 1.0);
        topography.setAirFlow(airFlow);

        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        VPoint initialPosition = cloud.getCenter();

        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(0.5);
        }

        assertEquals(initialPosition.x, cloud.getCenter().x, ALLOWED_DOUBLE_TOLERANCE,
                "Cloud should not move during off period of periodic airflow");
    }

    @Test
    public void testPeriodicAirflowEnablesCloudMovementDuringOnPeriod() {
        setAerosolCloudsActive(true);
        createAerosolCloud(airTransmissionModel);

        AirFlow airFlow = new AirFlow("test", "test_hash", 0.0, 0.0, 10, 10);
        double[][] xVelocity = new double[][]{{5, 5, 5}, {5, 5, 5}, {5, 5, 5}};
        double[][] yVelocity = new double[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        airFlow.setX_velocity(xVelocity);
        airFlow.setY_velocity(yVelocity);
        airFlow.setRectangularGridCellSize(Double.POSITIVE_INFINITY);
        airFlow.setPeriod(1.0, 1.0);
        topography.setAirFlow(airFlow);

        AerosolCloud cloud = topography.getAerosolClouds().stream().findFirst().get();
        VPoint initialPosition = cloud.getCenter();

        for (int i = 0; i < airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS; i++) {
            airTransmissionModel.updateAerosolCloudsLocation(1.5);
        }

        double expectedShift = 5 * airTransmissionModel.simTimeStepLength * airTransmissionModel.MOVE_AEROSOLS_EVERY_N_STEPS;
        assertEquals(initialPosition.x + expectedShift, cloud.getCenter().x, ALLOWED_DOUBLE_TOLERANCE,
                "Cloud should move during on period of periodic airflow");
    }
}