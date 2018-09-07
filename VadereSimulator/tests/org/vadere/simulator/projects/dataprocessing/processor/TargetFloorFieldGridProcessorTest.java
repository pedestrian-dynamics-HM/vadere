package org.vadere.simulator.projects.dataprocessing.processor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.sfm.SocialForceModel;

import java.util.Optional;

public class TargetFloorFieldGridProcessorTest extends ProcessorTest {

	@Before
	public void setup() {
		processorTestEnv = new TargetFloorFieldGridProcessorTestEnv();
		super.setup();
	}

	/**
	 * If Model is not present do nothing
	 */
	@Test
	public void doUpdateNoMainModel() throws Exception {
		processorTestEnv.clearStates();
		processorTestEnv.addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				Mockito.when(state.getMainModel()).thenReturn(Optional.empty());
				processorTestEnv.expectedOutput.clear();
			}
		});
		super.doUpdate();
	}


	@Test
	public void doUpdateWrongMainModel() throws Exception {
		processorTestEnv.clearStates();
		processorTestEnv.addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				MainModel wrongModel = Mockito.mock(SocialForceModel.class);
				Mockito.when(state.getMainModel()).thenReturn(Optional.of(wrongModel));
				processorTestEnv.expectedOutput.clear();
			}
		});
		super.doUpdate();
	}

	@Test
	public void doUpdate() throws Exception {
		super.doUpdate();
	}

}