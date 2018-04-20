package org.vadere.util.debug.gui.canvas;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draw selected {@link IMesh} with default coloring based on the quality of the triangulation.
 */
public class SimpleTriCanvas
		<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
		extends TriCanvas<P, V, E, F> {

	protected List<F> faces;

	private SimpleTriCanvas(final IMesh<P, V, E, F> mesh, VRectangle bound) {
		this(mesh, defaultWidth, defaultHeight, bound);
	}

	public SimpleTriCanvas(final IMesh<P, V, E, F> mesh) {
		this(mesh, defaultWidth, defaultHeight);
	}

	private SimpleTriCanvas(final IMesh<P, V, E, F> mesh, double width, double height) {
		this(mesh, defaultWidth, defaultHeight, defaultBound);
	}

	private SimpleTriCanvas(final IMesh<P, V, E, F> mesh, double width, double height, VRectangle bound) {
		super(mesh, width, height, bound);
		this.faces = new ArrayList<>();
		this.faces = mesh.getFacesWithHoles();

		// set state information
		setStateLog(sb -> {
			sb.append("Faces\n");
			int i = 0;
			for (IFace f : mesh.getFacesWithHoles()) {
				sb.append(i).append(": ");
				sb.append(f.toString()).append("\n");
				i++;
			}
		});
		addDrawingPrimitives();
	}

	//statics - Factory Methods.

	@NotNull
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
	SimpleTriCanvas<P, V, E, F> simpleCanvas(final IMesh<P, V, E, F> mesh, VRectangle bound) {
		return new SimpleTriCanvas<>(mesh, bound);
	}

	@NotNull
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
	SimpleTriCanvas<P, V, E, F> simpleCanvas(final IMesh<P, V, E, F> mesh) {

		return new SimpleTriCanvas<>(mesh);
	}

	//instance

	/**
	 * This is called from the Constructor. Defines what should be drawn on the canvas.
	 */
	private void addDrawingPrimitives() {

		// all faces with numbers and circles and vertices.
		addGuiDecorator(graphics -> {
			int i = 0;
			for (F face : faces) {
				try {
					VPolygon polygon = mesh.toPolygon(face);

					graphics.setColor(colorFunctions.faceFillColor(mesh, face));
					graphics.fill(polygon);
					graphics.draw(polygon);
					graphics.setColor(colorFunctions.faceDrawColor(mesh, face));
					graphics.draw(polygon);

					polygon.getPoints().forEach(p -> {
						graphics.setColor(Color.RED);
						graphics.fill(new VCircle(p, 0.1));
					});

					VPoint center = polygon.getCentroid();
					graphics.drawString(Integer.toString(i), (float) center.x, (float) center.y);
					i++;

				} catch (ArrayIndexOutOfBoundsException e) {
					log.error("could not paint a face + " + face);
				}

			}
		});


	}

	public List<F> getFaces() {
		return faces;
	}
}
