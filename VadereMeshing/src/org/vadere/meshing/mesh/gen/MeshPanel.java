package org.vadere.meshing.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.visualization.ColorHelper;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * This {@link Canvas} can be used to display a {@link IMesh}.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshPanel<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Canvas {

    private static final Logger log = LogManager.getLogger(MeshPanel.class);

	/**
	 * The mesh which will be rendered.
	 */
	private IMesh<P, V, E, F> mesh;

	/**
	 * The width of the canvas.
	 */
	private double width;

	/**
	 * The height of the canvas.
	 */
	private double height;

	/**
	 * A {@link Collection} of {@link F} from the mesh.
	 * Ths collection exist to avoid the {@link java.util.ConcurrentModificationException}.
	 */
	private Collection<F> faces;

	/**
	 * A {@link Predicate} of {@link F} which marks a face to be drawn (not filled) in a special way.
	 */
    private final Predicate<F> alertPred;

	/**
	 * The bound of the mesh.
	 */
	private VRectangle bound;

	/**
	 * The scaling between the mesh bound-size and the canvas bound-size.
	 */
    private final double scale;

	/**
	 * A function which decides by which color the face should be filled.
	 */
	private Function<F, Color> colorFunction;

	/**
	 * Default constructor.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 * @param bound         bound of the mesh
	 * @param colorFunction color function coloring faces
	 */
	public MeshPanel(
			@NotNull final IMesh<P, V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			final double width,
			final double height,
			@NotNull final VRectangle bound,
			@NotNull final Function<F, Color> colorFunction) {
		this.mesh = mesh;
		this.width = width;
		this.height = height;
		this.alertPred = alertPred;
		this.bound = bound;
		this.scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
		this.faces = new ArrayList<>();
		this.colorFunction = colorFunction;
	}

	/**
	 * Construct a mesh panel filling all faces with the color white.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 * @param bound         bound of the mesh
	 */
    public MeshPanel(
    		@NotNull final IMesh<P, V, E, F> mesh,
		    @NotNull final Predicate<F> alertPred,
		    final double width,
		    final double height,
		    @NotNull final VRectangle bound) {
    	this(mesh, alertPred, width, height, bound, f -> Color.WHITE);
    }

	/**
	 * Construct a mesh panel filling all faces with the color white.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 * @param bound         bound of the mesh
	 */
	public MeshPanel(
			@NotNull final IMesh<P, V, E, F> mesh,
			final double width,
			final double height,
			@NotNull final VRectangle bound) {
		this(mesh, f -> false, width, height, bound, f -> Color.WHITE);
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

	/**
	 * Constructs the image {@link BufferedImage} which will be drawn to the canvas / panel.
	 *
	 * @return an image of the mesh
	 */
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


		/*int groupSize = 64;
		ColorHelper colorHelper = new ColorHelper(faces.size());*/

		for(F face : faces) {
			VPolygon polygon = mesh.toTriangle(face);
			graphics.setColor(colorFunction.apply(face));
			graphics.fill(polygon);

			if(alertPred.test(face)) {
				graphics.setColor(Color.RED);
				graphics.draw(polygon);
			}
			else {
				graphics.setColor(Color.GRAY);
				graphics.draw(polygon);
			}
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
		return display("Mesh");
	}

	public JFrame display(final String title) {
		JFrame jFrame = new JFrame();
		jFrame.setSize((int)width+10, (int)height+10);
		jFrame.add(this);
		jFrame.setTitle(title);
		jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);
		jFrame.setVisible(true);
		repaint();
		jFrame.repaint();
		return jFrame;
	}
}
