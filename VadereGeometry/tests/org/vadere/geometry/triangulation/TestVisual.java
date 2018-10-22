package org.vadere.geometry.triangulation;

import org.vadere.geometry.mesh.impl.VPTriangulation;
import org.vadere.geometry.mesh.impl.VPUniformRefinement;
import org.vadere.geometry.mesh.inter.IPointConstructor;
import org.vadere.geometry.mesh.inter.ITriangulation;
import org.vadere.geometry.shapes.VCircle;
import org.vadere.geometry.shapes.VLine;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 */
public class TestVisual {

	static int height = 700;
	static int width = 700;
	static int max = Math.max(height, width);
	static VRectangle bound = new VRectangle(0, 0, width, height);


	public static void main(String[] args) {
		testTriangulation();
	}


	public static void testUniformRefinement() {

		VPUniformRefinement uniformRefinement = new VPUniformRefinement(
                () -> ITriangulation.createVPTriangulation(bound),
				bound,
				Arrays.asList(new VRectangle(200, 200, 100, 200)),
				p -> 40.0);

        ITriangulation triangulation = uniformRefinement.generate();
        Set<VLine> edges4 = triangulation.getEdges();

		JFrame window = new JFrame();
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setBounds(0, 0, max, max);
		window.getContentPane().add(new Lines(edges4, edges4.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window.setVisible(true);
	}

	public static void testTriangulation() {

		Set<VPoint> points = new HashSet<>();

		Random r = new Random();
		for(int i=0; i< 20; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		IPointConstructor<VPoint> pointConstructor =  (x, y) -> new VPoint(x, y);
		long ms = System.currentTimeMillis();

		VPTriangulation triangulation = ITriangulation.createVPTriangulation(bound);
		triangulation.insert(points);
		triangulation.finish();
		Set<VLine> edges = triangulation.getEdges();

		JFrame window = new JFrame();
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setBounds(0, 0, max, max);
		window.getContentPane().add(new Lines(edges, points, max));
		window.setVisible(true);
	}

	private static class Lines extends JComponent{
		private Set<VLine> edges;
		private Set<VPoint> points;
		private final int max;

		public Lines(final Set<VLine> edges, final Set<VPoint> points, final int max){
			this.edges = edges;
			this.points = points;
			this.max = max;
		}

		public void paint(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setBackground(Color.white);
			g2.setStroke(new BasicStroke(1.0f));
			g2.setColor(Color.black);
			g2.draw(new VRectangle(200, 200, 100, 200));
			g2.setColor(Color.gray);
			//g2.translate(200, 200);
			//g2.scale(0.2, 0.2);

			g2.draw(new VRectangle(200, 200, 100, 200));

			edges.stream().forEach(edge -> {
				Shape k = new VLine(edge.getP1().getX(), edge.getP1().getY(), edge.getP2().getX(), edge.getP2().getY());
				g2.draw(k);
			});

			points.stream().forEach(point -> {
				VCircle k = new VCircle(point.getX(), point.getY(), 1.0);
				g2.draw(k);
			});

		}
	}

}
