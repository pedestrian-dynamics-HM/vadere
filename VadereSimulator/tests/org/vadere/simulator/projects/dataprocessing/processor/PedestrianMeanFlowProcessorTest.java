package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PedestrianMeanFlowProcessorTest extends ProcessorTest {

	@Override
	@Before
	public void setup(){
		processorTestEnv = new PedestrianFlowProcessorTestEnv();
		super.setup();
	}

	@Override
	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

	@Override
	@Test
	public void init() throws Exception {
		super.init();
	}

}