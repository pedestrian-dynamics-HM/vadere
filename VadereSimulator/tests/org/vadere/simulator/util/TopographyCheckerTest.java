package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;

import java.util.List;

import static org.junit.Assert.*;

public class TopographyCheckerTest {

    @Test
    public void testCheckObstacleOverlapHasOverlap(){
        Topography topography = new Topography();

        Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0,0,1,1)));
        Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(0,0,1,1)));
        topography.addObstacle(obs1);
        topography.addObstacle(obs2);

        TopographyChecker topcheck = new TopographyChecker(topography);

        List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

        assertEquals(1, actualList.size());
    }

    @Test
    public void testCheckObstacleOverlapHasNoOverlap(){
        Topography topography = new Topography();

        Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0,0,1,1)));
        Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(1.1,0,1,1)));
        topography.addObstacle(obs1);
        topography.addObstacle(obs2);

        TopographyChecker topcheck = new TopographyChecker(topography);

        List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

        assertEquals(0, actualList.size());
    }

    @Test
    public void testCheckObstacleOverlapReturnsNoOverlapsIfTwoSegmentsTouch(){
        Topography topography = new Topography();

        Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0,0,1,1)));
        Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(1,0,1,1)));
        topography.addObstacle(obs1);
        topography.addObstacle(obs2);

        TopographyChecker topcheck = new TopographyChecker(topography);

        List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

        assertEquals(0, actualList.size());
    }
}