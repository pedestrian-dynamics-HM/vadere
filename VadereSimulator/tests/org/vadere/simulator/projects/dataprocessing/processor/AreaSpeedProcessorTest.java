package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;
import org.vadere.util.geometry.shapes.VRectangle;

import static org.junit.Assert.assertEquals;

public class AreaSpeedProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new AreaSpeedProcessorTestEnv();
		super.setup();
	}

	@Override
	public void assertInit(DataProcessor p) throws NoSuchFieldException, IllegalAccessException {
		assertEquals("Must be zero after init.", 0, p.getData().size());
		assertEquals("Must be zero after init.", 0, (int) r.valOfField("lastStep"));
		AttributesAreaProcessor attr = (AttributesAreaProcessor) p.getAttributes();
	}

	@Test
	public void doUpdate() throws Exception {
		AttributesAreaProcessor attr = (AttributesAreaProcessor) p.getAttributes();
		processorTestEnv.init();
		super.doUpdate();
	}

	@Test
	public void init() throws Exception {
		super.init();
	}

}