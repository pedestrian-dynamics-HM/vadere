package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

	public List<TopographyCheckerMessage> checkSourceObstacleOverlap(){
		List<TopographyCheckerMessage> ret = new ArrayList<>();
    	final List<Source> sources = topography.getSources();
    	final List<Obstacle> obstacles = topography.getObstacles();

		for (Obstacle obstacle : obstacles) {
			for(Source source: sources){
				if (obstacle.getShape().intersects(source.getShape())){
					ret.add(msgBuilder.error()
							.reason(TopographyCheckerReason.SOURCE_OVERLAP_WITH_OBSTACLE)
							.target(source, obstacle)
							.build());
				}
			}
		}
		return ret;
	}


    public List<TopographyCheckerMessage> checkBuildingStep(){
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		ret.addAll(checkValidTargetsInSource());
		ret.addAll(checkUniqueSourceId());
		ret.addAll(checkSourceObstacleOverlap());
		ret.addAll(checkUnusedTargets());
		ret.addAll(checkStairTreadSanity());
		ret.addAll(checkPedestrianSpeedSetup());
		return ret;
	}

	public List<TopographyCheckerMessage> checkPedestrianSpeedSetup() {
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		topography.getPedestrianDynamicElements().getInitialElements().forEach(p -> {
			double speedMean = p.getSpeedDistributionMean();
			if (speedMean < p.getAttributes().getMinimumSpeed() || speedMean > p.getAttributes().getMaximumSpeed()){
				ret.add(msgBuilder
						.error()
						.target(p)
						.reason(TopographyCheckerReason.PEDESTRIAN_SPEED_SETUP,
								"(" + p.getAttributes().getMinimumSpeed()
										+ "  &lt; treadDepth  &lt; "
										+ p.getAttributes().getMaximumSpeed() +
								") current SpeedDistributionMean is: " + String.format("%.2f",speedMean))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() > Pedestrian.HUMAN_MAX_SPEED || p.getAttributes().getMaximumSpeed() > Pedestrian.HUMAN_MAX_SPEED){
				ret.add(msgBuilder
						.warning()
						.target(p)
						.reason(TopographyCheckerReason.PEDESTRIAN_SPEED_NOT_LOGICAL,
								String.format("[max: %.2f min: %.2f threshold: %.2f]", p.getAttributes().getMinimumSpeed(),
										p.getAttributes().getMaximumSpeed(), Pedestrian.HUMAN_MAX_SPEED))
						.build());
			}
			if (p.getAttributes().getMinimumSpeed() < 0 || p.getAttributes().getMaximumSpeed() < 0){
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
		topography.getStairs().forEach( stairs -> {
			double treadDepth = stairs.getTreadDepth();
			if (treadDepth < Stairs.MIN_TREAD_DEPTH || treadDepth > Stairs.MAX_TREAD_DEPTH){
				ret.add(msgBuilder
						.warning()
						.target(stairs)
						.reason(TopographyCheckerReason.STAIRS_TREAD_DIM_WRONG
								, "(" + Stairs.MIN_TREAD_DEPTH + "m  &lt; treadDepth  &gt; " + Stairs.MAX_TREAD_DEPTH +
										"m) current treadDepth is: " + String.format("%.3fm",stairs.getTreadDepth()))
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
			if (!usedTargetIds.contains(t.getId())){
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

		for(Source s : topography.getSources()){
			if (s.getAttributes().getTargetIds().size() == 0){
				if (s.getAttributes().getSpawnNumber() == 0){
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
				List<String> notFoundTargetIds =  s.getAttributes().getTargetIds().stream()
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

		for(Source s : topography.getSources()){
			if (!sourceId.add(s.getId())){
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
