package org.vadere.simulator.models.sir;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributeSIR;
import org.vadere.state.attributes.models.AttributesInfectionModel;
import org.vadere.state.attributes.models.InfectionModelSourceParameters;
import org.vadere.state.health.InfectionStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfectionModelTest extends TestCase {
    public static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    List<Attributes> attributeList;
    InfectionModel infectionModel;

    public double expectedLastUpdateTime = 1.4;

    // will run before each test to setup test environment
    @Before
    public void setUp() {
        attributeList = getAttributeList();
        infectionModel = new InfectionModel();
    }

    // cleanup test environment
    @After
    public void after() {
        attributeList.clear();
    }

    @Test
    public void testInitializeOk() {
        // initialize must find the AttributesInfectionModel from the  attributeList
        infectionModel.initialize(attributeList,null,null,null);

        AttributesInfectionModel attributesInfectionModel = infectionModel.getAttributesInfectionModel();
        ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters = attributesInfectionModel.getInfectionModelSourceParameters();

        assertEquals(attributeList.get(0), attributesInfectionModel);

        assertEquals(expectedLastUpdateTime ,attributesInfectionModel.getInfectionModelLastUpdateTime(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(4, attributesInfectionModel.getInfectionModelUpdateStepLength(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(-1, infectionModelSourceParameters.get(0).getSourceId());
        assertEquals(InfectionStatus.SUSCEPTIBLE, infectionModelSourceParameters.get(0).getInfectionStatus());
        assertEquals(1, attributesInfectionModel.getPedestrianPathogenEmissionCapacity(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(0.0003, attributesInfectionModel.getPedestrianPathogenAbsorptionRate(), 10e-7);

    }

    public List<Attributes> getAttributeList() {
        ArrayList<Attributes> attrList = new ArrayList<>();
        var att = new AttributesInfectionModel();
        att.setInfectionModelLastUpdateTime(expectedLastUpdateTime);
        attrList.add(att);

        return attrList;
    }
}