package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * Panel to visualize the original DistMesh algorithm.
 */
public class PSDistmeshPanel extends Canvas {

	private PSDistmesh meshGenerator;
	private double width;
	private double height;
	private double scale;
	private VRectangle bound;
	private Predicate<VTriangle> predicate;

	public PSDistmeshPanel(final PSDistmesh meshGenerator, final double width, final double height, final VRectangle bound) {
		this(meshGenerator, width, height, bound, t -> false);
	}

	public PSDistmeshPanel(final PSDistmesh meshGenerator, final double width, final double height, final VRectangle bound, final Predicate<VTriangle> predicate) {
		this.meshGenerator = meshGenerator;
		this.width = width;
		this.height = height;
		this.scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
		this.bound = bound;
		this.predicate = predicate;
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D graphics2D = (Graphics2D) g;
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fill(new VRectangle(0, 0, getWidth(), getHeight()));
		Font currentFont = graphics.getFont();
		Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.064f);
		graphics.setFont(newFont);
		graphics.setColor(Color.GRAY);
		graphics.scale(scale, scale);
		graphics.translate(-bound.getMinX() + (0.5 * Math.max(0, bound.getWidth() - bound.getHeight())), -bound.getMinY() + (bound.getHeight() - height / scale));
		graphics.setStroke(new BasicStroke(0.003f));
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.setColor(Color.BLACK);
		Collection<VTriangle> faceSet = meshGenerator.getTriangles();

		for (VTriangle triangle : faceSet) {
			if(this.predicate.test(triangle)) {
				graphics.setColor(Color.RED);
				graphics.fill(triangle);
			}

			graphics.setColor(Color.GRAY);
			graphics.draw(triangle);
		}
		graphics2D.drawImage(image, 0, 0, null);
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

