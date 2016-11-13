package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion extends JFrame {
    private TestEnhancedVersion()
    {
//        VRectangle bbox = new VRectangle(0,0,100,100);
//        ArrayList<VRectangle> obs = new ArrayList<VRectangle>() {{ add(new VRectangle(20,20,20,20));}};

        double h0 = 1;

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

        ArrayList<VShape> obstacles = new ArrayList<VShape>() {{
//            add(new VRectangle(0.6*400, 0.6*400, 0.2*400, 5));
//            add(new VRectangle(0.6*400, 0.65*400, 0.2*400, 5));
            add(new VRectangle(0.65*300, -5, 0.1*300, 0.6*300));
            add(new VRectangle(0.65*300, 0.7*300, 0.1*300, 0.3*300));
//            add(p);
        }};
        ArrayList<VShape> fhIncluded = new ArrayList<VShape>() {{
//            add(p);
//            add(new VRectangle(0.6*400, 0.6*400, 0.2*400, 5));
//            add(new VRectangle(0.6*400, 0.65*400, 0.2*400, 5));
            add(new VRectangle(0.65*300, -5, 0.1*300, 0.6*300));
            add(new VRectangle(0.65*300, 0.7*300, 0.1*300, 0.3*300));
        }};

        PerssonStrangDistmesh psd = new PerssonStrangDistmesh(
                bbox,
                obstacles,
                h0,
                false,
                l -> 0.0,
                "Distmesh");
        System.out.println(System.currentTimeMillis()-now);
        now = System.currentTimeMillis();
        System.out.println(System.currentTimeMillis()-now);
        DrawPanel JPanel = new DrawPanel(psd);
        setSize(1000, 800);
        add(JPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private class DrawPanel extends Canvas {

        private PerssonStrangDistmesh t;

        private DrawPanel(PerssonStrangDistmesh t) {
            this.t = t;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D graphics = (Graphics2D) g;
            graphics.translate(125,125);
            graphics.setColor(Color.BLACK);
            t.getTriangulation().getVTriangles().parallelStream().forEach(t -> graphics.draw(t));
//            graphics.setColor(Color.RED);
//            tc.triangulation.getTriangles().parallelStream().forEach(graphics::draw);
        }
    }

    public static void main(String[] args) {
        new TestEnhancedVersion();
    }
}
