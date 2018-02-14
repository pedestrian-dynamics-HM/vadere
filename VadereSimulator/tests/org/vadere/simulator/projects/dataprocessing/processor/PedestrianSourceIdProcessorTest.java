package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

public class PedestrianSourceIdProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new PedestrianSourceIdProcessorTestEnv();
		super.setup();
	}


	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}