package org.vadere.meshing.utils.debug;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.utils.tex.TexGraphBuilder;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.util.List;

/**
 * Color each visited face to show the walk and Create a straight line between start q and target p
 */
public class WalkCanvas<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
		extends SimpleTriCanvas<P, V, E, F> {

	protected final VPoint q;
	protected final VPoint p;
	protected final F startFace;
	protected final E startEdge;
	protected final List<F> visitedFaces;

	/**
	 * Color each visited face to show the walk and Create a straight line between start q and target p
	 *
	 * @param mesh         Current Mesh
	 * @param q            Start point of walk.
	 * @param p            New/Target point of walk.
	 * @param startFace    Start face where the walk starts.
	 * @param startEdge    Start edge which should be crossed (it seems this is not used...).
	 * @param visitedFaces List of Faces already visited.
	 */
	public WalkCanvas(IMesh<P, V, E, F> mesh, VPoint q, VPoint p,
	                  F startFace,
	                  E startEdge,
	                  List<F> visitedFaces) {
		super(mesh);
		this.startEdge = startEdge;
		this.startFace = startFace;
		this.visitedFaces = visitedFaces;
		this.q = q;
		this.p = p;
		addDrawingPrimitives();
	}

	//statics - Factory Methods

	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
	WalkCanvas<P, V, E, F> getDefault(IMesh<P, V, E, F> mesh,
									  VPoint p,
									  VPoint q,
									  F startFace,
									  E startEdge,
									  List<F> visitedFaces) {

		return new WalkCanvas<P, V, E, F>(mesh, p, q, startFace, startEdge, visitedFaces);

	}

	// instance

	/**
	 * This is called from the Constructor. Defines what should be drawn on the canvas.
	 * Note this is additional to primitives defined by parent classes.
	 */
	private void addDrawingPrimitives() {

		// overwrite color for visitedFaces. This will affect both java.swing and tex.
		visitedFaces.forEach(f -> colorFunctions.overwriteFillColor(f, Color.PINK));
		colorFunctions.overwriteFillColor(startFace, Color.CYAN);

		// direct line between q and p for java.swing
		addGuiDecorator(graphics -> {
			graphics.setStroke(new BasicStroke(0.03f));

			graphics.setColor(Color.BLUE);
			graphics.fill(new VCircle(q, 0.1));
			graphics.draw(new VLine(q, p));

			graphics.setColor(Color.GREEN);
			graphics.fill(new VCircle(p, 0.1)); //new one

		});

		// direct line between q and p for tex
		addTexDecorator(sb -> {
			sb.append(TexGraphBuilder.point(Color.BLUE, q.getX(), q.getY(), 0.1));
			sb.append(TexGraphBuilder.line(Color.BLUE, q, p));
			sb.append(TexGraphBuilder.point(Color.GREEN, p.getX(), p.getY(), 0.1));
		});
	}

}
