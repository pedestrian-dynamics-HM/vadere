package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

public class EvacuationTimeProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new EvacuationTimeProcessorTestEnv();
		super.setup();
	}


	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

	@Test
	public void doUpdateNaN() throws Exception {
		((EvacuationTimeProcessorTestEnv) processorTestEnv).loadSimulationStateMocksNaN();
		super.doUpdate();
	}

	@Test
	public void init() throws Exception {
		super.init();
	}

}