package org.vadere.simulator.models.infection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel;
import org.vadere.state.attributes.models.infection.AttributesExtendedAirTransmissionModel;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.ExtendedAirTransmissionModelHealthStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtendedAirTransmissionModelTest {

    private static final double ALLOWED_DOUBLE_TOLERANCE = 10e-6;
    private static final double SIM_TIME_STEP_LENGTH = 0.4;

    List<Attributes> attributesList;
    ExtendedAirTransmissionModel airTransmissionModel;
    Topography topography;
    VadereContext ctx;
    Random rdm;
    double simStartTime;


    @BeforeEach
    public void setUp() {
        attributesList = new ArrayList<>();
        attributesList.add(new AttributesExtendedAirTransmissionModel());
        airTransmissionModel = new ExtendedAirTransmissionModel();
        topography = new Topography();
        topography.setContextId("testId");
        rdm = new Random(0);
        ctx = new VadereContext();
        ctx.put(AirTransmissionModel.simStepLength, SIM_TIME_STEP_LENGTH);
        VadereContext.add(topography.getContextId(), ctx);
        simStartTime = 0.0;

        airTransmissionModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    @AfterEach
    public void after() {
        attributesList.clear();
    }

    @Test
    public void testInitializeAttributesList() {
        assertEquals(attributesList.get(0), airTransmissionModel.getAttributes());
    }

    @Test
    public void testInitializeGetsSimTimeStepLength() {
        assertEquals(ctx.get(AirTransmissionModel.simStepLength), airTransmissionModel.simTimeStepLength);
    }

    @Test
    public void testUpdateAerosolCloudLocation() {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) airTransmissionModel.getAttributes();
        attrModel.setAerosolCloudHalfLife(SIM_TIME_STEP_LENGTH);

        setAerosolCloudsActive();
        createAerosolCloud(airTransmissionModel);

        int nSimSteps = 10;

        AerosolCloud cloud = topography.getAerosolClouds().get(0);
        VPoint lastLocation = cloud.getCenter();

        for (int i = 0; i < nSimSteps; i++) {
            airTransmissionModel.updateAerosolCloudsLocation();
            VPoint newLocation = cloud.getCenter();

            assertEquals(attrModel.getAerosolCloudWindSpeed() * SIM_TIME_STEP_LENGTH / 100, newLocation.distance(lastLocation), ALLOWED_DOUBLE_TOLERANCE);
            double angle = Math.acos(newLocation.subtract(lastLocation).scalarProduct(new VPoint(1, 0)) / newLocation.subtract(lastLocation).distance(0, 0));
            assertEquals(attrModel.getAerosolCloudWindDirection(), angle, ALLOWED_DOUBLE_TOLERANCE);
            lastLocation = newLocation;
        }
    }

    @Test
    public void testUpdateCreateAerosolCloudsPathogenLoadSneezing() {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) airTransmissionModel.getAttributes();
        setAerosolCloudsActive();
        int sneezingEveryNthTime = 3;
        Pedestrian ped = createPedestrian(false, false, true, 0, sneezingEveryNthTime);
        double simTimeEnd = 100 * SIM_TIME_STEP_LENGTH;
        initUpdate(simTimeEnd, ped);
        int cloudCount = topography.getAerosolClouds().size();

        for (double simTimeInSec = simStartTime; simTimeInSec < simTimeEnd; simTimeInSec += SIM_TIME_STEP_LENGTH) {
            airTransmissionModel.update(simTimeInSec);
            if (cloudCount < topography.getAerosolClouds().size()) {
                cloudCount++;
                if (cloudCount % sneezingEveryNthTime == 0) {
                    assertEquals(topography.getAerosolClouds().get(cloudCount-1).getCurrentPathogenLoad(),
                            attrModel.getAerosolCloudInitialPathogenLoad() * attrModel.getAerosolCloudPathogenLoadMultiplierSneezing());
                } else {
                    assertEquals(topography.getAerosolClouds().get(cloudCount-1).getCurrentPathogenLoad(), attrModel.getAerosolCloudInitialPathogenLoad());
                }
            }
        }
    }

    @Test
    public void testUpdateCreateAerosolCloudsPathogenLoadCoughing() {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) airTransmissionModel.getAttributes();
        setAerosolCloudsActive();
        int coughingEveryNthTime = 3;
        Pedestrian ped = createPedestrian(false, true, false, coughingEveryNthTime, 0);
        double simTimeEnd = 100 * SIM_TIME_STEP_LENGTH;
        initUpdate(simTimeEnd, ped);
        int cloudCount = topography.getAerosolClouds().size();

        for (double simTimeInSec = simStartTime; simTimeInSec < simTimeEnd; simTimeInSec += SIM_TIME_STEP_LENGTH) {
            airTransmissionModel.update(simTimeInSec);
            if (cloudCount < topography.getAerosolClouds().size()) {
                cloudCount++;
                if (cloudCount % coughingEveryNthTime == 0) {
                    assertEquals(topography.getAerosolClouds().get(cloudCount-1).getCurrentPathogenLoad(),
                            attrModel.getAerosolCloudInitialPathogenLoad() * attrModel.getAerosolCloudPathogenLoadMultiplierCoughing());
                } else {
                    assertEquals(topography.getAerosolClouds().get(cloudCount-1).getCurrentPathogenLoad(), attrModel.getAerosolCloudInitialPathogenLoad());
                }
            }
        }
    }

    @Test
    public void testUpdateCreateAerosolCloudsPathogenLoadTalking() {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) airTransmissionModel.getAttributes();
        setAerosolCloudsActive();
        Pedestrian ped = createPedestrian(true, false, false, 0, 0);
        double simTimeEnd = 100 * SIM_TIME_STEP_LENGTH;
        initUpdate(simTimeEnd, ped);
        int cloudCount = topography.getAerosolClouds().size();

        for (double simTimeInSec = simStartTime; simTimeInSec < simTimeEnd; simTimeInSec += SIM_TIME_STEP_LENGTH) {
            airTransmissionModel.update(simTimeInSec);
            if (cloudCount < topography.getAerosolClouds().size()) {
                cloudCount++;
                assertEquals(topography.getAerosolClouds().get(cloudCount-1).getCurrentPathogenLoad(),
                        attrModel.getAerosolCloudInitialPathogenLoad() * attrModel.getAerosolCloudPathogenLoadMultiplierTalking());
            }
        }
    }

    private void setAerosolCloudsActive() {
        AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) airTransmissionModel.getAttributes();

        attrModel.setAerosolCloudsActive(true);
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

    private Pedestrian createPedestrian(boolean isTalking, boolean isCoughing, boolean isSneezing, int coughingN, int sneezingN) {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), rdm);
        ExtendedAirTransmissionModelHealthStatus healthStatus = new ExtendedAirTransmissionModelHealthStatus();
        healthStatus.setTalking(isTalking);
        healthStatus.setCoughing(isCoughing);
        healthStatus.setCoughingEveryNthBreath(coughingN);
        healthStatus.setSneezing(isSneezing);
        healthStatus.setSneezingEveryNthBreath(sneezingN);
        pedestrian.setHealthStatus(healthStatus);
        pedestrian.setInfectious(true);
        return pedestrian;
    }

    private void initUpdate(double simTime, Pedestrian pedestrian) {
        AttributesExtendedAirTransmissionModel attrModel = (AttributesExtendedAirTransmissionModel) airTransmissionModel.getAttributes();

        // this is only necessary to assure that aerosol clouds or droplets remain until the simEndTime once they are inserted into the topography
        attrModel.setAerosolCloudHalfLife(simTime + 1);
        attrModel.setDropletsLifeTime(simTime + 1);

        VPoint pos = new VPoint(2,2);
        pedestrian.setPosition(pos);
        pedestrian.setId(1);
        topography.addElement(pedestrian);
    }
}