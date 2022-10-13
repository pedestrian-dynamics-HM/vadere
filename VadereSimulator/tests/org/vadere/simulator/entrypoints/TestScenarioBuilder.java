package org.vadere.simulator.entrypoints;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestScenarioBuilder {

    private String jsonScenario;

    @Before
   public void init() throws URISyntaxException, IOException {
        jsonScenario  =  IOUtils.readTextFile(
                Paths.get(getClass().getResource("/org/vadere/simulator/entrypoints/test.scenario").toURI()));
	}

    @Test
    public void testChangeAttributes() throws IOException {
        // 1. read the scenario from a string
        Scenario scenario = IOVadere.fromJson(jsonScenario);

        // 2. change the source id = -1 to source id = 1
        assertTrue(scenario.getTopography().getSources().get(0).getId() == -1);
        ScenarioBuilder builder = new ScenarioBuilder(scenario);
        builder.setSourceField("id", -1, 1);
        assertTrue(scenario.getTopography().getSources().get(0).getId() == -1);
        scenario = builder.build();
        assertTrue(scenario.getTopography().getSources().get(0).getId() == 1);

        // 3. change pedestrianAttributes / radius
        builder.setAttributesField("radius", 30.0, AttributesAgent.class);
        scenario = builder.build();
        assertTrue(scenario.getAttributesPedestrian().getRadius() == 30.0);

        // 4. change topographyAttributes / radius
        builder.setAttributesField("bounds", new VRectangle(10, 10, 100, 100), AttributesTopography.class);
        scenario = builder.build();
        assertTrue(scenario.getTopography().getBounds().equals(new VRectangle(10, 10, 100, 100)));

        // 5. change sourceAttributes
        var array = new ArrayList<Integer>(){{
            add(1);
        }};
        builder.setSourceField("targetIds", 1,array);
        scenario = builder.build();
        assertTrue(scenario.getTopography().getSources().get(0).getAttributes().getTargetIds().get(0) == 1);
    }

}
