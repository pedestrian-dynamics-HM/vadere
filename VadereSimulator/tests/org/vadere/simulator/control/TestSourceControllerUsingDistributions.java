package org.vadere.simulator.control;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.distribution.ConstantRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
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
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setDistributionClass(ConstantTestDistribution.class);
		initialize(builder);

		sourceController.update(0);
		pedestrianCountEquals(0);
		sourceController.update(0.9);
		pedestrianCountEquals(0);

		sourceController.update(1);
		pedestrianCountEquals(1);
	}

	@Test
	public void testEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder();
		initialize(builder);

		sourceController.update(1);
		pedestrianCountEquals(1);
		sourceController.update(2);
		pedestrianCountEquals(2);

		sourceController.update(3); // end time reached -> no effect
		pedestrianCountEquals(2);
	}

	@Test
	public void testOneTimeSpawn() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(1);
		initialize(builder);

		sourceController.update(0);
		pedestrianCountEquals(0);
		sourceController.update(1);
		pedestrianCountEquals(1);
		sourceController.update(2);
		pedestrianCountEquals(1);
	}

	@Test
	public void testSpawnNumber() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setSpawnNumber(10);
		initialize(builder);

		sourceController.update(1);
		pedestrianCountEquals(10);
		sourceController.update(2);
		pedestrianCountEquals(20);
	}

	@Test
	public void testSpawnRateGreaterThanUpdateRate() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(1)
				.setSpawnIntervalForConstantDistribution(0.3);
		initialize(builder);

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
		double startTime = 0;
		double endTime = 1;
		int spawnNumber = 100;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true);
		initialize(builder);

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
		double startTime = 1;
		double endTime = startTime;
		int spawnNumber = 100;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(startTime)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true);
		initialize(builder);

		doUpdates(100, 0, endTime + 1);

		// despite many updates, only one ped can be spawned
		assertEquals(1, countPedestrians());

		// now, move the peds away after updates
		doUpdatesBeamingPedsAway(1000);

		// now, all pedestrians should have been created
		assertEquals(spawnNumber, countPedestrians());

	}
	
	@Test
	public void testMaxSpawnNumberTotalSetTo0() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(0); // <-- max 0 -> spawn no peds at all
		initialize(builder);
		
		sourceController.update(1);
		sourceController.update(2);
		sourceController.update(3);
		
		assertEquals(0, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalNotSet() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(AttributesSource.NO_MAX_SPAWN_NUMBER_TOTAL); // <-- maximum not set
		initialize(builder);
		
		sourceController.update(1);
		sourceController.update(2);
		sourceController.update(3);
		
		assertEquals(2, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalWithSmallEndTime() {
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setMaxSpawnNumberTotal(4); // <-- not exhausted
		initialize(builder);
		
		sourceController.update(1);
		sourceController.update(2);
		sourceController.update(3);
		
		assertEquals(2, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTime() {
		double endTime = 100;
		int maxSpawnNumberTotal = 4;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(endTime)
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal); // <-- exhausted!
		initialize(builder);
		
		doUpdates(50, 0, 200);
		
		assertEquals(maxSpawnNumberTotal, countPedestrians());
	}

	@Test
	public void testMaxSpawnNumberTotalWithLargeEndTimeAndSpawnNumberGreater1() {
		int maxSpawnNumberTotal = 4; // <-- exhausted!
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setEndTime(100)
				.setSpawnNumber(5)
				.setMaxSpawnNumberTotal(maxSpawnNumberTotal);
		initialize(builder);
		
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
