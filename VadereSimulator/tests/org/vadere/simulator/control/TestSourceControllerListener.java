package org.vadere.simulator.control;

import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.listener.ControllerEventListener;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestSourceControllerListener extends TestSourceControllerUsingConstantSpawnRate {

	@Test
	public void testListener() throws IOException {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0)
				.setEndTime(10)
				.setSpawnNumber(1)
				.setMaxSpawnNumberTotal(2)
				.setId(33);
		initialize(builder);

		ControllerEventListener<Agent, SourceController> listener = new ControllerEventListener<Agent, SourceController>() {
			@Override
			public Agent notify(SourceController controller, double simTimeInSec, Agent scenarioElement) {
				scenarioElement.setSingleTarget(99, true);
				return null;
			}
		};
		first().sourceController.register(listener);

		// expect first pedestrians target list will be updated
		first().sourceController.update(0);
		Pedestrian p = first().topography.getPedestrianDynamicElements().getElements().toArray(Pedestrian[]::new)[0];
		int target = p.getTargets().getFirst();
		assertEquals("", 99, target);

		// after unregister listener must not change target
		first().sourceController.unregister(listener);
		first().sourceController.update(1);
		int pedCount = first().topography.getPedestrianDynamicElements().getElements().size();
		assertEquals("should be 2", 2, pedCount);
		p = first().topography.getPedestrianDynamicElements().getElements().toArray(Pedestrian[]::new)[1];
		target = p.getTargets().getFirst();
		assertNotEquals(99, target);
	}
}
