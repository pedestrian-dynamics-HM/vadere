package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TopographyChecker {
	private final Topography topography;
	private TopographyCheckerMessageBuilder msgBuilder;

	public TopographyChecker(@NotNull final Topography topography) {
		this.topography = topography;
		this.msgBuilder = new TopographyCheckerMessageBuilder();
	}

	public List<Pair<Obstacle, Obstacle>> checkObstacleOverlap() {
		List<Pair<Obstacle, Obstacle>> intersectList = new ArrayList<>();
		for (int i = 0; i < topography.getObstacles().size(); i++) {
			for (int j = i + 1; j < topography.getObstacles().size(); j++) {
				Obstacle obs1 = topography.getObstacles().get(i);
				Obstacle obs2 = topography.getObstacles().get(j);
				if (obs1.getShape().intersects(obs2.getShape())) {
					intersectList.add(Pair.create(obs1, obs2));
				}
			}
		}
		return intersectList;
	}


	public boolean hasObstacleOverlaps() {
		return checkObstacleOverlap().size() > 0;
	}

	public List<TopographyCheckerMessage> checkOverlap(BiFunction<ScenarioElementType, ScenarioElementType, Boolean> useTest) {
		List<TopographyCheckerMessage> ret = new ArrayList<>();

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


	private void handelObstacleObstacleOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		if (e1.enclosesScenarioElement(e2)) {
			ret.add(msgBuilder.warning().target(e1, e2)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_OBSTACLE).build());
		} else if (e2.enclosesScenarioElement(e1)){
			ret.add(msgBuilder.warning().target(e2, e1)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_OBSTACLE).build());
		} else if (e1.totalOverlapWith(e2)){
			ret.add(msgBuilder.warning().target(e2, e1)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_OBSTACLE).build());
		}
	}

	private void handelObstacleSourceOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		Obstacle obstacle = (Obstacle) elementOfType(e1, e2, ScenarioElementType.OBSTACLE);
		Source source  = (Source) elementOfType(e1, e2, ScenarioElementType.SOURCE);

		if (source.overlapWith(obstacle)) {
			ret.add(msgBuilder.error().target(source, obstacle)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_SOURCE).build());
		}
	}

	private void handelObstacleTargetOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		Obstacle obstacle = (Obstacle) elementOfType(e1, e2, ScenarioElementType.OBSTACLE);
		Target target = (Target) elementOfType(e1, e2, ScenarioElementType.TARGET);

		if (obstacle.enclosesScenarioElement(target)) {
			ret.add(msgBuilder.error().target(obstacle, target)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_TARGET_ERR).build());
		}else if (obstacle.overlapWith(target)) {
			ret.add(msgBuilder.warning().target(obstacle, target)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_TARGET_ERR).build());
		}
	}

	private void handelObstacleStairsOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		Obstacle obstacle = (Obstacle) elementOfType(e1, e2, ScenarioElementType.OBSTACLE);
		Stairs stairs = (Stairs) elementOfType(e1, e2, ScenarioElementType.STAIRS);

		if (obstacle.enclosesScenarioElement(stairs)) {
			ret.add(msgBuilder.error().target(obstacle, stairs)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_STAIRS_ERR).build());
		} else if (obstacle.overlapWith(stairs)) {
			ret.add(msgBuilder.warning().target(obstacle, stairs)
					.reason(TopographyCheckerReason.OVERLAP_OBSTACLE_STAIRS_WARN).build());
		}

	}

	private void handelSourceSourceOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		if (e1.overlapWith(e2)) {
			ret.add(msgBuilder.warning().target(e1, e2)
					.reason(TopographyCheckerReason.OVERLAP_SOURCE_SOURCE).build());
		}
	}


	private void handelSourceTargetOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		Source source= (Source) elementOfType(e1, e2, ScenarioElementType.SOURCE);
		Target target = (Target) elementOfType(e1, e2, ScenarioElementType.TARGET);

		if (source.overlapWith(target)) {
			ret.add(msgBuilder.warning().target(source, target)
					.reason(TopographyCheckerReason.OVERLAP_SOURCE_TARGET).build());
		}
	}

	private void handelSourceStairsOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		Source source = (Source) elementOfType(e1, e2, ScenarioElementType.SOURCE);
		Stairs stairs = (Stairs) elementOfType(e1, e2, ScenarioElementType.STAIRS);

		if (source.overlapWith(stairs)) {
			ret.add(msgBuilder.warning().target(source, stairs)
					.reason(TopographyCheckerReason.OVERLAP_SOURCE_STAIR).build());
		}
	}

	private void handelTargetTargetOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		//todo
	}

	// WARN total overlap == partial overlap
	private void handelTargetStairsOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		Target target = (Target) elementOfType(e1, e2, ScenarioElementType.TARGET);
		Stairs stairs = (Stairs) elementOfType(e1, e2, ScenarioElementType.STAIRS);

		if (target.overlapWith(stairs)) {
			ret.add(msgBuilder.warning().target(target, stairs)
					.reason(TopographyCheckerReason.OVERLAP_TARGET_STAIR).build());
		}
	}

	// ERROR total overlap == partial overlap
	private void handelStairStairOverlap(ScenarioElement e1, ScenarioElement e2, List<TopographyCheckerMessage> ret) {
		if (e1.overlapWith(e2)) {
			ret.add(msgBuilder.error().target(e1, e2)
					.reason(TopographyCheckerReason.OVERLAP_STAIR_STAIR).build());
		}
	}


	public List<TopographyCheckerMessage> checkBuildingStep() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		ret.addAll(checkValidTargetsInSource());
		ret.addAll(checkUniqueSourceId());
		ret.addAll(checkUnusedTargets());
		ret.addAll(checkStairTreadSanity());
		ret.addAll(checkPedestrianSpeedSetup());
		ret.addAll(checkOverlap((type, type2) -> true));
		return ret;
	}

	public List<TopographyCheckerMessage> checkPedestrianSpeedSetup() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		topography.getPedestrianDynamicElements().getInitialElements().forEach(p -> {
			double speedMean = p.getSpeedDistributionMean();
			if (speedMean < p.getAttributes().getMinimumSpeed() || speedMean > p.getAttributes().getMaximumSpeed()) {
				ret.add(msgBuilder
						.error()
						.target(p)
						.reason(TopographyCheckerReason.PEDESTRIAN_SPEED_SETUP,
								"(" + p.getAttributes().getMinimumSpeed()
										+ "  &lt; treadDepth  &lt; "
										+ p.getAttributes().getMaximumSpeed() +
										") current SpeedDistributionMean is: " + String.format("%.2f", speedMean))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() > Pedestrian.HUMAN_MAX_SPEED || p.getAttributes().getMaximumSpeed() > Pedestrian.HUMAN_MAX_SPEED) {
				ret.add(msgBuilder
						.warning()
						.target(p)
						.reason(TopographyCheckerReason.PEDESTRIAN_SPEED_NOT_LOGICAL,
								String.format("[max: %.2f min: %.2f threshold: %.2f]", p.getAttributes().getMinimumSpeed(),
										p.getAttributes().getMaximumSpeed(), Pedestrian.HUMAN_MAX_SPEED))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() < 0 || p.getAttributes().getMaximumSpeed() < 0) {
				ret.add(msgBuilder
						.error()
						.target(p)
						.reason(TopographyCheckerReason.PEDESTRIAN_SPEED_NEGATIVE)
						.build());
			}
		});

		return ret;
	}

	public List<TopographyCheckerMessage> checkStairTreadSanity() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		topography.getStairs().forEach(stairs -> {
			double treadDepth = stairs.getTreadDepth();
			if (treadDepth < Stairs.MIN_TREAD_DEPTH || treadDepth > Stairs.MAX_TREAD_DEPTH) {
				ret.add(msgBuilder
						.warning()
						.target(stairs)
						.reason(TopographyCheckerReason.STAIRS_TREAD_DIM_WRONG
								, "(" + Stairs.MIN_TREAD_DEPTH + "m  &lt; treadDepth  &gt; " + Stairs.MAX_TREAD_DEPTH +
										"m) current treadDepth is: " + String.format("%.3fm", stairs.getTreadDepth()))
						.build()
				);
			}
		});

		return ret;
	}


	public List<TopographyCheckerMessage> checkUnusedTargets() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		Set<Integer> usedTargetIds = new HashSet<>();
		topography.getSources()
				.forEach(s -> usedTargetIds.addAll(s.getAttributes().getTargetIds()));

		topography.getTargets().forEach(t -> {
			if (!usedTargetIds.contains(t.getId())) {
				ret.add(msgBuilder
						.warning()
						.reason(TopographyCheckerReason.TARGET_UNUSED)
						.target(t)
						.build());
			}
		});

		return ret;
	}


	public List<TopographyCheckerMessage> checkValidTargetsInSource() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		Set<Integer> targetIds = topography.getTargets().stream()
				.map(Target::getId)
				.collect(Collectors.toSet());

		for (Source s : topography.getSources()) {
			if (s.getAttributes().getTargetIds().size() == 0) {
				if (s.getAttributes().getSpawnNumber() == 0) {
					ret.add(msgBuilder
							.warning()
							.target(s)
							.reason(TopographyCheckerReason.SOURCE_NO_TARGET_ID_NO_SPAWN)
							.build());
				} else {
					ret.add(msgBuilder.error()
							.target(s)
							.reason(TopographyCheckerReason.SOURCE_NO_TARGET_ID_SET)
							.build());
				}
			} else {
				List<String> notFoundTargetIds = s.getAttributes().getTargetIds().stream()
						.filter(tId -> !targetIds.contains(tId))
						.map(tId -> Integer.toString(tId))
						.collect(Collectors.toList());
				if (notFoundTargetIds.size() > 0) {
					StringBuilder sj = new StringBuilder();
					sj.append("[");
					notFoundTargetIds.forEach(i -> sj.append(i).append(", "));
					sj.setLength(sj.length() - 2);
					sj.append("]");
					ret.add(msgBuilder.error()
							.target(s)
							.reason(TopographyCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, sj.toString())
							.build());
				}
			}
		}

		return ret;
	}

	public List<TopographyCheckerMessage> checkUniqueSourceId() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		Set<Integer> sourceId = new HashSet<>();

		for (Source s : topography.getSources()) {
			if (!sourceId.add(s.getId())) {
				ret.add(msgBuilder.warning()
						.target(s)
						.reason(TopographyCheckerReason.SOURCE_ID_NOT_UNIQUE)
						.build());
			}
		}
		return ret;
	}


	public void doCreatorChecks() {

	}
}
