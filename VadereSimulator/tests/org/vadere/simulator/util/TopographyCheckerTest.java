package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.List;

import javax.swing.*;

import static org.junit.Assert.assertEquals;

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
    public void tset(){
        MsgDocument doc = new MsgDocument();
        doc.setContentType("text/html");
        doc.setText("File not found please contact:<a href='element/0023'>e-mail to</a> or call 963");
        doc.addHyperlinkListener( e -> {
            System.out.println(e.getURL());
        });

    }

    class MsgDocument extends JTextPane {

        public MsgDocument(){

        }


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