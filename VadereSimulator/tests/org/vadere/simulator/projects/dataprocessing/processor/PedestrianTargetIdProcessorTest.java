package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

public class PedestrianTargetIdProcessorTest extends ProcessorTest {

	@Before
	public void setup(){
		processorTestEnv = new PedestrianTargetIdProcessorTestEnv();
		//int and load ProcessorTestEnv
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}