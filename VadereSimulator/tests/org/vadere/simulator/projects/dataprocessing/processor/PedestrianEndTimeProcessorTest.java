package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PedestrianEndTimeProcessorTest extends ProcessorTest {

	@Before
	public void setup(){
		processorTestEnv = new PedestrianEndTimeProcessorTestEnv();
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}