package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingTimeProcessor;

import static org.junit.Assert.*;

public class PedestrianWaitingTimeProcessorTest extends ProcessorTest{

	@Before
	public void setup(){
		processorTestEnv = new PedestrianWaitingTimeProcessorTestEnv();
		super.setup();
	}

	@Override
	public void assertInit(DataProcessor p) throws NoSuchFieldException, IllegalAccessException {
		assertEquals("Must be zero after init.", 0, p.getData().size());
		assertEquals("Must be zero after init.", 0, (int) r.valOfField("lastStep"));
		assertEquals(0.0, r.valOfField("lastSimTime"), 0.001);
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

	@Test
	public void init() throws Exception {
		assertInit(p);

		AttributesPedestrianWaitingTimeProcessor attr =
				(AttributesPedestrianWaitingTimeProcessor) p.getAttributes();

		for (SimulationState s : processorTestEnv.getSimStates()) {
			p.update(s);
		}
		processorTestEnv.getOutputFile().write();

		assertEquals(processorTestEnv.getOutput().size(), p.getData().size());
		assertEquals(processorTestEnv.getSimStates().size(), (int) r.valOfField("lastStep"));

		p.init(processorTestEnv.getManager());
		assertInit(p);
	}

	@Test
	public void getAttributes() {
	}

}