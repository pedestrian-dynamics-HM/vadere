package org.vadere.state.scenario.distribution;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public abstract class VDistributionTest {
	protected int spawnNumber = 1;

	abstract protected VDistribution<?> getDistributionUnderTest() throws Exception;

	@Test
	public void testGetSpawnNumber() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		int actual = dist.getSpawnNumber(1);
		assertEquals(spawnNumber, actual);
	}

	@Test
	public void testRemainingSpawnAgents() throws Exception {
		VDistribution<?> dist = getDistributionUnderTest();
		int expected = 1;
		dist.setRemainingSpawnAgents(expected);
		int actual = dist.getRemainingSpawnAgents();
		assertEquals(expected, actual);
	}

	@Test
	abstract public void testGetNextSpawnTime() throws Exception;

}
