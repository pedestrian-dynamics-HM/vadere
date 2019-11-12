package org.vadere.simulator.control;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.SourceTestAttributesBuilder;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TestSourceControllerUsingConstantSpawnRate {


	ArrayList<SourceTestData> sourceTestData;

	protected SourceTestData first() {
		return sourceTestData.get(0);
	}

	protected SourceTestData second() {
		return sourceTestData.get(1);
	}

	@Before
	public void init() {
		sourceTestData = new ArrayList<>();
	}

	public SourceControllerFactory getSourceControllerFactory(SourceTestData d) {
		return new SingleSourceControllerFactory();
	}


	public void initialize(SourceTestAttributesBuilder builder) throws IOException{

		SourceTestData d = new SourceTestData();

		d.attributesSource = builder.getResult();
		d.attributesPedestrian = new AttributesAgent();

		d.random = new Random(builder.getRandomSeed());

		d.source = new Source(d.attributesSource);
		d.pedestrianFactory = new DynamicElementFactory() {
			private int pedestrianIdCounter = 0;

			@Override
			public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {
				AttributesAgent att = new AttributesAgent(
						d.attributesPedestrian, registerDynamicElementId(null, id));
				Pedestrian ped = new Pedestrian(att, d.random);
				ped.setPosition(position);
				return ped;
			}

			@Override
			public int registerDynamicElementId(Topography topography, int id) {
				return id > 0 ? id : ++pedestrianIdCounter;
			}

			@Override
			public int getNewDynamicElementId(Topography topography) {
				return registerDynamicElementId(topography, AttributesAgent.ID_NOT_SET);
			}

			@Override
			public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
				return createElement(position, AttributesAgent.ID_NOT_SET, Pedestrian.class).getShape();
			}


		};

		d.sourceControllerFactory = getSourceControllerFactory(d);

		d.sourceController = d.sourceControllerFactory.create(d.topography, d.source,
				d.pedestrianFactory, d.attributesPedestrian, d.random);

		sourceTestData.add(d);
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEqualStartAndEndTime() throws IOException {

		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0);
		initialize(builder);

		first().sourceController.update(0);
		first().sourceController.update(1);
		first().sourceController.update(2);

		assertEquals("wrong pedestrian number", 1, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateEndTimeLarge() throws IOException {

		double startTime = 0.0;
		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(startTime).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(10);
		initialize(builder);

		first().sourceController.update(startTime);
		// one at the beginning
		assertEquals("wrong pedestrian number.", 1, countPedestrians(0));

		first().sourceController.update(endTime);
		// and one at the end
		assertEquals("wrong pedestrian number.", 2, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateSpawnDelayThreeTimes() throws IOException {

		double endTime = 10.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(5);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 3, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateSmallSpawnDelay() throws IOException {

		double endTime = 1.0;
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setStartTime(0).setEndTime(endTime)
				.setSpawnIntervalForConstantDistribution(0.1);
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < endTime * 2; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		assertEquals("wrong pedestrian number.", 11, countPedestrians(0));
	}

	/**
	 * Test method for {@link SourceController#update(double)}.
	 */
	@Test
	public void testUpdateUseFreeSpaceOnly() throws IOException {

		AttributesAgent attributesAgent = new AttributesAgent();
		SourceTestAttributesBuilder builder = new SourceTestAttributesBuilder()
				.setOneTimeSpawn(0)
				.setSpawnNumber(100)
				.setUseFreeSpaceOnly(true)
				.setSourceDim(new VRectangle(0, 0, attributesAgent.getRadius()*2 + 0.05, attributesAgent.getRadius()*2 + 0.05)); // small source
		initialize(builder);

		for (double simTimeInSec = 0; simTimeInSec < 1000; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);
		}

		// if the first ped does not move away, there should no more pedestrians
		// be created
		assertEquals("wrong pedestrian number.", 1, countPedestrians(0));

		// now, move the peds away after creating them
		for (double simTimeInSec = 1000; simTimeInSec < 2000; simTimeInSec += 1.0) {
			first().sourceController.update(simTimeInSec);

			VPoint positionFarAway = new VPoint(1000, 1000);
			for (Pedestrian pedestrian : first().topography.getElements(Pedestrian.class)) {
				pedestrian.setPosition(positionFarAway);
			}
		}

		// now, all pedestrians should have been created
		assertEquals("wrong pedestrian number.", 100, countPedestrians(0));
	}

	protected int countPedestrians(int source) {
		return sourceTestData.get(source).topography.getElements(Pedestrian.class).size();
	}

	protected void pedestrianCountEquals(int expected) {
		assertEquals(expected, countPedestrians(0));
	}

	protected void doUpdates(int source, int number, double startTime, double endTimeExclusive) {
		double timeStep = (endTimeExclusive - startTime) / number;
		for (double t = startTime; t < endTimeExclusive + 1; t += timeStep) {
			sourceTestData.get(source).sourceController.update(t);
		}
	}

	protected void doUpdatesBeamingPedsAway(int source, int number) {
		double start = 10;
		for (double t = start; t < start + number; t += 1) {
			sourceTestData.get(source).sourceController.update(t);
			beamPedsAway(source);
		}
	}

	protected void beamPedsAway(int source) {
		final VPoint positionFarAway = new VPoint(1000, 1000);
		for (Pedestrian pedestrian : sourceTestData.get(source).topography.getElements(Pedestrian.class)) {
			pedestrian.setPosition(positionFarAway);
		}
	}

	class SourceTestData {
		public Random random;
		public AttributesAgent attributesPedestrian;
		public DynamicElementFactory pedestrianFactory;
		public Source source;
		public Topography topography = new Topography();
		public SourceController sourceController;
		public AttributesSource attributesSource;
		public SourceControllerFactory sourceControllerFactory;
		public long randomSeed = 0;
	}
}
