package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Created by Matimati-ka on 27.09.2016.
 */
public class TestEnhancedVersion2 extends JFrame {

	ArrayList<VShape> obstacles;

	private TestEnhancedVersion2()
	{
//        VRectangle bbox = new VRectangle(0,0,100,100);
//        ArrayList<VRectangle> obs = new ArrayList<VRectangle>() {{ add(new VRectangle(20,20,20,20));}};
		double h0 = 1.0;

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

		obstacles = new ArrayList<VShape>() {{
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

		PerssonStrangDistmesh meshGenerator = new PerssonStrangDistmesh(
				bbox,
				obstacles,
				h0,
				false,
				l -> 0.0,
				"Distmesh");

		System.out.println(System.currentTimeMillis()-now);
		now = System.currentTimeMillis();
		System.out.println(System.currentTimeMillis()-now);
		DrawPanel JPanel = new DrawPanel(meshGenerator);
		setSize(1000, 800);
		add(JPanel);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);

		/*while(true) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			JPanel.repaint();
			System.out.println("repaint");
		}*/
	}

	private class DrawPanel extends Canvas {

		private PerssonStrangDistmesh meshGenerator;

		private DrawPanel(PerssonStrangDistmesh meshGenerator) {
			this.meshGenerator = meshGenerator;
		}

		@Override
		public void paint(Graphics g) {
			Graphics2D graphics2D = (Graphics2D) g;
			BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = (Graphics2D)image.getGraphics();

			graphics.setStroke(new BasicStroke(0.1f));
			graphics.scale(3, 3);
			graphics.setColor(Color.WHITE);
			graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));
			graphics.translate(5,5);
			graphics.setColor(Color.BLACK);

			meshGenerator.getTriangulation().getTriangles().parallelStream().forEach(t -> graphics.draw(t));

			graphics.setColor(Color.GRAY);
			for(VShape obstacle : obstacles) {
				graphics.draw(obstacle);
			}

			graphics2D.drawImage(image, 0, 0, null);

//            graphics.setColor(Color.RED);
//            tc.triangulation.getTriangles().parallelStream().forEach(graphics::draw);
		}
	}

	public static void main(String[] args) {
		new TestEnhancedVersion2();
	}
}
