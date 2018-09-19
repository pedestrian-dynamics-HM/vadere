package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.builder.AttributesObstacleBuilder;
import org.vadere.state.attributes.scenario.builder.AttributesSourceBuilder;
import org.vadere.state.attributes.scenario.builder.AttributesStairsBuilder;
import org.vadere.state.attributes.scenario.builder.AttributesTargetBuilder;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import static org.junit.Assert.assertEquals;

public class TopographyCheckerTest {

	TopographyTestBuilder builder;

	@Before
	public void setup() {
		builder = new TopographyTestBuilder();
	}

	@Test
	public void testCheckObstacleOverlapHasOverlap() {
		Topography topography = new Topography();

		Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0, 0, 1, 1)));
		Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(0, 0, 1, 1)));

		topography.addObstacle(obs1);
		topography.addObstacle(obs2);

		TopographyChecker topcheck = new TopographyChecker(topography);

		List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

		assertEquals(1, actualList.size());
	}


	@Test
	public void testCheckObstacleOverlapHasNoOverlap() {
		Topography topography = new Topography();

		Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0, 0, 1, 1)));
		Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(1.1, 0, 1, 1)));
		topography.addObstacle(obs1);
		topography.addObstacle(obs2);

		TopographyChecker topcheck = new TopographyChecker(topography);

		List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

		assertEquals(0, actualList.size());
	}

	@Test
	public void testCheckObstacleOverlapReturnsNoOverlapsIfTwoSegmentsTouch() {
		Topography topography = new Topography();

		Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0, 0, 1, 1)));
		Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(1, 0, 1, 1)));
		topography.addObstacle(obs1);
		topography.addObstacle(obs2);

		TopographyChecker topcheck = new TopographyChecker(topography);

		List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

		assertEquals(0, actualList.size());
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

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkUniqueSourceId();

		assertEquals("The sources should have the same id", 3, out.size());
		out.forEach(m -> assertEquals(TopographyCheckerReason.SOURCE_ID_NOT_UNIQUE, m.getReason()));
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

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkUniqueSourceId();

		assertEquals("No warnings expected", 0, out.size());
	}

	// Test checkValidTargetsInSource

	@Test
	public void TestCheckValidTargetsInSourceNoIdNoSpawn() {
		AttributesSourceBuilder attrBuilder = AttributesSourceBuilder.anAttributesSource();
		builder.addSource(attrBuilder
				.spawnNumber(0)
				.targetIds(new ArrayList<>())
				.build()
		);
		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		TopographyCheckerMessage msg = hasOneElement(out);
		assertEquals(TopographyCheckerReason.SOURCE_NO_TARGET_ID_NO_SPAWN, msg.getReason());
	}

	@Test
	public void TestCheckValidTargetsInSourceNoId() {
		AttributesSourceBuilder attrBuilder = AttributesSourceBuilder.anAttributesSource();
		builder.addSource(attrBuilder
				.targetIds(new ArrayList<>())
				.build()
		);
		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		TopographyCheckerMessage msg = hasOneElement(out);
		assertEquals(TopographyCheckerReason.SOURCE_NO_TARGET_ID_SET, msg.getReason());
		isErrorMsg(msg);
	}


	@Test
	public void TestCheckValidTargetsInSourceWrongId() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.targetIds(Collections.singletonList(4)) // id not found !
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());

		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		TopographyCheckerMessage msg = hasOneElement(out);
		assertEquals(TopographyCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, msg.getReason());
		isErrorMsg(msg);
	}

	@Test
	public void TestCheckValidTargetsInSourceWithSomeWrongId() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.targetIds(Arrays.asList(1, 2, 3)) // id 3 not found !
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());
		builder.addTarget(attrTargetB
				.id(3)
				.build());


		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		TopographyCheckerMessage msg = hasOneElement(out);
		assertEquals(TopographyCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, msg.getReason());
		isErrorMsg(msg);
		assertEquals("[2]", msg.getReasonModifier());
	}

	@Test
	public void TestCheckValidTargetsInSourceNoError() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.targetIds(Collections.singletonList(1))
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());

		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		hasNoElement(out);
	}

	// Test checkSourceObstacleOverlap

	@Test
	public void testCheckSourceObstacleOverlapWithNoOverlap(){
		AttributesObstacleBuilder attrObstacleB = AttributesObstacleBuilder.anAttributesObstacle();
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB
				.shape(new VRectangle(0,0,10,10))
				.build());

		builder.addObstacle(attrObstacleB
				.shape(new VRectangle(15,15,5,5))
				.build());

		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkSourceObstacleOverlap();

		hasNoElement(out);
	}


	@Test
	public void testCheckSourceObstacleOverlapWithOverlap(){
		AttributesObstacleBuilder attrObstacleB = AttributesObstacleBuilder.anAttributesObstacle();
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB
				.shape(new VRectangle(0,0,10,10))
				.build());
		Source testSource = (Source)builder.getLastAddedElement();

		builder.addSource(attrSourceB
				.shape(new VRectangle(100,100,10,10))
				.build());


		builder.addObstacle(attrObstacleB
				.shape(new VCircle(0,0,5.0))
				.build());
		Obstacle testObstacle = (Obstacle) builder.getLastAddedElement();

		builder.addObstacle(attrObstacleB
				.shape(new VRectangle(15,15,5,5))
				.build());

		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkSourceObstacleOverlap();

		TopographyCheckerMessage msg = hasOneElement(out);
		isErrorMsg(msg);
		assertEquals(TopographyCheckerReason.SOURCE_OVERLAP_WITH_OBSTACLE, msg.getReason());
		assertEquals(testSource, msg.getMsgTarget().getTargets().get(0));
		assertEquals(testObstacle, msg.getMsgTarget().getTargets().get(1));

	}

	// Test checkUnusedTargets

	@Test
	public void testCheckUnusedTargetsWithNoError(){
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB.targetIds(1,2).build());
		builder.addSource(attrSourceB.targetIds(3).build());

		builder.addTarget(1);
		builder.addTarget(2);
		builder.addTarget(3);

		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkUnusedTargets();

		hasNoElement(out);
	}

	@Test
	public void testCheckUnusedTargetsWithError(){
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB.targetIds(1,2).build());

		builder.addTarget(1);
		builder.addTarget(2);
		builder.addTarget(3);

		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkUnusedTargets();

		TopographyCheckerMessage msg = hasOneElement(out);

		isWarnMsg(msg);
		assertEquals(TopographyCheckerReason.TARGET_UNUSED, msg.getReason());
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
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkStairTreadSanity();

		TopographyCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(TopographyCheckerReason.STAIRS_TREAD_DIM_WRONG, msg.getReason());
	}

	@Test
	public void tesCheckStairTreadSanityTreadToSmall(){
		AttributesStairsBuilder attrStairsB = AttributesStairsBuilder.anAttributesStairs();

		builder.addStairs(attrStairsB
				.shape(new VRectangle(0,0,10.0,10.0))
				.treadCount(200) // 10m / 200 treads = 0.050
				.build());
		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkStairTreadSanity();

		TopographyCheckerMessage msg = hasOneElement(out);
		isWarnMsg(msg);
		assertEquals(TopographyCheckerReason.STAIRS_TREAD_DIM_WRONG, msg.getReason());
	}

	@Test
	public void tesCheckStairTreadSanityTreadOk(){
		AttributesStairsBuilder attrStairsB = AttributesStairsBuilder.anAttributesStairs();

		builder.addStairs(attrStairsB
				.shape(new VRectangle(0,0,10.0,10.0))
				.treadCount(80) // 10m / 80treads = 0.125
				.build());
		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkStairTreadSanity();
		hasNoElement(out);
	}

	private TopographyCheckerMessage hasOneElement(List<TopographyCheckerMessage> out){
		assertEquals(1, out.size());
		return out.get(0);
	}

	private void hasNoElement(List<TopographyCheckerMessage> out){
		assertEquals(0, out.size());
	}

	private void  isErrorMsg(TopographyCheckerMessage msg){
		assertEquals(TopographyCheckerMessageType.ERROR, msg.getMsgType());
	}

	private void isWarnMsg(TopographyCheckerMessage msg){
		assertEquals(TopographyCheckerMessageType.WARN, msg.getMsgType());
	}

}