package org.vadere.state.scenario;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

public class TestPedestrian {
	protected Pedestrian pedestrian;

	@Before
	public void setUp() throws Exception {
		pedestrian = createPedestrian();
		pedestrian.setNextTargetListIndex(0);
	}

	@Test
	public void testSetNextTargetListIndex() {
		pedestrian.setNextTargetListIndex(1);
		assertEquals(1, pedestrian.getNextTargetListIndex());
	}

	@Test
	public void testIncrementNextTargetListIndex() {
		assertEquals(0, pedestrian.getNextTargetListIndex());
		pedestrian.incrementNextTargetListIndex();
		assertEquals(1, pedestrian.getNextTargetListIndex());
	}

	@Test
	public void testGetNextTargetId() {
		pedestrian.getTargets().add(3);
		assertEquals(3, pedestrian.getNextTargetId());
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testGetNextTargetIdFail() {
		pedestrian.getTargets().clear();
		pedestrian.getNextTargetId();
	}

	@Test
	public void testHasNextTarget() {
		assertFalse(pedestrian.hasNextTarget());

		pedestrian.getTargets().add(0);
		assertTrue(pedestrian.hasNextTarget());

		pedestrian.incrementNextTargetListIndex();
		assertFalse(pedestrian.hasNextTarget());
	}

	protected Pedestrian createPedestrian() {
		return new Pedestrian(new AttributesAgent(), new Random(0));
	}

}
