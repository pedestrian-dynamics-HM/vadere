package org.vadere.simulator.utils.scenariochecker.checks.topography;

import com.github.davidmoten.rtree.geometry.Rectangle;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.simulator.utils.scenariochecker.checks.TopographyCheckerTest;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class ObstacleRepulsionCheck extends AbstractScenarioCheck implements TopographyCheckerTest {
    // minimal corridor width at  |gradient(target potential)| < |gradient(wall repulsion)| -> agents might get stuck
    private double threshold = 1.2755;

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Topography topography) {


        PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();

        List<Rectangle> shapes = topography.getObstacleShapes().stream().map(s -> {
            try {
                VRectangle shape = (VRectangle) s;
                return shape.mbr();
            } catch (ClassCastException ignored) {
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        int counter = 0;
        while (shapes.size() > 1) {

            Rectangle vShape = shapes.get(0);
            shapes.remove(0);

            List<Double> distances = shapes.stream().map(shape -> shape.distance(vShape)).collect(Collectors.toList());
            distances = distances.stream().map(d -> {
                if (d <= Double.MIN_VALUE) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    return d;
                }
            }).collect(Collectors.toList());

            int index = 1;
            for (Double distMin : distances) {
                if (distMin < threshold) {
                    String msg = " Distance between obstacles =" + String.format("%.2f", distMin) +
                            ". If this leads to a bottleneck width < " +
                            String.format("%.2f", threshold) +
                            ", agents might get stuck." +
                            " Consider increasing targetAttractionStrength.";
                    ret.add(msgBuilder
                            .topographyWarning()
                            .target(topography.getObstacles().get(counter), topography.getObstacles().get(counter + index))
                            .reason(ScenarioCheckerReason.NARROW_BOTTLENECK, msg ).build());

                }
                index++;

            }

            counter++;
        }


        return ret;
    }

}