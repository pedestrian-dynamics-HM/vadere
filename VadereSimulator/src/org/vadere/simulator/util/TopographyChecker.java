package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TopographyChecker {
    private final Topography topography;

    public TopographyChecker(@NotNull final Topography topography) {
        this.topography = topography;
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

    public List<Source> getSourceWithoutTarget() {
        return topography.getSources().stream()
                .filter(s -> s.getAttributes().getTargetIds().size() == 0)
                .collect(Collectors.toList());
    }


}
