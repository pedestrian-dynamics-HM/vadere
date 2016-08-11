package org.vadere.state.scenario;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.scenario.Pedestrian;

public class TestDeprecatedPedestrian extends TestPedestrian {

	@Override
	@Before
	public void setUp() throws Exception {
		pedestrian = createDeprecatedPedestrian();
	}

	@Override
	@Test(expected = IllegalStateException.class)
	public void testIncrementNextTargetListIndex() {
		pedestrian.incrementNextTargetListIndex();
	}

	@Override
	@Test
	public void testGetNextTargetId() {
		pedestrian.getTargets().add(3);
		pedestrian.getTargets().add(4);
		assertEquals(3, pedestrian.getNextTargetId());
	}

	@Override
	@Test(expected = NoSuchElementException.class)
	public void testGetNextTargetIdFail() {
		pedestrian.getTargets().clear();
		pedestrian.getNextTargetId();
	}

	@Override
	@Test
	public void testHasNextTarget() {
		assertFalse(pedestrian.hasNextTarget());
		pedestrian.getTargets().add(0);
		assertTrue(pedestrian.hasNextTarget());
		pedestrian.getTargets().add(1);
		assertTrue(pedestrian.hasNextTarget());
	}

	private Pedestrian createDeprecatedPedestrian() {
		Pedestrian pedestrian = createPedestrian();
		pedestrian.setNextTargetListIndex(-1);
		return pedestrian;
	}
}
