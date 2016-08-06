package org.vadere.simulator.simulation;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.apache.commons.math3.distribution.RealDistribution;
import org.junit.Test;
import org.vadere.simulator.control.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.IOUtils;

public class TestSourceControllerUsingConstantSpawnRate {

	protected Random random;
	protected AttributesAgent attributesPedestrian;
	protected DynamicElementFactory pedestrianFactory;
	protected Source source;
	protected Topography topography = new Topography();
	protected SourceController sourceController;
	protected AttributesSource attributesSource;
	protected long randomSeed = 0;

	public void initialize(double startTime, double endTime, int spawnNumber, double spawnDelay,
			boolean useFreeSpaceOnly, Class<? extends RealDistribution> distributionClass, int maxSpawnNumberTotal) {

		String json = generateSourceAttributesJson(startTime, endTime, spawnNumber, spawnDelay,
				String.valueOf(spawnDelay), useFreeSpaceOnly, distributionClass.getName(), maxSpawnNumberTotal);
		attributesSource = IOUtils.getGson().fromJson(json, AttributesSource.class);
		attributesPedestrian = new AttributesAgent();

		random = new Random(randomSeed);

		source = new Source(attributesSource);
		pedestrianFactory = new DynamicElementFactory() {
			private int pedestrianIdCounter = 0;

			@Override
			public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {
				AttributesAgent att = new AttributesAgent(
						attributesPedestrian, id > 0 ? id : ++pedestrianIdCounter);
				Pedestrian ped = new Pedestrian(att, random);
				ped.setPosition(position);
				return ped;
			}
		};

		sourceController = new SourceController(topography, source,
				pedestrianFactory, attributesPedestrian, random);
	}

	private void initializeDefault(double startTime, double endTime, int spawnNumber,
			double spawnDelay) {
		final int noMaxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay,
				false, ConstantDistribution.class, noMaxSpawnNumberTotal);
	}

	private static String generateSourceAttributesJson(double startTime, double endTime,
			int spawnNumber, double spawnDelay, String distributionParams, boolean useFreeSpaceOnly,
			String distributionClassName, int maxSpawnNumberTotal) {
		return "{\"shape\": {\"type\": \"POLYGON\",\"points\": ["
				+ "{\"x\": 0.0,\"y\": 0.0},{\"x\": 0.1,\"y\": 0},{\"x\": 0.1,\"y\": 0.1},{\"x\": 0,\"y\": 0.1}]},"
				+ "\"spawnDelay\": " + spawnDelay
				+ ",\"spawnNumber\":  " + spawnNumber
				+ ",\"maxSpawnNumberTotal\":  " + maxSpawnNumberTotal
				+ ",\"interSpawnTimeDistribution\": \"" + distributionClassName + "\""
				+ ",\"distributionParameters\": [" + distributionParams + "]"
				+ ",\"startTime\": " + startTime
				+ ",\"endTime\": " + endTime
				+ ",\"spawnAtRandomPositions\": true"
				+ ",\"useFreeSpaceOnly\": " + useFreeSpaceOnly
				+ ",\"targetIds\": [1]}";
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEqualStartAndEndTime() {

		double startTime = 0.0;
		double endTime = 0.0;
		int spawnNumber = 1;
		double spawnDelay = 10;
		initializeDefault(startTime, endTime, spawnNumber, spawnDelay);

		sourceController.update(0);

		assertEquals("wrong pedestrian number", 1, countPedestrians());
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEndTimeLarge() {

		double startTime = 0.0;
		double endTime = 10.0;
		int spawnNumber = 1;
		double spawnDelay = 10;
		initializeDefault(startTime, endTime, spawnNumber, spawnDelay);

		sourceController.update(startTime);
		// one at the beginning
		assertEquals("wrong pedestrian number.", 1, countPedestrians());

		sourceController.update(endTime);
		// and one at the end
		assertEquals("wrong pedestrian number.", 2, countPedestrians());
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateSpawnDelayThreeTimes() {

		double startTime = 0.0;
		double endTime = 10.0;
		int spawnNumber = 1;
		double spawnDelay = 5; // should spawn one pedestrian at start, middle and end.
		initializeDefault(startTime, endTime, spawnNumber, spawnDelay);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 3, countPedestrians());
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateSmallSpawnDelay() {

		double startTime = 0.0;
		double endTime = 1.0;
		int spawnNumber = 1;
		double spawnDelay = 0.1;
		initializeDefault(startTime, endTime, spawnNumber, spawnDelay);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 11, countPedestrians());
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateUseFreeSpaceOnly() {

		double startTime = 0;
		double endTime = 0;
		int spawnNumber = 100;
		double spawnDelay = 1;
		int noMaxSpawnNumberTotal = 0;
		initialize(startTime, endTime, spawnNumber, spawnDelay, true, ConstantDistribution.class, noMaxSpawnNumberTotal);

		for (double simTimeInSec = 0; simTimeInSec < 1000; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);
		}

		// if the first ped does not move away, there should no more pedestrians
		// be created
		assertEquals("wrong pedestrian number.", 1, countPedestrians());

		// now, move the peds away after creating them
		for (double simTimeInSec = 1000; simTimeInSec < 2000; simTimeInSec += 1.0) {
			sourceController.update(simTimeInSec);

			VPoint positionFarAway = new VPoint(1000, 1000);
			for (Pedestrian pedestrian : topography.getElements(Pedestrian.class)) {
				pedestrian.setPosition(positionFarAway);
			}
		}

		// now, all pedestrians should have been created
		assertEquals("wrong pedestrian number.", 100, countPedestrians());
	}

	protected int countPedestrians() {
		return topography.getElements(Pedestrian.class).size();
	}

}
