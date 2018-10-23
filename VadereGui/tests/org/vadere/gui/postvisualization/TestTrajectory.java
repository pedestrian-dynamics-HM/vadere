package org.vadere.gui.postvisualization;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.geometry.shapes.VPoint;

import junit.framework.TestCase;

/**
 * Unit test for {@link org.vadere.state.simulation.Trajectory}
 *
 */
public class TestTrajectory extends TestCase {

	private Map<Step, List<Agent>> pedestriansByStep;

	@Override
	protected void setUp() throws Exception {
		pedestriansByStep = new HashMap<>();
		List<Step> steps = Arrays.asList(new Step(2), new Step(4), new Step(5), new Step(7));
		List<Pedestrian> pedestrians = Arrays.asList(
				new Pedestrian(new AttributesAgent(1), new Random()),
				new Pedestrian(new AttributesAgent(2), new Random()),
				new Pedestrian(new AttributesAgent(3), new Random()),
				new Pedestrian(new AttributesAgent(5), new Random()),
				new Pedestrian(new AttributesAgent(6), new Random()),
				new Pedestrian(new AttributesAgent(-1), new Random()));

		steps.forEach(step -> pedestriansByStep.put(step,
				pedestrians.stream().map(ped -> (Agent) ped.clone())
						.peek(ped -> ped.setPosition(
								ped.getPosition().add(new VPoint(step.getStepNumber(), step.getStepNumber()))))
						.collect(Collectors.toList())));
	}

	@Test
	public void testGetPedestrian() {
		Trajectory trajectory = new Trajectory(pedestriansByStep, 1);
		assertEquals(trajectory.getAgent(new Step(3)), (trajectory.getAgent(new Step(2))));
		assertTrue(trajectory.getAgent(new Step(3)).isPresent());
		assertEquals(trajectory.getAgent(new Step(6)), (trajectory.getAgent(new Step(5))));
		assertTrue(trajectory.getAgent(new Step(6)).isPresent());
		assertEquals(trajectory.getAgent(new Step(12)), (trajectory.getAgent(new Step(7))));
		assertTrue(trajectory.getAgent(new Step(12)).isPresent());
		assertEquals(trajectory.getAgent(new Step(1)), (trajectory.getAgent(new Step(2))));
		assertTrue(trajectory.getAgent(new Step(1)).isPresent());

		assertFalse(trajectory.getAgent(new Step(2)).equals(trajectory.getAgent(new Step(4))));
		assertTrue(trajectory.getAgent(new Step(2)).isPresent());
		assertFalse(trajectory.getAgent(new Step(1)).equals(trajectory.getAgent(new Step(12))));
		assertTrue(trajectory.getAgent(new Step(1)).isPresent());
		assertFalse(trajectory.getAgent(new Step(1)).equals(trajectory.getAgent(new Step(12))));
	}

	@Test
	public void testGetPositionReverse() {
		Trajectory trajectory = new Trajectory(pedestriansByStep, 2);
		IntStream.rangeClosed(1, 17).forEach(stepNumber -> assertTrue(
				trajectory.getPositionsReverse(new Step(stepNumber)).count() + "!=" + stepNumber,
				trajectory.getPositionsReverse(new Step(stepNumber)).count() == stepNumber));
		List<VPoint> reversePositions = trajectory.getPositionsReverse(new Step(12)).collect(Collectors.toList());
		IntStream.rangeClosed(0, reversePositions.size() - 1)
				.forEach(index -> assertEquals(
						reversePositions.get(index),
						trajectory.getAgent(new Step(reversePositions.size() - index)).get().getPosition()));
	}
}
