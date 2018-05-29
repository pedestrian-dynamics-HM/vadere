package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AreaDensityVoronoiProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new AreaDensityVoronoiProcessorTestEnv();
		super.setup();
	}

	@Test
	public void doUpdate() throws Exception {
		AreaDensityVoronoiProcessorTestEnv env = (AreaDensityVoronoiProcessorTestEnv) processorTestEnv;
		env.loadOneCircleEvent();
		super.doUpdate();
	}

	/**
	 * This will fail. The implementation fails to create the write segmentation if all
	 * Pedestrian are collinear.
	 */
	@Test
	@Ignore
	public void withCollinear() throws Exception {
		AreaDensityVoronoiProcessorTestEnv env = (AreaDensityVoronoiProcessorTestEnv) processorTestEnv;
		env.loadCollinearSetup();
		super.doUpdate();
	}

	@Test
	@Ignore
	public void init() throws Exception {
		super.init();
	}

}