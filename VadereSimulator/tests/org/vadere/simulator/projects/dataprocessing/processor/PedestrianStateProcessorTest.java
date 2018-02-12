package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.tests.reflection.ReflectionHelper;

public class PedestrianStateProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new PedestrianStateProcessorTestEnv();
		processorTestEnv.loadDefaultSimulationStateMocks();
		processorTestEnv.init();
		p = processorTestEnv.getTestedProcessor();
		r = ReflectionHelper.create(p);
	}

	@Test
	public void init() throws Exception {
		super.init();
	}


	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}