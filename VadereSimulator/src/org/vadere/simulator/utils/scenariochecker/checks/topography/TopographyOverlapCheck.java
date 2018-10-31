package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.function.BiFunction;

public class TopographyOverlapCheck extends AbstractScenarioCheck implements TopographyCheckerTest {


	private BiFunction<ScenarioElementType, ScenarioElementType, Boolean> useTest;

	public TopographyOverlapCheck(){
		useTest = (type, type2) -> true; // activate all test.
	}

	@Override
	public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
		PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();

		ArrayList<ScenarioElement> all = topography.getAllScenarioElements();
		for (int i = 0; i < all.size(); i++) {
			for (int j = i + 1; j < all.size(); j++) {
				ScenarioElement elementA = all.get(i);
				ScenarioElement elementB = all.get(j);

				// same Type complete overlap
				if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.OBSTACLE, ScenarioElementType.OBSTACLE)) {

					handelObstacleObstacleOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.OBSTACLE, ScenarioElementType.SOURCE)) {
					handelObstacleSourceOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.OBSTACLE, ScenarioElementType.TARGET)) {

					handelObstacleTargetOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.OBSTACLE, ScenarioElementType.STAIRS)) {

					handelObstacleStairsOverlap(elementA, elementB, ret);


				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.SOURCE, ScenarioElementType.SOURCE)) {

					handelSourceSourceOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.SOURCE, ScenarioElementType.TARGET)) {

					handelSourceTargetOverlap(elementA, elementB, ret);


				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.SOURCE, ScenarioElementType.STAIRS)) {

					handelSourceStairsOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.TARGET, ScenarioElementType.TARGET)) {

					handelTargetTargetOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.TARGET, ScenarioElementType.STAIRS)) {

					handelTargetStairsOverlap(elementA, elementB, ret);

				} else if (isTestActive(useTest, elementA, elementB,
						ScenarioElementType.STAIRS, ScenarioElementType.STAIRS)) {

					handelStairStairOverlap(elementA, elementB, ret);
				}

			}
		}
		return ret;
	}


	private boolean isTestActive(BiFunction<ScenarioElementType, ScenarioElementType, Boolean> checkTest,
								 ScenarioElement eA, ScenarioElement eB,
								 ScenarioElementType type1, ScenarioElementType type2) {
		return (checkTest.apply(type1, type2) &&
				((eA.getType().equals(type1) && eB.getType().equals(type2)) ||
						(eA.getType().equals(type2) && eB.getType().equals(type1)))
		);

	}

	private ScenarioElement elementOfType(ScenarioElement e1, ScenarioElement e2, ScenarioElementType type) {
		return e1.getType().equals(type) ? e1 : e2;
	}


	private void handelObstacleObstacleOverlap(ScenarioElement e1, ScenarioElement e2,
											   PriorityQueue<ScenarioCheckerMessage> ret) {

		if (e1.enclosesScenarioElement(e2)) {
			ret.add(msgBuilder.topographyWarning().target(e1, e2)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_OBSTACLE).build());
		} else if (e2.enclosesScenarioElement(e1)){
			ret.add(msgBuilder.topographyWarning().target(e2, e1)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_OBSTACLE).build());
		} else if (e1.totalOverlapWith(e2)){
			ret.add(msgBuilder.topographyWarning().target(e2, e1)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_OBSTACLE).build());
		}
	}

	private void handelObstacleSourceOverlap(ScenarioElement e1, ScenarioElement e2,
											 PriorityQueue<ScenarioCheckerMessage> ret) {
		Obstacle obstacle = (Obstacle) elementOfType(e1, e2, ScenarioElementType.OBSTACLE);
		Source source  = (Source) elementOfType(e1, e2, ScenarioElementType.SOURCE);

		if (source.overlapWith(obstacle)) {
			ret.add(msgBuilder.topographyWarning().target(source, obstacle)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_SOURCE).build());
		}
	}

	private void handelObstacleTargetOverlap(ScenarioElement e1, ScenarioElement e2,
											 PriorityQueue<ScenarioCheckerMessage> ret) {

		Obstacle obstacle = (Obstacle) elementOfType(e1, e2, ScenarioElementType.OBSTACLE);
		Target target = (Target) elementOfType(e1, e2, ScenarioElementType.TARGET);

		if (obstacle.enclosesScenarioElement(target)) {
			ret.add(msgBuilder.topographyError().target(obstacle, target)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_TARGET_ERR).build());
		}else if (obstacle.overlapWith(target)) {
			ret.add(msgBuilder.topographyWarning().target(obstacle, target)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_TARGET_WARN).build());
		}
	}

	private void handelObstacleStairsOverlap(ScenarioElement e1, ScenarioElement e2,
											 PriorityQueue<ScenarioCheckerMessage> ret) {

		Obstacle obstacle = (Obstacle) elementOfType(e1, e2, ScenarioElementType.OBSTACLE);
		Stairs stairs = (Stairs) elementOfType(e1, e2, ScenarioElementType.STAIRS);

		if (obstacle.enclosesScenarioElement(stairs)) {
			ret.add(msgBuilder.topographyError().target(obstacle, stairs)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_STAIRS_ERR).build());
		} else if (obstacle.overlapWith(stairs)) {
			ret.add(msgBuilder.topographyWarning().target(obstacle, stairs)
					.reason(ScenarioCheckerReason.OVERLAP_OBSTACLE_STAIRS_WARN).build());
		}

	}

	private void handelSourceSourceOverlap(ScenarioElement e1, ScenarioElement e2,
										   PriorityQueue<ScenarioCheckerMessage> ret) {
		if (e1.overlapWith(e2)) {
			ret.add(msgBuilder.topographyWarning().target(e1, e2)
					.reason(ScenarioCheckerReason.OVERLAP_SOURCE_SOURCE).build());
		}
	}


	private void handelSourceTargetOverlap(ScenarioElement e1, ScenarioElement e2,
										   PriorityQueue<ScenarioCheckerMessage> ret) {

		Source source= (Source) elementOfType(e1, e2, ScenarioElementType.SOURCE);
		Target target = (Target) elementOfType(e1, e2, ScenarioElementType.TARGET);

		if (source.overlapWith(target)) {
			ret.add(msgBuilder.topographyWarning().target(source, target)
					.reason(ScenarioCheckerReason.OVERLAP_SOURCE_TARGET).build());
		}
	}

	private void handelSourceStairsOverlap(ScenarioElement e1, ScenarioElement e2,
										   PriorityQueue<ScenarioCheckerMessage> ret) {

		Source source = (Source) elementOfType(e1, e2, ScenarioElementType.SOURCE);
		Stairs stairs = (Stairs) elementOfType(e1, e2, ScenarioElementType.STAIRS);

		if (source.overlapWith(stairs)) {
			ret.add(msgBuilder.topographyWarning().target(source, stairs)
					.reason(ScenarioCheckerReason.OVERLAP_SOURCE_STAIR).build());
		}
	}

	private void handelTargetTargetOverlap(ScenarioElement e1, ScenarioElement e2,
										   PriorityQueue<ScenarioCheckerMessage> ret) {
		if (e1.overlapWith(e2)) {
			ret.add(msgBuilder.topographyWarning().target(e1, e2)
					.reason(ScenarioCheckerReason.OVERLAP_TARGET_TARGET).build());
		}
	}

	// TOPOGRAPHY_WARN total overlap == partial overlap
	private void handelTargetStairsOverlap(ScenarioElement e1, ScenarioElement e2,
										   PriorityQueue<ScenarioCheckerMessage> ret) {

		Target target = (Target) elementOfType(e1, e2, ScenarioElementType.TARGET);
		Stairs stairs = (Stairs) elementOfType(e1, e2, ScenarioElementType.STAIRS);

		if (target.overlapWith(stairs)) {
			ret.add(msgBuilder.topographyWarning().target(target, stairs)
					.reason(ScenarioCheckerReason.OVERLAP_TARGET_STAIR).build());
		}
	}

	// TOPOGRAPHY_ERROR total overlap == partial overlap
	private void handelStairStairOverlap(ScenarioElement e1, ScenarioElement e2,
										 PriorityQueue<ScenarioCheckerMessage> ret) {
		if (e1.overlapWith(e2)) {
			ret.add(msgBuilder.topographyError().target(e1, e2)
					.reason(ScenarioCheckerReason.OVERLAP_STAIR_STAIR).build());
		}
	}

}
