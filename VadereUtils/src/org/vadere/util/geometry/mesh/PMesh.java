package org.vadere.util.geometry.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.List;

/**
 * @author Benedikt Zoennchen
 */
public class PMesh<P extends IPoint> implements IMesh<P, PHalfEdge<P>, Face<P>> {

	public PMesh() {}

	@Override
	public PHalfEdge<P> getNext(final PHalfEdge<P> halfEdge) {
		return halfEdge.getNext();
	}

	@Override
	public PHalfEdge<P> getPrev(final PHalfEdge<P> halfEdge) {
		return halfEdge.getPrevious();
	}

	@Override
	public PHalfEdge<P> getTwin(final PHalfEdge<P> halfEdge) {
		return halfEdge.getTwin();
	}

	@Override
	public Face<P> getFace(final PHalfEdge<P> halfEdge) {
		return halfEdge.getFace();
	}

	@Override
	public PHalfEdge<P> getEdge(@NotNull P vertex) {
		return null;
	}

	@Override
	public PHalfEdge<P> getEdge(Face<P> face) {
		return face.getEdge();
	}

	@Override
	public P getVertex(@NotNull PHalfEdge<P> halfEdge) {
		return halfEdge.getEnd();
	}

	@Override
	public boolean isBoundary(@NotNull PHalfEdge<P> halfEdge) {
		return halfEdge.isBoundary();
	}

	@Override
	public void setTwin(final PHalfEdge<P> halfEdge, final PHalfEdge<P> twin) {
		halfEdge.setTwin(twin);
	}

	@Override
	public void setNext(final PHalfEdge<P> halfEdge, final PHalfEdge<P> next) {
		halfEdge.setNext(next);
	}

	@Override
	public void setPrev(final PHalfEdge<P> halfEdge, final PHalfEdge<P> prev) {
		halfEdge.setPrevious(prev);
	}

	@Override
	public void setFace(final PHalfEdge<P> halfEdge, final Face<P> face) {
		halfEdge.setFace(face);
	}

	@Override
	public void setEdge(final Face<P> face, final PHalfEdge<P> edge) {
		face.setEdge(edge);
	}

	@Override
	public void setEdge(@NotNull P vertex, @NotNull PHalfEdge<P> edge) {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	@Override
	public void setVertex(@NotNull PHalfEdge<P> halfEdge, @NotNull P vertex) {
		halfEdge.setEnd(vertex);
	}

	@Override
	public List<PHalfEdge<P>> getEdges(@NotNull P vertex) {
		throw new UnsupportedOperationException("no jet implemented.");
	}

	@Override
	public PHalfEdge<P> createEdge(@NotNull P vertex) {
		return new PHalfEdge<>(vertex);
	}

	@Override
	public PHalfEdge<P> createEdge(@NotNull P vertex, @NotNull Face<P> face) {
		return new PHalfEdge<P>(vertex, face);
	}

	@Override
	public Face<P> createFace() {
		return new Face<>();
	}
}
