package org.vadere.geometry.mesh.triangulation.improver;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.color.ColorHelper;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IMesh;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class EikMeshPanel<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Canvas {

    private static final Logger log = LogManager.getLogger(EikMeshPanel.class);
	private IMesh<P, V, E, F> mesh;
	private double width;
	private double height;
	private Collection<F> faces;
    private final Predicate<F> alertPred;
    private Collection<VTriangle> triangles;
    private VRectangle bound;
    private final double scale;
    private Function<F, Color> colorFunction;

	public EikMeshPanel(
			@NotNull final IMesh<P, V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			final double width, final double height,
			final VRectangle bound,
			final Function<F, Color> colorFunction) {
		this.mesh = mesh;
		this.width = width;
		this.height = height;
		this.alertPred = alertPred;
		this.triangles = new ArrayList<>();
		this.bound = bound;
		this.scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
		this.faces = new ArrayList<>();
		this.colorFunction = colorFunction;
	}

    public EikMeshPanel(final IMesh<P, V, E, F> mesh, final Predicate<F> alertPred, final double width, final double height, final VRectangle bound) {
    	this(mesh, alertPred, width, height, bound, f -> Color.WHITE);
    }

	@Override
	public void update(Graphics g) {
    	// TODO clone it!
		synchronized (mesh) {
			refresh();
			super.update(g);
		}

	}

	public void refresh() {
		synchronized (mesh) {
			faces = mesh.clone().getFaces();
		}
	}

	public BufferedImage getImage() {
		BufferedImage image = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fill(new VRectangle(0, 0, width, height));
		Font currentFont = graphics.getFont();
		Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.064f);
		graphics.setFont(newFont);
		graphics.setColor(Color.GRAY);
		graphics.translate(-bound.getMinX() * scale, -bound.getMinY() * scale);
		graphics.scale(scale, scale);

		//graphics.translate(-bound.getMinX()+(0.5*Math.max(0, bound.getWidth()-bound.getHeight())), -bound.getMinY() + (bound.getHeight()-height / scale));
		graphics.setStroke(new BasicStroke(0.003f));
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


		int groupSize = 64;
		ColorHelper colorHelper = new ColorHelper(faces.size());

		for(F face : faces) {
			VPolygon polygon = mesh.toTriangle(face);
			if(alertPred.test(face)) {
				graphics.setColor(Color.RED);
				graphics.draw(polygon);
			}
			else {
				graphics.setColor(colorFunction.apply(face));
				graphics.fill(polygon);
			}
			graphics.setColor(Color.GRAY);
			graphics.draw(polygon);
		}

		return image;
	}

	@Override
	public void paint(Graphics g) {
    	// double buffering => draw into an image
		Graphics2D graphics2D = (Graphics2D) g;
		graphics2D.drawImage(getImage(), 0, 0, null);
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
