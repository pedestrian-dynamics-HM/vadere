package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.junit.Test;
import org.vadere.simulator.utils.TopographyTestBuilder;
import org.vadere.simulator.utils.scenariochecker.ScenarioChecker;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessageType;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.PriorityQueue;

import static org.junit.Assert.*;

public class TopographyOffsetCheckTest {

	@Test
	public void runScenarioCheckerTestNoWarning() {
		Topography topography = new Topography();
		topography.getAttributes().setBounds(new VRectangle(0.0, 0.0 , 10 ,12));

		TopographyOffsetCheck check = new TopographyOffsetCheck();
		PriorityQueue<ScenarioCheckerMessage> ret = check.runScenarioCheckerTest(topography);

		assertEquals(0, ret.size());

	}

	@Test
	public void runScenarioCheckerTestWarning1() {
		Topography topography = new Topography();
		topography.getAttributes().setBounds(new VRectangle(0.1, 0.0 , 10 ,12));

		TopographyOffsetCheck check = new TopographyOffsetCheck();
		PriorityQueue<ScenarioCheckerMessage> ret = check.runScenarioCheckerTest(topography);

		assertEquals(1, ret.size());
		assertEquals(ScenarioCheckerReason.TOPOGRAPHY_OFFSET, ret.poll().getReason());

	}

	@Test
	public void runScenarioCheckerTestWarning2() {
		Topography topography = new Topography();
		topography.getAttributes().setBounds(new VRectangle(0.0, 0.1 , 10 ,12));

		TopographyOffsetCheck check = new TopographyOffsetCheck();
		PriorityQueue<ScenarioCheckerMessage> ret = check.runScenarioCheckerTest(topography);

		assertEquals(1, ret.size());
		assertEquals(ScenarioCheckerReason.TOPOGRAPHY_OFFSET, ret.poll().getReason());
	}

	@Test
	public void runScenarioCheckerTestWarning3() {
		Topography topography = new Topography();
		topography.getAttributes().setBounds(new VRectangle(500000, 500000 , 10 ,12));

		TopographyOffsetCheck check = new TopographyOffsetCheck();
		PriorityQueue<ScenarioCheckerMessage> ret = check.runScenarioCheckerTest(topography);

		assertEquals(1, ret.size());
		assertEquals(ScenarioCheckerReason.TOPOGRAPHY_OFFSET, ret.poll().getReason());

	}
}