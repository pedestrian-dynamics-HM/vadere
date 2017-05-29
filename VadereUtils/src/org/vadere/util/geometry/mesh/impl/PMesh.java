package org.vadere.util.geometry.mesh.impl;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.triangulation.IPointConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class PMesh<P extends IPoint> implements IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> {

	private List<PFace<P>> faces;
	private PFace<P> boundary;
	private List<PHalfEdge<P>> edges;
	private IPointConstructor<P> pointConstructor;
	private Set<PVertex<P>> vertices;

	public PMesh(final IPointConstructor<P> pointConstructor) {
		this.faces = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new HashSet<>();
		this.boundary = new PFace<>(true);
		//this.faces.add(boundary);
		this.pointConstructor = pointConstructor;
	}

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
	public PFace<P> getFace(@NotNull final PHalfEdge<P> halfEdge) {
		return halfEdge.getFace();
	}

	@Override
	public PHalfEdge<P> getEdge(@NotNull PVertex<P> vertex) {
		return vertex.getEdge();
	}

	@Override
	public PHalfEdge<P> getEdge(PFace<P> face) {
		return face.getEdge();
	}

	@Override
	public P getPoint(@NotNull PHalfEdge<P> halfEdge) {
		return getVertex(halfEdge).getPoint();
	}

	@Override
	public PVertex<P> getVertex(@NotNull PHalfEdge<P> halfEdge) {
		return halfEdge.getEnd();
	}

	@Override
	public P getPoint(@NotNull PVertex<P> vertex) {
		return vertex.getPoint();
	}

	@Override
	public PFace<P> getFace() {
		return faces.stream().filter(face -> !face.isDestroyed()).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull PFace<P> face) {
		return face.isBorder();
	}

	@Override
	public boolean isBoundary(@NotNull PHalfEdge<P> halfEdge) {
		return halfEdge.isBoundary();
	}

	@Override
	public boolean isDestroyed(@NotNull PFace<P> face) {
		return face.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull PHalfEdge<P> edge) {
		return !edge.isValid();
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
	public void setFace(final PHalfEdge<P> halfEdge, final PFace<P> face) {
		halfEdge.setFace(face);
	}

	@Override
	public void setEdge(final PFace<P> face, final PHalfEdge<P> edge) {
		face.setEdge(edge);
	}

	@Override
	public void setEdge(@NotNull PVertex<P> vertex, @NotNull PHalfEdge<P> edge) {
		vertex.setEdge(edge);
	}

	@Override
	public void setVertex(@NotNull PHalfEdge<P> halfEdge, @NotNull PVertex<P> vertex) {
		halfEdge.setEnd(vertex);
	}

	@Override
	public List<PHalfEdge<P>> getEdges(@NotNull final PVertex<P> vertex) {
		return edges.stream().filter(edge -> !edge.isValid()).filter(edge -> getVertex(edge).equals(vertex)).collect(Collectors.toList());
	}

	@Override
	public Collection<PVertex<P>> getVertices() {
		return vertices;
	}

	@Override
	public int getNumberOfVertices() {
		return vertices.size();
	}

	@Override
	public int getNumberOfFaces() {
		return faces.size();
	}

	@Override
	public PHalfEdge<P> createEdge(@NotNull PVertex<P> vertex) {
		PHalfEdge<P> edge = new PHalfEdge<>(vertex);
		edges.add(edge);
		return edge;
	}

	@Override
	public PHalfEdge<P> createEdge(@NotNull PVertex<P> vertex, @NotNull PFace<P> face) {
		PHalfEdge<P> edge = new PHalfEdge<>(vertex, face);
		edges.add(edge);
		return edge;
	}

	@Override
	public PFace<P> createFace() {
		return createFace(false);
	}

	@Override
	public PFace<P> createFace(boolean boundary) {
		if(!boundary) {
			PFace<P> face = new PFace<>();
			faces.add(face);
			return face;
		}
		else {
			return this.boundary;
		}
	}

	@Override
	public P createPoint(double x, double y) {
		return pointConstructor.create(x, y);
	}


	// TODO: maybe remove insertVertex!
	@Override
	public PVertex<P> createVertex(double x, double y) {
		return createVertex(pointConstructor.create(x, y));
	}

	@Override
	public PVertex<P> createVertex(P point) {
		return new PVertex<>(point);
	}

	@Override
	public PFace<P> getBoundary() {
		return boundary;
	}

	@Override
	public void insert(final PVertex<P> vertex) {
		vertices.add(vertex);
	}

	@Override
	public void insertVertex(final PVertex<P> vertex) {
		vertices.add(vertex);
	}

	@Override
	public void destroyFace(@NotNull PFace<P> face) {
		faces.remove(face);
		face.destroy();
	}

	@Override
	public void destroyEdge(@NotNull PHalfEdge<P> edge) {
		edges.remove(edge);
		edge.destroy();
	}

	@Override
	public void setDown(@NotNull PVertex<P> up, @NotNull PVertex<P> down) {
		up.setDown(down);
	}

	@Override
	public PVertex<P> getDown(@NotNull PVertex<P> vertex) {
		return vertex.getDown();
	}

	@Override
	public void destroyVertex(@NotNull PVertex<P> vertex) {
		vertices.remove(vertex);
	}

	@Override
	public Stream<PFace<P>> streamFaces() {
		return faces.stream();
	}

	@Override
	public Stream<PHalfEdge<P>> streamEdges() {
		return streamFaces().flatMap(face -> streamEdges(face));
	}

	@Override
	public List<PFace<P>> getFaces() {
		return streamFaces().filter(face -> !face.isDestroyed()).collect(Collectors.toList());
	}
}
