package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.geom.Ellipse2D;
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
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshRenderer<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	private static final Logger log = Logger.getLogger(MeshRenderer.class);

	/**
	 * The mesh which will be rendered.
	 */
	private IMesh<V, E, F> mesh;

	/**
	 * A {@link Collection} of {@link F} from the mesh.
	 * Ths collection exist to avoid the {@link java.util.ConcurrentModificationException}.
	 */
	private Collection<F> faces;

	private Collection<E> edges;

	private Collection<V> vertices;

	/**
	 * A {@link Predicate} of {@link F} which marks a face to be drawn (not filled) in a special way.
	 */
	private final Predicate<F> alertPred;


	/**
	 * A function which decides by which color the face should be filled.
	 */
	@Nullable private Function<F, Color> faceColorFunction;

	@Nullable private Function<E, Color> edgeColorFunction;

	@Nullable private Function<V, Color> vertexColorFunction;

	private BufferedImage bufferedImage = null;


	/**
	 * Default constructor.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param colorFunction color function coloring faces
	 */
	public MeshRenderer(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			@Nullable final Function<F, Color> colorFunction) {
		this(mesh, alertPred, colorFunction, null);
	}

	public MeshRenderer(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			@Nullable final Function<F, Color> faceColorFunction,
			@Nullable final Function<E, Color> edgeColorFunction) {
		this.mesh = mesh;
		this.alertPred = alertPred;
		this.faces = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.faceColorFunction = faceColorFunction;
		this.edgeColorFunction = edgeColorFunction;
	}

	public MeshRenderer(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			@Nullable final Function<F, Color> faceColorFunction,
			@Nullable final Function<E, Color> edgeColorFunction,
			@Nullable final Function<V, Color> vertexColorFunction) {
		this.mesh = mesh;
		this.alertPred = alertPred;
		this.faces = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.faceColorFunction = faceColorFunction;
		this.edgeColorFunction = edgeColorFunction;
		this.vertexColorFunction = vertexColorFunction;
	}

	/**
	 * Construct a mesh renderer which will not fill the faces.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 */
	public MeshRenderer(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred) {
		this(mesh, alertPred, null);
	}

	/**
	 * Construct a mesh renderer which will not fill the faces.
	 *
	 * @param mesh          the mesh which will be rendered
	 */
	public MeshRenderer(
			@NotNull final IMesh<V, E, F> mesh) {
		this(mesh, f -> false, null);
	}

	public void setMesh(@NotNull final IMesh<V, E, F> mesh) {
		this.mesh = mesh;
	}

	public void render(@NotNull final Graphics2D targetGraphics2D, final int width, final int height) {
		render(targetGraphics2D, 0, 0, width, height);
	}

	public void render(@NotNull final Graphics2D targetGraphics2D, final int x, final int y, final int width, final int height) {
		renderImage(width, height);
		targetGraphics2D.drawImage(bufferedImage, x, y, null);
		targetGraphics2D.dispose();
	}

	private void renderGraphics(@NotNull final Graphics2D graphics, final int width, final int height) {
		renderGraphics(graphics, width, height, null);
	}

	private void renderGraphics(@NotNull final Graphics2D graphics, final int width, final int height, VRectangle bound) {
		/*Font currentFont = graphics.getFont();
		Font newFont = currentFont.deriveFont(currentFont.getSize() * 0.064f);
		graphics.setFont(newFont);
		graphics.setColor(Color.GRAY);*/

		Color c = graphics.getColor();
		Stroke stroke = graphics.getStroke();

		double scale;
		synchronized (mesh) {
			if(bound == null) {
				bound = mesh.getBound();
			}

			scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
			faces = mesh/*.clone()*/.getFaces();
			edges = mesh.getEdges();
			vertices = mesh.getVertices();
		}

		//graphics.translate(-bound.getMinX() * scale, height-bound.getMinY() * scale);
		//graphics.scale(scale, -scale);

		graphics.translate(-bound.getMinX() * scale, -bound.getMinY() * scale);
		graphics.scale(scale, scale);
		graphics.fill(bound);

		//graphics.translate(-bound.getMinX()+(0.5*Math.max(0, bound.getWidth()-bound.getHeight())), -bound.getMinY() + (bound.getHeight()-height / scale));
		graphics.setStroke(new BasicStroke(0.5f * (float)(1/scale)));
		double ptdiameter = 3.0f * (float)(1/scale);
		//graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		/*int groupSize = 64;
		ColorHelper colorHelper = new ColorHelper(faces.size());*/

		for(F face : faces) {
			VPolygon polygon = mesh.toPolygon(face);

			if(alertPred.test(face)) {
				graphics.setColor(new Color(200, 0, 0));
			} else {
				if(faceColorFunction != null) {
					graphics.setColor(faceColorFunction.apply(face));
				} else {
					graphics.setColor(Color.GRAY);
				}
			}
			graphics.fill(polygon);
		}

		for(E edge : edges) {
			Color ec = Color.DARK_GRAY;
			if(edgeColorFunction != null) {
				ec = edgeColorFunction.apply(edge);
			}
			graphics.setColor(ec);
			graphics.draw(mesh.toLine(edge));
		}

		for(V vertex : vertices) {
			Color vc = Color.BLACK;
			if(vertexColorFunction != null) {
				vc = vertexColorFunction.apply(vertex);
			} /*else if(mesh.isAtBoundary(vertex)) {
				vc = Color.RED;
			}*/
			//graphics.setColor(vc);
			//graphics.fill(new Ellipse2D.Double(vertex.getX()-ptdiameter/2, vertex.getY()-ptdiameter/2, ptdiameter, ptdiameter));
		}

		graphics.setColor(c);
		graphics.setStroke(stroke);
		graphics.scale(1.0 / scale, 1.0 / scale);
		graphics.translate(bound.getMinX() * scale, bound.getMinY() * scale);

	}

	/*public void renderGraphics(@NotNull final Graphics2D graphics, final double width, final double height) {
		VRectangle bound;
		synchronized (mesh) {
			bound = mesh.getBound();
		}
		double scale = Math.min(width / bound.getWidth(), height / bound.getHeight());
	//	graphics.setColor(Color.WHITE);
	//	graphics.fill(new VRectangle(0, 0, width, height));
		renderGraphics(graphics, scale, bound);
	}*/

	public BufferedImage renderImage(final int width, final int height, VRectangle bound) {
		synchronized (mesh) {
			if(bufferedImage == null) {
				bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			}

			//if(mesh.getNumberOfVertices() > 6) {
			Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
			graphics.fillRect(0, 0, width, height);
			renderGraphics(graphics, width, height, bound);
			//}

			return bufferedImage;
		}
	}

	public BufferedImage renderImage(final int width, final int height) {
		synchronized (mesh) {
			if(bufferedImage == null) {
				bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			}

			//if(mesh.getNumberOfVertices() > 6) {
			Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
			graphics.fillRect(0, 0, width, height);
			renderGraphics(graphics, width, height);
			//}

			return bufferedImage;
		}
	}
}
