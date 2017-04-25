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

/**
 * @author Benedikt Zoennchen
 */
public class PMesh<P extends IPoint> implements IMesh<P, PHalfEdge<P>, PFace<P>> {

	private List<PFace<P>> faces;
	private PFace<P> boundary;
	private List<PHalfEdge<P>> edges;
	private IPointConstructor<P> pointConstructor;
	private Set<P> vertices;

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
	public PFace<P> getFace(final PHalfEdge<P> halfEdge) {
		return halfEdge.getFace();
	}

	@Override
	public PHalfEdge<P> getEdge(@NotNull P vertex) {
		return null;
	}

	@Override
	public PHalfEdge<P> getEdge(PFace<P> face) {
		return face.getEdge();
	}

	@Override
	public P getVertex(@NotNull PHalfEdge<P> halfEdge) {
		return halfEdge.getEnd();
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
	public boolean isHole(@NotNull PFace<P> face) {
		return false;
	}

	@Override
	public boolean isHole(@NotNull PHalfEdge<P> halfEdge) {
		return false;
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
	public void setEdge(@NotNull P vertex, @NotNull PHalfEdge<P> edge) {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	@Override
	public void setVertex(@NotNull PHalfEdge<P> halfEdge, @NotNull P vertex) {
		halfEdge.setEnd(vertex);
	}

	@Override
	public List<PHalfEdge<P>> getEdges(@NotNull P vertex) {
		return edges.stream().filter(edge -> !edge.isValid()).filter(edge -> getVertex(edge).equals(vertex)).collect(Collectors.toList());
	}

	@Override
	public Collection<P> getVertices() {
		return vertices;
	}

	@Override
	public int getNumberOfVertices() {
		return vertices.size();
	}

	@Override
	public PHalfEdge<P> createEdge(@NotNull P vertex) {
		PHalfEdge<P> edge = new PHalfEdge<>(vertex);
		edges.add(edge);
		return edge;
	}

	@Override
	public PHalfEdge<P> createEdge(@NotNull P vertex, @NotNull PFace<P> face) {
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
	public P createVertex(double x, double y) {
		P vertex = pointConstructor.create(x, y);
		//vertices.add(vertex);
		return vertex;
	}

	@Override
	public void insert(P vertex) {
		vertices.add(vertex);
	}

	@Override
	public void insertVertex(P vertex) {
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
	public void destroyVertex(@NotNull P vertex) {
		vertices.remove(vertex);
	}

	@Override
	public List<PFace<P>> getFaces() {
		return faces.stream().filter(face -> !face.isDestroyed()).collect(Collectors.toList());
	}
}
