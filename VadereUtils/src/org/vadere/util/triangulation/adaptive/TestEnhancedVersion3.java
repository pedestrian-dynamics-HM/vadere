package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;

import javax.swing.*;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion3 extends JFrame {

	ArrayList<VShape> obstacles;

	private TestEnhancedVersion3()
	{
       //VRectangle bbox = new VRectangle(0,0,100,100);
       // ArrayList<VRectangle> obs = new ArrayList<VRectangle>() {{ add(new VRectangle(20,20,20,20));}};
		//double h0 = 3.0;

		long now = System.currentTimeMillis();

		VRectangle bbox = new VRectangle(-10, -10, 20, 20);
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

		obstacles = new ArrayList<VShape>() {{
//            add(new VRectangle(0.6*400, 0.6*400, 0.2*400, 5));
//            add(new VRectangle(0.6*400, 0.65*400, 0.2*400, 5));
			add(new VRectangle(0.65*300, -5, 0.1*300, 0.6*300));
			add(new VRectangle(0.65*300, 0.7*300, 0.1*300, 0.3*300));
//            add(p);
		}};

		java.util.List<VShape> boundingBox = new ArrayList<VShape>() {{
			add(new VRectangle(0, 0, 5, width));
			add(new VRectangle(0, 0, width, 5));
			add(new VRectangle(width-5, 0, 5, height));
			add(new VRectangle(0, height-5, 5, height));
		}};

		//IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 3;

		IDistanceFunction distanceFunc = p -> Math.abs(7 - Math.max(Math.abs(p.getX()), Math.abs(p.getY()))) - 3;
		//IEdgeLengthFunction edgeLengthFunc = p -> 1.0 + p.distanceToOrigin()*10;
		IEdgeLengthFunction edgeLengthFunc = p -> 1.0 - distanceFunc.apply(p);

		PSMeshing meshGenerator = new PSMeshing(distanceFunc, edgeLengthFunc, bbox, new ArrayList<>());

		System.out.println(System.currentTimeMillis()-now);
		now = System.currentTimeMillis();
		System.out.println(System.currentTimeMillis()-now);
		PSMeshingPanel distmeshPanel = new PSMeshingPanel(meshGenerator, 1000, 800);
		JFrame frame = distmeshPanel.display();
		frame.setVisible(true);

		meshGenerator.initialize();
		//double maxLen = meshGenerator.step();
		double maxLen = 10;

		while (maxLen > 0.5 || true) {
			System.out.println("maxLen:"+ maxLen);
			meshGenerator.improve();
			distmeshPanel.update();
			distmeshPanel.repaint();
		}
	}

	public static void main(String[] args) {
		new TestEnhancedVersion3();
	}
}
