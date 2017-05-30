package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

/**
 * Created by bzoennchen on 29.05.17.
 */
public class PSMeshingPanel extends Canvas {

	private PSMeshing meshGenerator;
	private double width;
	private double height;

	public PSMeshingPanel(final PSMeshing meshGenerator, final double width, final double height) {
		this.meshGenerator = meshGenerator;
		this.width = width;
		this.height = height;
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D graphics2D = (Graphics2D) g;
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();

		graphics.setStroke(new BasicStroke(0.03f));
		graphics.scale(3, 3);
		graphics.setColor(Color.WHITE);
		graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));

		graphics.setColor(Color.GRAY);
	       /* for(VShape obstacle : obstacles) {
		        graphics.fill(obstacle);
	        }*/

		graphics.setColor(Color.BLACK);
		for(VTriangle triangle : meshGenerator.getTriangles()) {
			if(triangle.isNonAcute()) {
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
		graphics2D.drawImage(image, 20, 20, null);


//            graphics.setColor(Color.RED);
//            tc.triangulation.getTriangles().parallelStream().forEach(graphics::draw);
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
