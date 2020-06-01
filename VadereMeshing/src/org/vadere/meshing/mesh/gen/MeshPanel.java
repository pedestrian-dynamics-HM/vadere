package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;

import java.awt.*;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.*;

/**
 * This {@link Canvas} can be used to display a {@link IMesh}.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class MeshPanel<V extends IVertex, E extends IHalfEdge, F extends IFace> extends JPanel {

	private MeshRenderer<V, E, F> meshRenderer;
	public static Function defaultFaceColors = f -> new Color(0.8584083044982699f, 0.9134486735870818f, 0.9645674740484429f);

	/**
	 * The width of the canvas.
	 */
	private double width;

	/**
	 * The height of the canvas.
	 */
	private double height;


	/**
	 * Default constructor.
	 *
	 * @param meshRenderer  a renderer which draws the mesh
	 * @param width         the width of the canvas
	 * @param height        the height of the canvas
	 */
	public MeshPanel(
			@NotNull final MeshRenderer<V, E, F> meshRenderer,
			final double width,
			final double height) {
		this.meshRenderer = meshRenderer;
		this.height = height;
		this.width = width;
	}

	/**
	 * Construct a mesh panel filling faces with the color defined by the color function.
	 *
	 * @param mesh              the mesh which will be rendered
	 * @param alertPred         a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param width             width of the canvas
	 * @param height            height of the canvas
	 * @param colorFunction     color function coloring faces
	 * @param edgeColorFunction color function coloring edges
	 */
	public MeshPanel(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			final double width,
			final double height,
			@NotNull final Function<F, Color> colorFunction,
			@NotNull final Function<E, Color> edgeColorFunction) {
		this.meshRenderer = new MeshRenderer<>(mesh, alertPred, colorFunction, edgeColorFunction);
		this.height = height;
		this.width = width;
	}

	public MeshPanel(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			final double width,
			final double height,
			@NotNull final Function<F, Color> colorFunction,
			@NotNull final Function<E, Color> edgeColorFunction,
			@NotNull final Function<V, Color> vertexColorFuntion) {
		this.meshRenderer = new MeshRenderer<>(mesh, alertPred, colorFunction, edgeColorFunction, vertexColorFuntion);
		this.height = height;
		this.width = width;
	}

	/**
	 * Construct a mesh panel filling faces with the color defined by the color function.
	 *
	 * @param mesh              the mesh which will be rendered
	 * @param alertPred         a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param width             width of the canvas
	 * @param height            height of the canvas
	 * @param colorFunction     color function coloring faces
	 */
	public MeshPanel(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			final double width,
			final double height,
			@NotNull final Function<F, Color> colorFunction) {
		this.meshRenderer = new MeshRenderer<>(mesh, alertPred, colorFunction, e -> Color.GRAY);
		this.height = height;
		this.width = width;
	}

	/**
	 * Construct a mesh panel filling all faces with the color white.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 */
    public MeshPanel(
    		@NotNull final IMesh<V, E, F> mesh,
		    @NotNull final Predicate<F> alertPred,
		    final double width,
		    final double height) {
    	this(mesh, alertPred, width, height, defaultFaceColors, e -> Color.GRAY);
    }

	/**
	 * Construct a mesh panel filling all faces with the color white.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 */
	public MeshPanel(
			@NotNull final IMesh<V, E, F> mesh,
			final double width,
			final double height) {
		this(mesh, f -> false, width, height, defaultFaceColors, e -> Color.GRAY);
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	@Override
	public void paint(Graphics g) {
		meshRenderer.render((Graphics2D) g, (int)Math.ceil(width), (int)Math.ceil(height));
	}

	public MeshRenderer<V, E, F> getMeshRenderer() {
		return meshRenderer;
	}

	public JFrame display() {
		return display("Mesh");
	}

	public JFrame display(final String title) {
		JFrame jFrame = new JFrame();
		jFrame.setSize((int)width+10, (int)height+10);
		jFrame.add(this);
		jFrame.setTitle(title);
		jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
		jFrame.setVisible(true);
		//jFrame.setIgnoreRepaint(true);
		jFrame.repaint();
		return jFrame;
	}
}
