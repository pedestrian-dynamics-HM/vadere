package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

public class NumberOverlapsProcessorTest extends ProcessorTest{
	@Before
	public void setup() {
		processorTestEnv = new NumberOverlapsProcessorTestEnv();
		super.setup();
	}


	@Test
	public void doUpdateWithOverlaps() throws Exception {
		super.doUpdate();
	}

	@Test
	public void doUpdateWithoutOverlaps() throws Exception {
		((NumberOverlapsProcessorTestEnv)processorTestEnv).noOverlapsMock();
		super.doUpdate();
	}
}