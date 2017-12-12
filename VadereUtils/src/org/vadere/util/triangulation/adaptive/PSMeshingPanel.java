package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.improver.IMeshImprover;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * Created by bzoennchen on 29.05.17.
 */
public class PSMeshingPanel<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Canvas {

	private IMeshImprover meshGenerator;
	private double width;
	private double height;
	private Collection<F> faces;
    private final Predicate<F> alertPred;

    public PSMeshingPanel(final IMeshImprover<P, V, E, F> meshGenerator, final Predicate<F> alertPred, final double width, final double height) {
        this.meshGenerator = meshGenerator;
        this.width = width;
        this.height = height;
        this.alertPred = alertPred;
    }

	public void update() {
		faces = meshGenerator.getTriangulation().getMesh().getFaces();
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D graphics2D = (Graphics2D) g;
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();


		//graphics.scale(3, 3);

		graphics.setColor(Color.WHITE);
		graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));

		graphics.setColor(Color.GRAY);
	       /* for(VShape obstacle : obstacles) {
		        graphics.fill(obstacle);
	        }*/

		graphics.translate(400, 400);
		graphics.scale(30, 30);
		graphics.setStroke(new BasicStroke(0.003f));
		graphics.setColor(Color.BLACK);
		for(F face : faces) {
            VTriangle triangle = meshGenerator.getTriangulation().getMesh().toTriangle(face);
			/*if(triangleToQuality(triangle) < 0.2) {
				graphics.setColor(Color.GRAY);
				graphics.draw(triangle);
				graphics.setColor(Color.RED);
				graphics.fill(triangle);
			}*/

			if(alertPred.test(face)) {
				graphics.setColor(Color.GRAY);
				graphics.draw(triangle);
				graphics.setColor(Color.RED);
				graphics.fill(triangle);
			} else {
				graphics.setColor(Color.GRAY);
				graphics.draw(triangle);
			}
		}
		//graphics.translate(5,5);
		graphics2D.drawImage(image, 0, 0, null);


//            graphics.setColor(Color.RED);
//            tc.triangulation.getTriangles().parallelStream().forEach(graphics::draw);
	}

	public double triangleToQuality(final VTriangle triangle) {

		VLine[] lines = triangle.getLines();
		double a = lines[0].length();
		double b = lines[1].length();
		double c = lines[2].length();
		double part = 0.0;
		if(a != 0.0 && b != 0.0 && c != 0.0) {
			part = ((b + c - a) * (c + a - b) * (a + b - c)) / (a * b * c);
		}
		else {
			throw new IllegalArgumentException(triangle + " is not a feasible triangle!");
		}
		return part;
	}

	public JFrame display() {
		JFrame jFrame = new JFrame();
		jFrame.setSize((int)width+10, (int)height+10);
		jFrame.add(this);
		jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);
		return jFrame;
	}
}
