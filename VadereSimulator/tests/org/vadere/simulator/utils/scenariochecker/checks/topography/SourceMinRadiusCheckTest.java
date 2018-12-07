package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.junit.Test;
import org.vadere.simulator.utils.TopographyTestBuilder;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.PriorityQueue;

import static org.junit.Assert.*;

public class SourceMinRadiusCheckTest {


	@Test
	public void TestSourceMinRadiusPositive() {
		SourceMinRadiusCheck sourceMinRadiusCheck = new SourceMinRadiusCheck();
		TopographyTestBuilder builder = new TopographyTestBuilder();
		builder.addSource(1, new VCircle(10.0, 10.0, 0.4));
		builder.addSource(2, new VRectangle(5.0, 5.0, 1.0, 1.0));
		PriorityQueue<ScenarioCheckerMessage> ret = sourceMinRadiusCheck.runScenarioCheckerTest(builder.build());

		assertEquals(1, ret.size());
		assertEquals(ScenarioCheckerReason.SOURCE_TO_SMALL, ret.poll().getReason());

	}

	@Test
	public void TestSourceMinRadiusNegative() {
		SourceMinRadiusCheck sourceMinRadiusCheck = new SourceMinRadiusCheck();
		TopographyTestBuilder builder = new TopographyTestBuilder();
		builder.addSource(2, new VRectangle(5.0, 5.0, 0.4, 0.4));
		PriorityQueue<ScenarioCheckerMessage> ret = sourceMinRadiusCheck.runScenarioCheckerTest(builder.build());

		assertEquals(0, ret.size());
	}

	@Test
	public void TestSourceMinRadiusPositive2() {
		SourceMinRadiusCheck sourceMinRadiusCheck = new SourceMinRadiusCheck();
		TopographyTestBuilder builder = new TopographyTestBuilder();
		builder.addSource(1, new VCircle(10.0, 10.0, 4.0));
		builder.addSource(2, new VRectangle(5.0, 5.0, 1.0, 1.0));
		PriorityQueue<ScenarioCheckerMessage> ret = sourceMinRadiusCheck.runScenarioCheckerTest(builder.build());

		assertEquals(0, ret.size());

	}

}