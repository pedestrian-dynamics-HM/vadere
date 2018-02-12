package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.tests.reflection.ReflectionHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Base Test for all Processors.
 *
 * @author Stefan Schuhbäck
 */
public abstract class ProcessorTest {

	DataProcessor p;
	ReflectionHelper r;
	ProcessorTestEnv<?, ?> processorTestEnv;

	@Before
	public void setup(){
		processorTestEnv.loadDefaultSimulationStateMocks();
		processorTestEnv.init();
		p = processorTestEnv.getTestedProcessor();
		r = ReflectionHelper.create(p);
	}

	/**
	 * After a call to {@link DataProcessor#init(ProcessorManager)} the data map and the step count
	 * must be reset.
	 */
	public void assertInit(DataProcessor p) throws NoSuchFieldException, IllegalAccessException {
		assertEquals("Must be zero after init.", 0, p.getData().size());
		assertEquals("Must be zero after init.", 0, (int) r.valOfField("lastStep"));
	}

	/**
	 * Run {@link SimulationState}s defined within {@link ProcessorTestEnv} and compare expected
	 * output with output of {@link OutputFile}
	 */
	@Test
	public void doUpdate() throws Exception {
		assertInit(p);

		for (SimulationState s : processorTestEnv.getSimStates()) {
			p.update(s);
		}
		processorTestEnv.getOutputFile().write();

		String header = String.join(processorTestEnv.getDelimiter(), p.getHeaders());
		assertTrue(processorTestEnv.getHeader().contains(header));
		assertEquals(processorTestEnv.getExpectedOutputAsList(), processorTestEnv.getOutput());
	}

	/**
	 * Ensure that after multiple runs the state of a {@link DataProcessor} is reset.
	 */
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
