package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PedestrianLastPositionProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new PedestrianLastPositionProcessorTestEnv();
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

	@Test
	public void init() throws Exception {
	}


}