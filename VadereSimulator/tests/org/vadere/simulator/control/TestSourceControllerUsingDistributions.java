package org.vadere.simulator.control;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

public class TestSourceControllerUsingDistributions extends TestSourceControllerUsingConstantSpawnRate {

	public static class ConstantTestDistribution extends ConstantRealDistribution {
		private static final long serialVersionUID = 1L;

		public ConstantTestDistribution(RandomGenerator unused, double value) {
			super(value);
		}
	}

	@Test
	public void testStartTime() {
		double startTime = 1;
		double endTime = 2;
		int spawnNumber = 1;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, false,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		sourceController.update(0);
		pedestrianCountEquals(0);
		sourceController.update(0.9);
		pedestrianCountEquals(0);

		sourceController.update(1);
		pedestrianCountEquals(1);
	}

	@Test
	public void testEndTime() {
		double startTime = 1;
		double endTime = 2;
		int spawnNumber = 1;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, false,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		sourceController.update(1);
		pedestrianCountEquals(1);
		sourceController.update(2);
		pedestrianCountEquals(2);

		sourceController.update(3); // end time reached -> no effect
		pedestrianCountEquals(2);
	}

	@Test
	public void testOneTimeSpawn() {
		double startTime = 1;
		double endTime = startTime; // only one single spawn event
		int spawnNumber = 1;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, false,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		sourceController.update(0);
		pedestrianCountEquals(0);
		sourceController.update(1);
		pedestrianCountEquals(1);
		sourceController.update(2);
		pedestrianCountEquals(1);
	}

	@Test
	public void testSpawnNumber() {
		double startTime = 1;
		double endTime = 2;
		int spawnNumber = 10;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, false,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		sourceController.update(1);
		pedestrianCountEquals(10);
		sourceController.update(2);
		pedestrianCountEquals(20);
	}

	@Test
	public void testSpawnRateGreaterThanUpdateRate() {
		double startTime = 0;
		double endTime = 1;
		int spawnNumber = 1;
		double spawnDelay = 0.3;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, false,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		// per update only one "spawn action" is performed.
		// if the spawn rate is higher than the update time increment, spawns will get lost.
		sourceController.update(0);
		pedestrianCountEquals(1);
		sourceController.update(1);
		pedestrianCountEquals(4);
	}

	@Test
	public void testUseFreeSpaceOnly() {
		// expected: not stop spawning before all pedestrians are created (even after end time)
		boolean useFreeSpaceOnly = true;
		double startTime = 0;
		double endTime = 1;
		int spawnNumber = 100;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, useFreeSpaceOnly,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		doUpdates(100, startTime, endTime + 1);

		// despite many updates, only one ped can be spawned
		assertEquals(1, countPedestrians());

		doUpdatesBeamingPedsAway(1000);

		// now, all pedestrians should have been created
		assertEquals(2 * spawnNumber, countPedestrians());
	}

	@Test
	public void testUseFreeSpaceOnlyWithSingleSpawnEvent() {
		// works also with sources that have startTime == endTime?
		// expected: not stop spawning before all pedestrians are created (even after end time)
		boolean useFreeSpaceOnly = true;
		double startTime = 1;
		double endTime = 1;
		int spawnNumber = 100;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, useFreeSpaceOnly,
				ConstantTestDistribution.class, maxSpawnNumberTotal);

		doUpdates(100, 0, endTime + 1);

		// despite many updates, only one ped can be spawned
		assertEquals(1, countPedestrians());

		// now, move the peds away after updates
		doUpdatesBeamingPedsAway(1000);

		// now, all pedestrians should have been created
		assertEquals(spawnNumber, countPedestrians());

	}
	
	@Test
	public void testMaxSpawnNumberTotalNotSet() {
		boolean useFreeSpaceOnly = false;
		double startTime = 1;
		double endTime = 2;
		int spawnNumber = 1;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 0; // <-- maximum not set
		initialize(startTime, endTime, spawnNumber, spawnDelay, useFreeSpaceOnly,
				ConstantTestDistribution.class, maxSpawnNumberTotal);
		
		sourceController.update(1);
		sourceController.update(2);
		sourceController.update(3);
		
		assertEquals(2, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalWithSmallEndTime() {
		boolean useFreeSpaceOnly = false;
		double startTime = 1;
		double endTime = 2;
		int spawnNumber = 1;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 4; // <-- not exhausted
		initialize(startTime, endTime, spawnNumber, spawnDelay, useFreeSpaceOnly,
				ConstantTestDistribution.class, maxSpawnNumberTotal);
		
		sourceController.update(1);
		sourceController.update(2);
		sourceController.update(3);
		
		assertEquals(2, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTime() {
		boolean useFreeSpaceOnly = false;
		double startTime = 1;
		double endTime = 100;
		int spawnNumber = 1;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 4; // <-- exhausted!
		initialize(startTime, endTime, spawnNumber, spawnDelay, useFreeSpaceOnly,
				ConstantTestDistribution.class, maxSpawnNumberTotal);
		
		doUpdates(50, 0, 200);
		
		assertEquals(maxSpawnNumberTotal, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTimeAndSpawnNumberGreater1() {
		boolean useFreeSpaceOnly = false;
		double startTime = 1;
		double endTime = 100;
		int spawnNumber = 5;
		double spawnDelay = 1;
		int maxSpawnNumberTotal = 4; // <-- exhausted!
		initialize(startTime, endTime, spawnNumber, spawnDelay, useFreeSpaceOnly,
				ConstantTestDistribution.class, maxSpawnNumberTotal);
		
		doUpdates(50, 0, 200);
		
		assertEquals(maxSpawnNumberTotal, countPedestrians());
	}

	private void doUpdates(int number, double startTime, double endTimeExclusive) {
		double timeStep = (endTimeExclusive - startTime) / number;
		for (double t = startTime; t < endTimeExclusive + 1; t += timeStep) {
			sourceController.update(t);
		}
	}

	private void doUpdatesBeamingPedsAway(int number) {
		double start = 10;
		for (double t = start; t < start + number; t += 1) {
			sourceController.update(t);
			beamPedsAway();
		}
	}

	private void beamPedsAway() {
		final VPoint positionFarAway = new VPoint(1000, 1000);
		for (Pedestrian pedestrian : topography.getElements(Pedestrian.class)) {
			pedestrian.setPosition(positionFarAway);
		}
	}

	private void pedestrianCountEquals(int expected) {
		assertEquals(expected, countPedestrians());
	}
	
}
