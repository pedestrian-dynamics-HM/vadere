package org.vadere.simulator.utils.scenariochecker.checks.topography;

import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;
import java.util.PriorityQueue;

public class ObstacleAreaCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {
        List<Obstacle> obstacleList = topography.getObstacles();
        obstacleList.forEach(obstacle -> {
            if(shapeHasNoArea(obstacle.getShape())){
                messages.add(msgBuilder.topographyError().target(obstacle).reason(ScenarioCheckerReason.OBSTACLE_NO_AREA).build());
            }
        });
        return messages;
    }
    private boolean shapeHasNoArea(VShape shape) {
        List<VPoint> points = shape.getPath();
        VPoint refPoint = points.get(0);
        for ( int i = 1; i < points.size();i++){
            if(!refPoint.equals(points.get(i))) {
                return false;
            }
        }
        return true;
    }
}
