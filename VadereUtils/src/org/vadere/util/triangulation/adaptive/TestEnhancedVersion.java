package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.*;

import java.util.*;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion extends JFrame {

    ArrayList<VShape> obstacles;

    private TestEnhancedVersion()
    {
//        VRectangle bbox = new VRectangle(0,0,100,100);
//        ArrayList<VRectangle> obs = new ArrayList<VRectangle>() {{ add(new VRectangle(20,20,20,20));}};
        double h0 = 3.0;

        long now = System.currentTimeMillis();

        VRectangle bbox = new VRectangle(0, 0, 300, 300);
//        Path2D.Double test = new Path2D.Double();
//        test.moveTo(30,30);
//        test.lineTo(90,80);
//        test.lineTo(70,90);
//        test.lineTo(50,80);
//        test.lineTo(35,65);
//        test.lineTo(30,30);
//        VPolygon p = new VPolygon(test);

        double height = 300;
        double width = 300;

        java.util.List<VShape> boundingBox = new ArrayList<VShape>() {{
            add(new VRectangle(0, 0, 5, width));
            add(new VRectangle(0, 0, width, 5));
            add(new VRectangle(width-5, 0, 5, height));
            add(new VRectangle(0, height-5, 5, height));
        }};

        PSDistmesh meshGenerator = new PSDistmesh(bbox, boundingBox, h0,false);

        System.out.println(System.currentTimeMillis()-now);
        now = System.currentTimeMillis();
        System.out.println(System.currentTimeMillis()-now);
        PSDistmeshPanel distmeshPanel = new PSDistmeshPanel(meshGenerator, face -> meshGenerator.getTriangulation().getMesh().toTriangle(face).isNonAcute(), 1000, 800);
        JFrame frame = distmeshPanel.display();
        frame.setVisible(true);
        double quality = meshGenerator.qualityCheck();

        while (quality < 0.95) {
            System.out.println("quality:"+ quality);
            meshGenerator.step();
	        /*try {
		        Thread.sleep(5000);
	        } catch (InterruptedException e) {
		        e.printStackTrace();
	        }*/
            distmeshPanel.repaint();
            quality = meshGenerator.qualityCheck();
        }
    }


    public static void main(String[] args) {
        new TestEnhancedVersion();
    }
}
