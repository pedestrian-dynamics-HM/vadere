package org.vadere.meshing.utils.debug;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
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
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class SimpleTriCanvas
		<P extends IPoint, CE, CF,V extends IVertex, E extends IHalfEdge, F extends IFace>
		extends TriCanvas<P, CE, CF, V, E, F> {

	protected List<F> faces;

	private SimpleTriCanvas(final IMesh<V, E, F> mesh, VRectangle bound) {
		this(mesh, defaultWidth, defaultHeight, bound);
	}

	public SimpleTriCanvas(final IMesh<V, E, F> mesh) {
		this(mesh, defaultWidth, defaultHeight);
	}

	private SimpleTriCanvas(final IMesh<V, E, F> mesh, double width, double height) {
		this(mesh, defaultWidth, defaultHeight, defaultBound);
	}

	private SimpleTriCanvas(final IMesh<V, E, F> mesh, double width, double height, VRectangle bound) {
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
	public static <P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace>
	SimpleTriCanvas<P, CE, CF, V, E, F> simpleCanvas(final IMesh<V, E, F> mesh, VRectangle bound) {
		return new SimpleTriCanvas<>(mesh, bound);
	}

	@NotNull
	public static <P extends IPoint, CE, CF, V extends IVertex, E extends IHalfEdge, F extends IFace>
	SimpleTriCanvas<P, CE, CF, V, E, F> simpleCanvas(final IMesh<V, E, F> mesh) {

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
						graphics.fill(new VCircle(p, 0.025));
					});

					VPoint center = polygon.getCentroid();
					graphics.fill(new VCircle(center, 0.05));
					//graphics.drawString(Integer.toString(i), (float) center.x, (float) center.y);
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
