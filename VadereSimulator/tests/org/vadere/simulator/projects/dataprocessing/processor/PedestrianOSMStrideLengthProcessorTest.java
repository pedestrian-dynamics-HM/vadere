package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

public class PedestrianOSMStrideLengthProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new PedestrianOSMStrideLengthProcessorTestEnv();
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

	@Test
	public void init() throws Exception {
		super.doUpdate();
	}

}