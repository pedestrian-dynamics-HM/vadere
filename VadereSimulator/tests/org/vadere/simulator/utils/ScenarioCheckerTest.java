package org.vadere.simulator.utils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.scenariochecker.ScenarioChecker;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessageType;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.builder.*;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.simulator.utils.reflection.TestResourceHandlerScenario;
import org.vadere.state.scenario.spawner.impl.RegularSpawner;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Area;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ScenarioCheckerTest implements TestResourceHandlerScenario {

	TopographyTestBuilder builder;

	@Before
	public void setup() {
		builder = new TopographyTestBuilder();
	}

	// Test checkUniqueSourceId

	/**
	 * There should be non unique ids
	 */
	@Test
	public void testCheckUniqueSourceIdNegative() {
		builder.addSource(); //id = -1 ok first
		builder.addSource(); //id = -1 err
		builder.addSource(); //id = -1 err
		builder.addSource(2); // ok first
		builder.addSource(2); // err
		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkUniqueSourceId();

		assertEquals("The sources should have the same id", 3, out.size());
		out.forEach(m -> assertEquals(ScenarioCheckerReason.SOURCE_ID_NOT_UNIQUE, m.getReason()));
	}

	@Test
	@Ignore
	public void TestOverlap(){
		VShape a = new VRectangle(0, 0, 10, 10);
		VShape b = new VRectangle(1, 1, 1, 1);

		Area aa = new Area(a);
		Area bb = new Area(b);
		System.out.println(a.containsShape(b));
		System.out.println(a.containsShape(a));
		System.out.println(b.containsShape(a));
		System.out.println(b.containsShape(b));
	}

	/**
	 * There should be only unique ids
	 */
	@Test
	public void testCheckUniqueSourceIdPositive() {
		builder.addSource(1);
		builder.addSource(2);
		builder.addSource(3);
		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkUniqueSourceId();

		assertEquals("No warnings expected", 0, out.size());
	}

	// Test checkValidTargetsInSource

	@Test
	public void TestCheckValidTargetsInSourceNoIdNoSpawn() {
		AttributesSourceBuilder attrBuilder = AttributesSourceBuilder.anAttributesSource();
		builder.addSource(attrBuilder
				.setTargetIds(new ArrayList<>())
						.setSpawnerAttributes(new AttributesSpawnerBuilder().build(RegularSpawner.class))
				.build()
		);
		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkValidTargetsInSource();

		ScenarioCheckerMessage msg = hasOneElement(out);
		assertEquals(ScenarioCheckerReason.SOURCE_NO_TARGET_ID_NO_SPAWN, msg.getReason());
	}

	@Test
	public void TestCheckValidTargetsInSourceNoId() {
		AttributesSourceBuilder attrBuilder = AttributesSourceBuilder.anAttributesSource();
		builder.addSource(attrBuilder
				.setTargetIds(new ArrayList<>())
						.setSpawnerAttributes(new AttributesSpawnerBuilder().setEventElementCount(5).build(RegularSpawner.class))
				.build()
		);
		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkValidTargetsInSource();

		ScenarioCheckerMessage msg = hasOneElement(out);
		assertEquals(ScenarioCheckerReason.SOURCE_NO_TARGET_ID_SET, msg.getReason());
		isErrorMsg(msg);
	}


	@Test
	public void TestCheckValidTargetsInSourceWrongId() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.setTargetIds(new ArrayList<>(){{add(4);}}) // id not found !
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());

		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkValidTargetsInSource();

		ScenarioCheckerMessage msg = hasOneElement(out);
		assertEquals(ScenarioCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, msg.getReason());
		isErrorMsg(msg);
	}

	@Test
	public void TestCheckValidTargetsInSourceWithSomeWrongId() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.setTargetIds(new ArrayList<>(){{add(1);add(2);add(3);}}) // id 3 not found !
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());
		builder.addTarget(attrTargetB
				.id(3)
				.build());


		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkValidTargetsInSource();

		ScenarioCheckerMessage msg = hasOneElement(out);
		assertEquals(ScenarioCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, msg.getReason());
		isErrorMsg(msg);
		assertEquals("[2]", msg.getReasonModifier());
	}

	@Test
	public void TestCheckValidTargetsInSourceNoError() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.setTargetIds(new ArrayList<>(){{add(1);}})
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());

		Topography topography = builder.build();

		ScenarioChecker checker = new ScenarioChecker(topography);
		PriorityQueue<ScenarioCheckerMessage> out = checker.checkValidTargetsInSource();

		hasNoElement(out);
	}

	// Test checkSourceObstacleOverlap

	@Test
	public void testCheckSourceObstacleOverlapWithNoOverlap(){
		AttributesObstacleBuilder attrObstacleB = AttributesObstacleBuilder.anAttributesObstacle();
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB
				.setVisualBuilder(
						new AttributesVisualElementBuilder()
								.setShape(new VRectangle(0,0,10,10)))
				.build());

		builder.addObstacle(attrObstacleB
				.shape(new VRectangle(15,15,5,5))
				.build());

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkOverlap();

		hasNoElement(out);
	}


	@Test
	public void testCheckSourceObstacleOverlapWithOverlap(){
		AttributesObstacleBuilder attrObstacleB = AttributesObstacleBuilder.anAttributesObstacle();
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB
				.setVisualBuilder(
						new AttributesVisualElementBuilder()
								.setShape(new VRectangle(0,0,10,10)))
				.build());
		Source testSource = (Source)builder.getLastAddedElement();

		builder.addSource(attrSourceB
				.setVisualBuilder(
						new AttributesVisualElementBuilder()
								.setShape(new VRectangle(100,100,10,10)))
				.build());


		builder.addObstacle(attrObstacleB
				.shape(new VCircle(0,0,5.0))
				.build());
		Obstacle testObstacle = (Obstacle) builder.getLastAddedElement();

		builder.addObstacle(attrObstacleB
				.shape(new VRectangle(15,15,5,5))
				.build());

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkOverlap();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(ScenarioCheckerReason.OVERLAP_OBSTACLE_SOURCE, msg.getReason());
		assertEquals(testSource, msg.getMsgTarget().getTargets().get(0));
		assertEquals(testObstacle, msg.getMsgTarget().getTargets().get(1));

	}

	// Test checkUnusedTargets

	@Test
	public void testCheckUnusedTargetsWithNoError(){
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB.setTargetIds(1,2).build());
		builder.addSource(attrSourceB.setTargetIds(3).build());

		builder.addTarget(1);
		builder.addTarget(2);
		builder.addTarget(3);

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkUnusedTargets();

		hasNoElement(out);
	}

	@Test
	public void testCheckUnusedTargetsWithError(){
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB.setTargetIds(1,2).build());

		builder.addTarget(1);
		builder.addTarget(2);
		builder.addTarget(3);

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkUnusedTargets();

		ScenarioCheckerMessage msg = hasOneElement(out);

		isWarnMsg(msg);
		assertEquals(ScenarioCheckerReason.TARGET_UNUSED, msg.getReason());
	}

	// Test checkStairTreadSanity

	@Test
	public void tesCheckStairTreadSanityTreadToBig(){
		AttributesStairsBuilder attrStairsB = AttributesStairsBuilder.anAttributesStairs();

		builder.addStairs(attrStairsB
				.shape(new VRectangle(0,0,10.0,10.0))
				.treadCount(3) // 10m / 3treads = 3.333
				.build());
		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkStairTreadSanity();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(ScenarioCheckerReason.STAIRS_TREAD_DIM_WRONG, msg.getReason());
	}

	@Test
	public void tesCheckStairTreadSanityTreadToSmall(){
		AttributesStairsBuilder attrStairsB = AttributesStairsBuilder.anAttributesStairs();

		builder.addStairs(attrStairsB
				.shape(new VRectangle(0,0,10.0,10.0))
				.treadCount(200) // 10m / 200 treads = 0.050
				.build());
		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkStairTreadSanity();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(ScenarioCheckerReason.STAIRS_TREAD_DIM_WRONG, msg.getReason());
	}

	@Test
	public void tesCheckStairTreadSanityTreadOk(){
		AttributesStairsBuilder attrStairsB = AttributesStairsBuilder.anAttributesStairs();

		builder.addStairs(attrStairsB
				.shape(new VRectangle(0,0,10.0,10.0))
				.treadCount(80) // 10m / 80treads = 0.125
				.build());
		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkStairTreadSanity();
		hasNoElement(out);
	}


	// Test checkPedestrianSpeedSetup

	@Test
	public void testCheckPedestrianSpeedSetupToSmall(){
		AttributesAgentBuilder attrAgentB = AttributesAgentBuilder.anAttributesAgent();

		builder.addPedestrian(attrAgentB
								.minimumSpeed(1.2)
								.maximumSpeed(2.2)
								.speedDistributionMean(0.8)
								.build());

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkPedestrianSpeedSetup();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isErrorMsg(msg);
		assertEquals(ScenarioCheckerReason.PEDESTRIAN_SPEED_SETUP, msg.getReason());
	}

	@Test
	public void testCheckPedestrianSpeedSetupToBig(){
		AttributesAgentBuilder attrAgentB = AttributesAgentBuilder.anAttributesAgent();

		builder.addPedestrian(attrAgentB
				.minimumSpeed(0.5)
				.maximumSpeed(2.2)
				.build());

		// SpeedDistributionMean cannot be set bigger than max speed at construction time.
		Pedestrian p = (Pedestrian) builder.getLastAddedElement();
		p.getAttributes().setSpeedDistributionMean(10.0);

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkPedestrianSpeedSetup();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isErrorMsg(msg);
		assertEquals(ScenarioCheckerReason.PEDESTRIAN_SPEED_SETUP, msg.getReason());
	}


	@Test
	public void testCheckPedestrianSpeedSetupOk(){
		AttributesAgentBuilder attrAgentB = AttributesAgentBuilder.anAttributesAgent();

		builder.addPedestrian(attrAgentB
				.minimumSpeed(0.5)
				.maximumSpeed(2.2)
				.speedDistributionMean(0.8)
				.build());

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkPedestrianSpeedSetup();

		hasNoElement(out);
	}

	@Test
	public void testCheckPedestrianSpeedMinIsWorldRecord(){
		AttributesAgentBuilder attrAgentB = AttributesAgentBuilder.anAttributesAgent();

		builder.addPedestrian(attrAgentB
				.minimumSpeed(13.0)
				.maximumSpeed(17.0)
				.speedDistributionStandardDeviation(2.0)
				.speedDistributionMean(15.0)
				.build());

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkPedestrianSpeedSetup();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(ScenarioCheckerReason.PEDESTRIAN_SPEED_NOT_LOGICAL, msg.getReason());
	}

	@Test
	public void testCheckPedestrianSpeedMaxIsWorldRecord(){
		AttributesAgentBuilder attrAgentB = AttributesAgentBuilder.anAttributesAgent();

		builder.addPedestrian(attrAgentB
				.minimumSpeed(10.0)
				.maximumSpeed(17.0)
				.speedDistributionMean(10.0)
				.build());

		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkPedestrianSpeedSetup();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(ScenarioCheckerReason.PEDESTRIAN_SPEED_NOT_LOGICAL, msg.getReason());
	}

	@Test
	public void testCheckPedestrianSpeedSetupWithNegativSpeed(){
		AttributesAgentBuilder attrAgentB = AttributesAgentBuilder.anAttributesAgent();

		builder.addPedestrian(attrAgentB
				.minimumSpeed(-0.5)
				.maximumSpeed(2.2)
				.speedDistributionMean(0.8)
				.build());


		Topography topography = builder.build();
		ScenarioChecker checker = new ScenarioChecker(topography);

		PriorityQueue<ScenarioCheckerMessage> out = checker.checkPedestrianSpeedSetup();

		ScenarioCheckerMessage msg = hasOneElement(out);
		isErrorMsg(msg);
		assertEquals(ScenarioCheckerReason.PEDESTRIAN_SPEED_NEGATIVE, msg.getReason());
	}

	@Test
	public void testCheckOverlapAllCases(){
		Scenario testScenarioWithErrors = getScenarioFromRelativeResource("ScenarioCheckerTest.scenario");

		ScenarioChecker checker = new ScenarioChecker(testScenarioWithErrors);

		PriorityQueue<ScenarioCheckerMessage>  out = checker.checkOverlap(); //activate all tests.

		assertEquals(22, out.size());
		List<ScenarioCheckerMessage> errorMsg = out.stream()
				.filter(m -> m.getMsgType().equals(ScenarioCheckerMessageType.TOPOGRAPHY_ERROR))
				.collect(Collectors.toList());
		assertEquals(4, errorMsg.size());

		List<ScenarioCheckerMessage> warnMsg = out.stream()
				.filter(m -> m.getMsgType().equals(ScenarioCheckerMessageType.TOPOGRAPHY_WARN))
				.collect(Collectors.toList());
		assertEquals(18, warnMsg.size());

		// Errors
		assertIdAndReason(9,8,ScenarioCheckerReason.OVERLAP_OBSTACLE_TARGET_ERR, errorMsg);
		assertIdAndReason(11,13,ScenarioCheckerReason.OVERLAP_OBSTACLE_STAIRS_ERR, errorMsg);
		assertIdAndReason(35,36,ScenarioCheckerReason.OVERLAP_STAIR_STAIR, errorMsg);
		assertIdAndReason(35,36,ScenarioCheckerReason.OVERLAP_STAIR_STAIR, errorMsg);


		// Warnings
		assertIdAndReason(1,6,ScenarioCheckerReason.OVERLAP_OBSTACLE_SOURCE, warnMsg);
		assertIdAndReason(2,6,ScenarioCheckerReason.OVERLAP_OBSTACLE_SOURCE, warnMsg);
		assertIdAndReason(4,5,ScenarioCheckerReason.OVERLAP_OBSTACLE_OBSTACLE, warnMsg);
		assertIdAndReason(9,7,ScenarioCheckerReason.OVERLAP_OBSTACLE_TARGET_WARN, warnMsg);
		assertIdAndReason(11,12,ScenarioCheckerReason.OVERLAP_OBSTACLE_STAIRS_WARN, warnMsg);
		assertIdAndReason(22,24,ScenarioCheckerReason.OVERLAP_SOURCE_STAIR, warnMsg);
		assertIdAndReason(22,25,ScenarioCheckerReason.OVERLAP_SOURCE_STAIR, warnMsg);
		assertIdAndReason(23,26,ScenarioCheckerReason.OVERLAP_SOURCE_STAIR, warnMsg);
		assertIdAndReason(30,32,ScenarioCheckerReason.OVERLAP_TARGET_STAIR, warnMsg);
		assertIdAndReason(30,33,ScenarioCheckerReason.OVERLAP_TARGET_STAIR, warnMsg);
		assertIdAndReason(31,34,ScenarioCheckerReason.OVERLAP_TARGET_STAIR, warnMsg);
		assertIdAndReason(17,19,ScenarioCheckerReason.OVERLAP_SOURCE_TARGET, warnMsg);
		assertIdAndReason(17,20,ScenarioCheckerReason.OVERLAP_SOURCE_TARGET, warnMsg);
		assertIdAndReason(18,21,ScenarioCheckerReason.OVERLAP_SOURCE_TARGET, warnMsg);
		assertIdAndReason(27,28,ScenarioCheckerReason.OVERLAP_TARGET_TARGET, warnMsg);
		assertIdAndReason(27,29,ScenarioCheckerReason.OVERLAP_TARGET_TARGET, warnMsg);
		assertIdAndReason(14,15,ScenarioCheckerReason.OVERLAP_SOURCE_SOURCE, warnMsg);
		assertIdAndReason(14,16,ScenarioCheckerReason.OVERLAP_SOURCE_SOURCE, warnMsg);
	}

	private void assertIdAndReason(int idA, int idB, ScenarioCheckerReason reason, List<ScenarioCheckerMessage> messages){

		List<ScenarioCheckerMessage> msg = messages.stream()
				.filter(m -> m.isMessageForAllElements(idA, idB) && m.getReason().equals(reason))
				.collect(Collectors.toList());

		assertEquals("expected Message with ids{" + idA + ", " + idB + "} and Reason: "+ reason.toString(), 1, msg.size());
	}

	private ScenarioCheckerMessage hasOneElement(PriorityQueue<ScenarioCheckerMessage> out){
		assertEquals(1, out.size());
		return out.poll();
	}

	private void hasNoElement(PriorityQueue<ScenarioCheckerMessage> out){
		assertEquals(0, out.size());
	}

	private void  isErrorMsg(ScenarioCheckerMessage msg){
		assertEquals(ScenarioCheckerMessageType.TOPOGRAPHY_ERROR, msg.getMsgType());
	}

	private void isWarnMsg(ScenarioCheckerMessage msg){
		assertEquals(ScenarioCheckerMessageType.TOPOGRAPHY_WARN, msg.getMsgType());
	}

	@Override
	public Path getTestDir() {
		return getPathFromResources("/data/ScenarioChecker");
	}
}