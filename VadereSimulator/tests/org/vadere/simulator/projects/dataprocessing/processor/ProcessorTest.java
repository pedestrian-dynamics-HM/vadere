package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.utils.reflection.ReflectionHelper;

import static org.junit.Assert.assertEquals;

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
	public void setup() {
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
		int l = processorTestEnv.getSimStates().size();
		p.postLoop(processorTestEnv.getSimStates().get(l - 1));

		OutputFile outputFile = processorTestEnv.getOutputFile();
		outputFile.write();

		// NOTE: these are the column names that have the additional information of the data processor ID.
		assertEquals(processorTestEnv.getHeader(), outputFile.getHeaderLine());

		String header = String.join(processorTestEnv.getDelimiter(), p.getHeaders());
		if (header.equals("")){
			assertEquals(processorTestEnv.getExpectedOutputAsList(), processorTestEnv.getOutput(0));
		} else {
			assertEquals(processorTestEnv.getExpectedOutputAsList(), processorTestEnv.getOutput(1));
		}
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

		String header = String.join(processorTestEnv.getDelimiter(), p.getHeaders());
		if (header.equals("")){
			assertEquals(processorTestEnv.getOutput(0).size(), p.getData().size());
		} else {
			assertEquals(processorTestEnv.getOutput(1).size(), p.getData().size());

		}
		assertEquals(processorTestEnv.getSimStates().size(), (int) r.valOfField("lastStep"));

		p.init(processorTestEnv.getManager());
		assertInit(p);
	}

}
