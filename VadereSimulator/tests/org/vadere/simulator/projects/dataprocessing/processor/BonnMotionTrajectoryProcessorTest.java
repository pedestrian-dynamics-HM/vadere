package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore
public class BonnMotionTrajectoryProcessorTest extends ProcessorTest {

	@Before
	public void setup(){
		processorTestEnv = new BonnMotionTrajectoryProcessorTestEnv();
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception{
		//DefaultSimulationStateMocks
		super.doUpdate();
	}

}