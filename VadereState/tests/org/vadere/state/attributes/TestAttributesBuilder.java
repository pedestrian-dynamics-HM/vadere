package org.vadere.state.attributes;

import org.junit.Ignore;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestAttributesBuilder {

    private AttributesBuilder<AttributesAgent> attributesAgentAttributesBuilder;
    private double radius = 0.2;

    @Test
    public void testChangeAttributes() {
        AttributesAgent attributesAgent = new AttributesAgent();
        assertTrue(attributesAgent.getRadius() == radius);

        AttributesBuilder<AttributesAgent> builder = new AttributesBuilder<>(attributesAgent);
        builder.setField("radius", 2.1);
        assertTrue(builder.build().getRadius() == 2.1);
        assertTrue(attributesAgent.getRadius() == radius);
    }
}
