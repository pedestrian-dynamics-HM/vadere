package org.vadere.util.geometry.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;

import java.awt.geom.Path2D;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A Face is a region of a planar separation of the 2-D space, e.g. the region of a Polygon/Triangle and so on.
 *
 * @author Benedikt Zoennchen
 * @param <P> the type of the coordinates the face uses.
 */
public class Face<P extends IPoint> implements Iterable<PHalfEdge<P>>, IFace<P> {

	public static <P extends IPoint> Face<P> of(P x, P y, P z) {
		Face superTriangle = new Face();
		Face borderFace = new Face(true);
		PHalfEdge xy = new PHalfEdge(y, superTriangle);
		PHalfEdge yz = new PHalfEdge(z, superTriangle);
		PHalfEdge zx = new PHalfEdge(x, superTriangle);

		xy.setNext(yz);
		yz.setNext(zx);
		zx.setNext(xy);

		PHalfEdge yx = new PHalfEdge(x, borderFace);
		PHalfEdge zy = new PHalfEdge(y, borderFace);
		PHalfEdge xz = new PHalfEdge(z, borderFace);

		yx.setNext(xz);
		xz.setNext(zy);
		zy.setNext(yx);

		xy.setTwin(yx);
		yz.setTwin(zy);
		zx.setTwin(xz);

		superTriangle.setEdge(xy);
		borderFace.setEdge(yx);
		return superTriangle;
	}


	public static <P extends IPoint> Face<P> getBorder(Class<P> p) {
		return new Face<>(true);
	}

	/**
	 * One of the half-edges bordering this face.
	 */
	private PHalfEdge<P> edge;

	private boolean border;

	private boolean destroyed = false;

	/**
	 * Default constructor. To construct a face where you have already some half-edges
	 * bordering this face.
	 *
	 * @param edge one of the half-edges bordering this face.
	 */
	public Face(@NotNull final PHalfEdge<P> edge) {
		this(edge, false);
	}

	public Face(@NotNull final PHalfEdge<P> edge, boolean border) {
		this.border = border;
		this.edge = edge;
	}

	/**
	 * This constructor can be used for constructing a new face without having
	 * constructed the bordering half-edges jet.
	 */
	public Face(boolean border) {
		this.border = border;
	}

	public Face() {
		this.border = false;
	}

	public boolean isBorder() {
		return border;
	}

	public void destroy() {
		setEdge(null);
		destroyed = true;
	}

	public void toBorder() {
		border = true;
	}

	/**
	 * Sets one of the half-edges bordering this face.
	 *
	 * @param edge half-edge bordering this face
	 */
	public void setEdge(final PHalfEdge<P> edge) {
		this.edge = edge;
	}

	public PHalfEdge<P> getEdge() {
		return edge;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Computes the area of this face.
	 *
	 * @return the area of this face
	 */
	public double getArea() {
		return GeometryUtils.areaOfPolygon(getPoints());
	}

	/**
	 *
	 * @return
	 */
	public List<P> getPoints() {
		return streamPoints().collect(Collectors.toList());
	}

	/**
	 * Returns true if and only if the point contained in this face.
	 *
	 * @param point the point which might be contained
	 * @return true if and only if the point contained in this face.
	 */
	public boolean contains(final P point) {
		return toPolygon().contains(point);
	}

	/**
	 * Transforms this face into a Polygon object.
	 *
	 * @return the Polygon object defined by this face
	 */
	public VPolygon toPolygon() {
		Path2D path2D = new Path2D.Double();
		path2D.moveTo(edge.getPrevious().getEnd().getX(), edge.getPrevious().getEnd().getY());
		for(PHalfEdge edge : this) {
			path2D.lineTo(edge.getEnd().getX(), edge.getEnd().getY());
		}
		return new VPolygon(path2D);
	}

	/**
	 * Transforms this face into a triangle. Assumption: The face is a valid triangle.
	 *
	 * @throws IllegalArgumentException if the face does not define a valid triangle
	 * @return a triangle which is defined by this face
	 */
	public VTriangle toTriangle() {
		List<PHalfEdge<P>> edges = getEdges();
		if(edges.size() != 3) {
			throw new IllegalArgumentException("this face is not a feasible triangle.");
		}
		else {
			VPoint p1 = new VPoint(edges.get(0).getEnd().getX(), edges.get(0).getEnd().getY());
			VPoint p2 = new VPoint(edges.get(1).getEnd().getX(), edges.get(1).getEnd().getY());
			VPoint p3 = new VPoint(edges.get(2).getEnd().getX(), edges.get(2).getEnd().getY());
			return new VTriangle(p1, p2, p3);
		}
	}

	@Override
	public Iterator<PHalfEdge<P>> iterator() {
		return new HalfEdgeIterator();
	}

	public Stream<PHalfEdge<P>> stream () {
		Iterable<PHalfEdge<P>> iterable = () -> iterator();
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	public List<PHalfEdge<P>> getEdges() {
		return stream().collect(Collectors.toList());
	}

	public Stream<MLine<P>> streamLines() {
		return stream().map(halfEdge -> new MLine(halfEdge.getPrevious().getEnd(), halfEdge.getEnd()));
	}

	public Stream<P> streamPoints() {
		return stream().map(edge -> edge.getEnd());
	}

	@Override
	public String toString() {
		return stream().map(edge -> edge.getEnd().toString()).reduce("", (s1, s2) -> s1 + " " + s2);
	}

	private class HalfEdgeIterator implements Iterator<PHalfEdge<P>> {
		private PHalfEdge<P> currentHalfEdge;
		private boolean started = false;

		private HalfEdgeIterator(){
			this.currentHalfEdge = edge;
		}

		@Override
		public boolean hasNext() {
			return currentHalfEdge != null && (!started || !currentHalfEdge.equals(edge));
		}

		@Override
		public PHalfEdge<P> next() {
			started = true;
			PHalfEdge result = currentHalfEdge;
			currentHalfEdge = currentHalfEdge.getNext();
			return result;
		}
	}
}
