package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.simulator.control.simulation.SimulationState;

import static org.junit.Assert.assertEquals;

public class PedestrianOverlapProcessorTest extends ProcessorTest {

	@Before
	public void setup(){
		processorTestEnv = new PedestrianOverlapProcessorTestEnv();
		super.setup();

	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

	@Test
	public void doUpdateWithOverlap() throws Exception {
		((PedestrianOverlapProcessorTestEnv)processorTestEnv).verySmallOverlapping();
		super.doUpdate();
	}

	@Test
	public void doUpdateWithoutOverlap() throws Exception {
		((PedestrianOverlapProcessorTestEnv)processorTestEnv).verySmallNotOverlapping();
		super.doUpdate();
	}

	@Test @Ignore
	public void doUpdateWithTouching() throws Exception {
		((PedestrianOverlapProcessorTestEnv)processorTestEnv).touching();
		super.doUpdate();
	}

	@Test
	public void init() throws Exception {
		assertInit(p);

		for (SimulationState s : processorTestEnv.getSimStates()) {
			p.update(s);
		}
		processorTestEnv.getOutputFile().write();

		assertEquals(processorTestEnv.getOutput().size(), p.getData().size());
		assertEquals(processorTestEnv.getSimStates().size(), (int) r.valOfField("lastStep"));

		p.init(processorTestEnv.getManager());
		assertInit(p);
	}




}