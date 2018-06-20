package org.vadere.simulator.control;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

public class TestSourceControllerUsingConstantSpawnRate {

	protected Random random;
	protected AttributesAgent attributesPedestrian;
	protected DynamicElementFactory pedestrianFactory;
	protected Source source;
	protected Topography topography = new Topography();
	protected SourceController sourceController;
	protected AttributesSource attributesSource;
	protected SourceControllerFactory sourceControllerFactory;
	protected long randomSeed = 0;

	public void initSourceControllerFactory() {
		sourceControllerFactory = new SingleSourceControllerFactory();
	}

	public void initialize(SourceTestAttributesBuilder builder) {

		attributesSource = builder.getResult();
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

		initSourceControllerFactory();

		sourceController = this.sourceControllerFactory.create(topography, source,
				pedestrianFactory, attributesPedestrian, random);
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEqualStartAndEndTime() {

		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0);
		initialize(builder);

		sourceController.update(0);
		sourceController.update(1);
		sourceController.update(2);

		assertEquals("wrong pedestrian number", 1, countPedestrians());
	}

	/**
	 * Test method for {@link org.vadere.simulator.control.SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEndTimeLarge() {

		double startTime = 0.0;
		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(10);
		initialize(builder);

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

		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(5);
		initialize(builder);

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

		double endTime = 1.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(0.1);
		initialize(builder);

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

		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true);
		initialize(builder);

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

	protected void pedestrianCountEquals(int expected) {
		assertEquals(expected, countPedestrians());
	}

	protected void doUpdates(int number, double startTime, double endTimeExclusive) {
		double timeStep = (endTimeExclusive - startTime) / number;
		for (double t = startTime; t < endTimeExclusive + 1; t += timeStep) {
			sourceController.update(t);
		}
	}

	protected void doUpdatesBeamingPedsAway(int number) {
		double start = 10;
		for (double t = start; t < start + number; t += 1) {
			sourceController.update(t);
			beamPedsAway();
		}
	}

	protected void beamPedsAway() {
		final VPoint positionFarAway = new VPoint(1000, 1000);
		for (Pedestrian pedestrian : topography.getElements(Pedestrian.class)) {
			pedestrian.setPosition(positionFarAway);
		}
	}
}
