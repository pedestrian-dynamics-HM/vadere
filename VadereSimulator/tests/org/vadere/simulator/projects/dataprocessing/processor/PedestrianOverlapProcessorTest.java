package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processor.AttributesPedestrianOverlapProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

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
	public void init() throws Exception {
		AttributesPedestrianOverlapProcessor att = (AttributesPedestrianOverlapProcessor) p.getAttributes();
		assertInit(p);
		assertEquals(att.getPedRadius(), r.valOfField("pedRadius"), 0.001);

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