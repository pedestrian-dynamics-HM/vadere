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
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.vadere.simulator.models.infection.AirTransmissionModel.*;
import static org.vadere.state.scenario.AerosolCloud.createAerosolCloudShape;

public class AirTransmissionModelTest {
    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;
    static final double simTimeStepLength = 0.4;

    List<Attributes> attributeList;
    AirTransmissionModel airTransmissionModel;
    Topography topography;
    VadereContext ctx;

    // member variables defining aerosol cloud shapes
    double radius = 1;
    VPoint center = new VPoint(0, 0);
    VShape circle;


    // will run before each test to setup test environment
    @Before
    public void setUp() {
        attributeList = getAttributeList();
        airTransmissionModel = new AirTransmissionModel();
        topography = new Topography();
        topography.setContextId("testId");

        ctx = new VadereContext();
        ctx.put(AirTransmissionModel.simStepLength, simTimeStepLength);
        VadereContext.add(topography.getContextId(), ctx);

        circle = createAerosolCloudShape(center, radius);
    }

    // cleanup test environment
    @After
    public void after() {
        attributeList.clear();
    }

    @Test
    public void testInitializeOk() {
        // initialize must find the AttributesInfectionModel from the  attributeList
        airTransmissionModel.initialize(attributeList, new Domain(topography),null,null);

        AttributesAirTransmissionModel attributesAirTransmissionModel = airTransmissionModel.getAttributesAirTransmissionModel();

        Assert.assertEquals(attributeList.get(0), attributesAirTransmissionModel);
    }

    public List<Attributes> getAttributeList() {
        ArrayList<Attributes> attrList = new ArrayList<>();
        var att = new AttributesAirTransmissionModel();

        attrList.add(att);

        return attrList;
    }

    private void createPedestrian(Topography topography, VPoint pedPosition, int pedId, int targetId, boolean isInfectious) {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), new Random(1));
        pedestrian.setPosition(pedPosition);
        pedestrian.setInfectious(isInfectious);
        pedestrian.setId(pedId);

        LinkedList<Integer> targetsPedestrian = new LinkedList<>();
        targetsPedestrian.add(targetId);
        pedestrian.setTargets(targetsPedestrian);

        topography.addElement(pedestrian);
    }

    private void createTarget(Topography topography, VPoint targetCenter, int id) {
        Target target = new Target(new AttributesTarget());
        target.setShape(new VCircle(targetCenter, 0.5));
        target.getAttributes().setId(id);

        topography.addTarget(target);
    }

    @Test
    public void testUpdateNumberOfGeneratedAerosolClouds() {

        airTransmissionModel.initialize(attributeList, new Domain(topography),null,null);

        createPedestrian(topography, new VPoint(10, 10), 1, -1, true);

        double simTimeInSec = 0.0;
        double simTimeStepLength = 0.4;
        double simEndTime = 100;

        while(simTimeInSec <= simEndTime) {
            airTransmissionModel.update(simTimeInSec);
            simTimeInSec += simTimeStepLength;
        }

        int expectedNumberOfAerosolClouds = (int) Math.floor(simEndTime / airTransmissionModel.getAttributesAirTransmissionModel().getPedestrianRespiratoryCyclePeriod());
        int actualNumberOfAerosolClouds = topography.getAerosolClouds().size();

        assertEquals(actualNumberOfAerosolClouds, expectedNumberOfAerosolClouds);
    }

    @Test
    public void throwIfPedestrianInsideAerosolCloudNotDetected() {
        Topography topography = new Topography();
        createPedestrian(topography, new VPoint(10, 10), 1, -1, false);
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(Attributes.ID_NOT_SET, 1, new VPoint(10, 10), 0.0,  0));
        topography.addAerosolCloud(aerosolCloud);
        boolean inAerosolCloud = isPedestrianInAerosolCloud(aerosolCloud, topography.getPedestrianDynamicElements().getElement(1));

        assertTrue(inAerosolCloud);
    }

    @Test
    public void throwIfPedestrianOutsideAerosolCloudConsideredInside() {
        Topography topography = new Topography();
        createPedestrian(topography, new VPoint(5, 5), 1, -1, false);
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(Attributes.ID_NOT_SET, 1, new VPoint(10, 10), 0.0,  0));
        topography.addAerosolCloud(aerosolCloud);
        boolean inAerosolCloud = isPedestrianInAerosolCloud(aerosolCloud, topography.getPedestrianDynamicElements().getElement(1));

        assertFalse(inAerosolCloud);
    }

    @Test
    public void testGetPedestriansInsideAerosolCloud() {
        Topography topography = new Topography();
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(Attributes.ID_NOT_SET, 1, new VPoint(10, 10), 0.0,  0));
        aerosolCloud.setId(1);
        topography.addAerosolCloud(aerosolCloud);
        // pedestrians outside cloud
        createPedestrian(topography, new VPoint(12, 12), 2, -1, false);
        createPedestrian(topography, new VPoint(1, 1), 3, -1,false);
        // pedestrians inside cloud
        createPedestrian(topography, new VPoint(10, 10), 4, -1, false);
        createPedestrian(topography, new VPoint(10.5, 10.5), 5, -1, false);

        Collection<Pedestrian> expectedPedestriansInAerosolCloud = new LinkedList<>();
        expectedPedestriansInAerosolCloud.add(topography.getPedestrianDynamicElements().getElement(4));
        expectedPedestriansInAerosolCloud.add(topography.getPedestrianDynamicElements().getElement(5));

        Collection<Pedestrian> actualPedestriansInAerosolCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);
        assertEquals(actualPedestriansInAerosolCloud, expectedPedestriansInAerosolCloud);
    }
}