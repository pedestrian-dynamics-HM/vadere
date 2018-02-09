package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.tests.reflection.ReflectionHelper;

/**
 * Tests for {@link PedestrianStartTimeProcessorTest} for Test data see {@link
 * PedestrianStartTimeProcessorTestEnv}
 *
 * @author Stefan Schuhbäck
 */
public class PedestrianStartTimeProcessorTest extends ProcessorTest {

	@Before
	public void init() {
		processorTestEnv = new PedestrianStartTimeProcessorTestEnv();
		processorTestEnv.loadDefaultSimulationStateMocks();
		processorTestEnv.init();
		p = processorTestEnv.getTestedProcessor();
		r = ReflectionHelper.create(p);
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}