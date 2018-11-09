package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link PedestrianStartTimeProcessorTest} for Test data see {@link
 * PedestrianStartTimeProcessorTestEnv}
 *
 * @author Stefan Schuhbäck
 */
public class PedestrianStartTimeProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new PedestrianStartTimeProcessorTestEnv();
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}