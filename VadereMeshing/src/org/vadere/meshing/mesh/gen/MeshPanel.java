package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.*;

import java.awt.*;
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

	private  MeshRenderer<P, V, E, F> meshRenderer;

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
			@NotNull final MeshRenderer<P, V, E, F> meshRenderer,
			final double width,
			final double height) {
		this.meshRenderer = meshRenderer;
		this.height = height;
		this.width = width;
	}

	/**
	 * Construct a mesh panel filling faces with the color defined by the color function.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param alertPred     a {@link Predicate} of {@link F} which marks a face to be drawn in a special way.
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 * @param colorFunction color function coloring faces
	 */
	public MeshPanel(
			@NotNull final IMesh<P, V, E, F> mesh,
			@NotNull final Predicate<F> alertPred,
			final double width,
			final double height,
			@NotNull final Function<F, Color> colorFunction) {
		this.meshRenderer = new MeshRenderer<>(mesh, alertPred, colorFunction);
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
    		@NotNull final IMesh<P, V, E, F> mesh,
		    @NotNull final Predicate<F> alertPred,
		    final double width,
		    final double height) {
    	this(mesh, alertPred, width, height, f -> Color.WHITE);
    }

	/**
	 * Construct a mesh panel filling all faces with the color white.
	 *
	 * @param mesh          the mesh which will be rendered
	 * @param width         width of the canvas
	 * @param height        height of the canvas
	 */
	public MeshPanel(
			@NotNull final IMesh<P, V, E, F> mesh,
			final double width,
			final double height) {
		this(mesh, f -> false, width, height, f -> Color.WHITE);
	}

	public MeshRenderer<P, V, E, F> getMeshRenderer() {
		return meshRenderer;
	}

	@Override
	public void paint(Graphics g) {
    	// double buffering => draw into an image
		meshRenderer.render((Graphics2D) g, (int)Math.ceil(width), (int)Math.ceil(height));
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
		repaint();
		jFrame.repaint();
		return jFrame;
	}
}
