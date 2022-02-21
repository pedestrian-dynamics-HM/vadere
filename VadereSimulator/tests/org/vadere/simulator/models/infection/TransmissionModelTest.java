package org.vadere.simulator.models.infection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesTransmissionModel;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.vadere.simulator.models.infection.TransmissionModel.*;
import static org.vadere.state.scenario.AerosolCloud.createAerosolCloudShape;

public class TransmissionModelTest {
    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;
    static final double simTimeStepLength = 0.4;

    List<Attributes> attributeList;
    TransmissionModel transmissionModel;
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
        transmissionModel = new TransmissionModel();
        topography = new Topography();
        topography.setContextId("testId");

        ctx = new VadereContext();
        ctx.put(TransmissionModel.simStepLength, simTimeStepLength);
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
        transmissionModel.initialize(attributeList, new Domain(topography),null,null);

        AttributesTransmissionModel attributesTransmissionModel = transmissionModel.getAttributesTransmissionModel();

        Assert.assertEquals(attributeList.get(0), attributesTransmissionModel);
    }

    public List<Attributes> getAttributeList() {
        ArrayList<Attributes> attrList = new ArrayList<>();
        var att = new AttributesTransmissionModel();

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

        transmissionModel.initialize(attributeList, new Domain(topography),null,null);

        createPedestrian(topography, new VPoint(10, 10), 1, -1, true);

        double simTimeInSec = 0.0;
        double simTimeStepLength = 0.4;
        double simEndTime = 100;

        while(simTimeInSec <= simEndTime) {
            transmissionModel.update(simTimeInSec);
            simTimeInSec += simTimeStepLength;
        }

        int expectedNumberOfAerosolClouds = (int) Math.floor(simEndTime / transmissionModel.getAttributesTransmissionModel().getPedestrianRespiratoryCyclePeriod());
        int actualNumberOfAerosolClouds = topography.getAerosolClouds().size();

        assertEquals(actualNumberOfAerosolClouds, expectedNumberOfAerosolClouds);
    }

    @Test
    public void throwIfPedestrianInsideAerosolCloudNotDetected() {
        Topography topography = new Topography();
        createPedestrian(topography, new VPoint(10, 10), 1, -1, false);
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(new VCircle(new VPoint(10, 10), 1), 0.0));
        topography.addAerosolCloud(aerosolCloud);
        boolean inAerosolCloud = isPedestrianInAerosolCloud(aerosolCloud, topography.getPedestrianDynamicElements().getElement(1));

        assertTrue(inAerosolCloud);
    }

    @Test
    public void throwIfPedestrianOutsideAerosolCloudConsideredInside() {
        Topography topography = new Topography();
        createPedestrian(topography, new VPoint(5, 5), 1, -1, false);
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(new VCircle(new VPoint(10, 10), 1), 0.0));
        topography.addAerosolCloud(aerosolCloud);
        boolean inAerosolCloud = isPedestrianInAerosolCloud(aerosolCloud, topography.getPedestrianDynamicElements().getElement(1));

        assertFalse(inAerosolCloud);
    }

    @Test
    public void testGetPedestriansInsideAerosolCloud() {
        Topography topography = new Topography();
        AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(new VCircle(new VPoint(10, 10), 1), 0.0));
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