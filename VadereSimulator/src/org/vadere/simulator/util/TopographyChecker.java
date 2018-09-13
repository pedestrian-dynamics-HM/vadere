package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.StringJoiner;
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

    public List<TopographyCheckerMessage> checkBuildingStep(){
		List<TopographyCheckerMessage> ret = new ArrayList<>();
		ret.addAll(checkValidTargetsInSource());
		ret.addAll(checkUniqueSourceId());
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
							.element(s)
							.reason(TopographyCheckerReason.SOURCE_NO_TARGET_ID_NO_SPAWN)
							.build());
				} else {
					ret.add(msgBuilder.error()
							.element(s)
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
							.element(s)
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
						.element(s)
						.reason(TopographyCheckerReason.SOURCE_ID_NOT_UNIQUE)
						.build());
			}
		}
		return ret;
	}


    public void doCreatorChecks() {

    }
}
