package org.vadere.meshing.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This helper class renders a {@link IMesh} into a {@link BufferedImage} or a {@link Graphics2D}.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshRenderer<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> {

	private static final Logger log = LogManager.getLogger(MeshRenderer.class);

	/**
	 * The mesh which will be rendered.
	 */
	private IMesh<P, CE, CF, V, E, F> mesh;

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
	 * A function which decides by which color the face should be filled.
	 */
	@Nullable private Function<F, Color> colorFunction;


	/**
	 * Default constructor.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param colorFunction color function coloring faces
	 */
	public MeshRenderer(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			@Nullable final Function<F, Color> colorFunction) {
		this.mesh = mesh;
		this.alertPred = alertPred;
		this.faces = new ArrayList<>();
		this.colorFunction = colorFunction;
	}

	/**
	 * Construct a mesh renderer which will not fill the faces.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 */
	public MeshRenderer(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh,
			@NotNull final Predicate<F> alertPred) {
		this(mesh, alertPred, null);
	}

	/**
	 * Construct a mesh renderer which will not fill the faces.
	 *
	 * @param mesh          the mesh which will be rendered
	 */
	public MeshRenderer(
			@NotNull final IMesh<P, CE, CF, V, E, F> mesh) {
		this(mesh, f -> false, null);
	}

	public void render(@NotNull final Graphics2D targetGraphics2D, final int width, final int height) {
		render(targetGraphics2D, 0, 0, width, height);
	}

	public void render(@NotNull final Graphics2D targetGraphics2D, final int x, final int y, final int width, final int height) {
		targetGraphics2D.drawImage(renderImage(width, height), x, y, null);
		targetGraphics2D.dispose();
	}

	public void renderGraphics(@NotNull final Graphics2D graphics, @NotNull final VRectangle bound) {
		renderGraphics(graphics, 1.0, bound);
	}

	private void renderGraphics(@NotNull final Graphics2D graphics, final double scale, @NotNull final VRectangle bound) {
		//graphics.setColor(Color.WHITE);
		//graphics.fill(new VRectangle(0, 0, width, height));
		/*Font currentFont = graphics.getFont();
		Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.064f);
		graphics.setFont(newFont);
		graphics.setColor(Color.GRAY);*/

		Color c = graphics.getColor();
		Stroke stroke = graphics.getStroke();
		synchronized (mesh) {
			faces = mesh.clone().getFaces();
		}

		graphics.translate(-bound.getMinX() * scale, -bound.getMinY() * scale);
		graphics.scale(scale, scale);

		//graphics.translate(-bound.getMinX()+(0.5*Math.max(0, bound.getWidth()-bound.getHeight())), -bound.getMinY() + (bound.getHeight()-height / scale));
		graphics.setStroke(new BasicStroke(0.003f));
		graphics.setColor(Color.BLACK);
		//graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


		/*int groupSize = 64;
		ColorHelper colorHelper = new ColorHelper(faces.size());*/

		for(F face : faces) {
			VPolygon polygon = mesh.toTriangle(face);

			if(colorFunction != null) {
				graphics.setColor(colorFunction.apply(face));
				graphics.fill(polygon);
			}


			if(alertPred.test(face)) {
				graphics.setColor(Color.RED);
				graphics.draw(polygon);
			}
			else {
				graphics.setColor(Color.GRAY);
				graphics.draw(polygon);
			}
		}

		graphics.setColor(c);
		graphics.setStroke(stroke);
		graphics.scale(1.0 / scale, 1.0 / scale);
		graphics.translate(bound.getMinX() * scale, bound.getMinY() * scale);
	}

	public void renderGraphics(@NotNull final Graphics2D graphics, final double width, final double height) {
		VRectangle bound;
		synchronized (mesh) {
			bound = mesh.getBound();
		}

		double scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
		renderGraphics(graphics, scale, bound);
	}

	public BufferedImage renderImage(final int width, final int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = (Graphics2D) image.getGraphics();
		renderGraphics(graphics, width, height);
		return image;
	}

}
