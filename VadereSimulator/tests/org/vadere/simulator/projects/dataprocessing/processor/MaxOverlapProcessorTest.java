package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;


public class MaxOverlapProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new MaxOverlapProcessorTestEnv();
		super.setup();
	}


	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}
}