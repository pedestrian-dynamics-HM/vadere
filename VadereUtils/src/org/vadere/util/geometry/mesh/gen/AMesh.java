package org.vadere.util.geometry.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.triangulation.IPointConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by bzoennchen on 06.09.17.
 */
public class AMesh<P extends IPoint> implements IMesh<P, AVertex<P>, AHalfEdge<P>, AFace<P>> {
	private final static Logger log = LogManager.getLogger(AMesh.class);
	private List<AFace<P>> faces;
	private boolean elementRemoved;
	private int numberOfVertices;
	private int numberOfEdges;
	private int numberOfFaces;
	//private List<PFace<P>> borderFaces;
	private AFace<P> boundary;
	private List<AHalfEdge<P>> edges;
	private IPointConstructor<P> pointConstructor;
	private List<AVertex<P>> vertices;

	public AMesh(final IPointConstructor<P> pointConstructor) {
		this.faces = new ArrayList<>();
		//this.borderFaces = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();
		this.boundary = new AFace<>(-1, true);
		//this.faces.add(boundary);
		this.pointConstructor = pointConstructor;
		this.elementRemoved = false;
		this.numberOfFaces = 0;
		this.numberOfEdges = 0;
		this.numberOfVertices = 0;
	}

	@Override
	public AHalfEdge<P> getNext(@NotNull AHalfEdge<P> halfEdge) {
		return edges.get(halfEdge.getNext());
	}

	@Override
	public AHalfEdge<P> getPrev(@NotNull AHalfEdge<P> halfEdge) {
		return edges.get(halfEdge.getPrevious());
	}

	@Override
	public AHalfEdge<P> getTwin(@NotNull AHalfEdge<P> halfEdge) {
		return edges.get(halfEdge.getTwin());
	}

	@Override
	public AFace<P> getFace(@NotNull AHalfEdge<P> halfEdge) {
		int edgeId = halfEdge.getFace();
		if (edgeId == -1) {
			if (halfEdge.isDestroyed()) {
				throw new IllegalArgumentException(halfEdge + " is already destroyed.");
			}
			return boundary;
		} else {
			return faces.get(halfEdge.getFace());
		}
	}

	@Override
	public AHalfEdge<P> getEdge(@NotNull AVertex<P> vertex) {
		return edges.get(vertex.getEdge());
	}

	@Override
	public AHalfEdge<P> getEdge(@NotNull AFace<P> face) {
		return edges.get(face.getEdge());
	}

	@Override
	public P getPoint(@NotNull AHalfEdge<P> halfEdge) {
		return getVertex(halfEdge).getPoint();
	}

	@Override
	public AVertex<P> getVertex(@NotNull AHalfEdge<P> halfEdge) {
		return vertices.get(halfEdge.getEnd());
	}

	// the vertex should not be contained in vertices, only the up/down
	@Override
	public AVertex<P> getDown(@NotNull AVertex<P> vertex) {
		return vertices.get(vertex.getDown());
	}

	// the vertex should not be contained in vertices, only the up/down
	@Override
	public void setDown(@NotNull AVertex<P> up, @NotNull AVertex<P> down) {
		up.setDown(down.getId());
	}

	@Override
	public P getPoint(@NotNull AVertex<P> vertex) {
		return vertex.getPoint();
	}

	@Override
	public AFace<P> getFace() {
		return faces.stream().filter(f -> !isDestroyed(f)).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull AFace<P> face) {
		return face == boundary;
	}

	@Override
	public boolean isBoundary(@NotNull AHalfEdge<P> halfEdge) {
		return halfEdge.getFace() == boundary.getId();
	}

	@Override
	public boolean isDestroyed(@NotNull AFace<P> face) {
		return face.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull AHalfEdge<P> edge) {
		return edge.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull AVertex<P> vertex) {
		return vertex.isDestroyed();
	}

	@Override
	public void setTwin(@NotNull AHalfEdge<P> halfEdge, @NotNull AHalfEdge<P> twin) {
		halfEdge.setTwin(twin.getId());
		twin.setTwin(halfEdge.getId());
	}

	@Override
	public void setNext(@NotNull AHalfEdge<P> halfEdge, @NotNull AHalfEdge<P> next) {
		halfEdge.setNext(next.getId());
		next.setPrevious(halfEdge.getId());
	}

	@Override
	public void setPrev(@NotNull AHalfEdge<P> halfEdge, @NotNull AHalfEdge<P> prev) {
		halfEdge.setPrevious(prev.getId());
		prev.setNext(halfEdge.getId());
	}

	@Override
	public void setFace(@NotNull AHalfEdge<P> halfEdge, @NotNull AFace<P> face) {
		halfEdge.setFace(face.getId());
	}

	@Override
	public void setEdge(@NotNull AFace<P> face, @NotNull AHalfEdge<P> edge) {
		face.setEdge(edge.getId());
	}

	@Override
	public void setEdge(@NotNull AVertex<P> vertex, @NotNull AHalfEdge<P> edge) {
		vertex.setEdge(edge.getId());
	}

	@Override
	public void setVertex(@NotNull AHalfEdge<P> halfEdge, @NotNull AVertex<P> vertex) {
		halfEdge.setEnd(vertex.getId());
	}

	@Override
	public AHalfEdge<P> createEdge(@NotNull AVertex<P> vertex) {
		int id = edges.size();
		AHalfEdge<P> edge = new AHalfEdge<>(id, vertex.getId());
		edges.add(edge);
		numberOfFaces++;
		return edge;
	}

	@Override
	public AHalfEdge<P> createEdge(@NotNull AVertex<P> vertex, @NotNull AFace<P> face) {
		int id = edges.size();
		AHalfEdge<P> edge = new AHalfEdge<>(id, vertex.getId(), face.getId());
		edges.add(edge);
		numberOfEdges++;
		return edge;
	}

	@Override
	public AFace<P> createFace() {
		int id = faces.size();
		AFace<P> face = new AFace<>(id, -1);
		faces.add(face);
		numberOfFaces++;
		return face;
	}

	@Override
	public AFace<P> createFace(boolean boundary) {
		if (boundary) {
			return this.boundary;
		} else {
			return createFace();
		}
	}

	@Override
	public P createPoint(final double x, final double y) {
		return pointConstructor.create(x, y);
	}

	@Override
	public AVertex<P> createVertex(final double x, final double y) {
		return createVertex(pointConstructor.create(x, y));
	}

	@Override
	public AVertex<P> createVertex(@NotNull final P point) {
		int id = vertices.size();
		AVertex<P> vertex = new AVertex<>(id, point);
		return vertex;
	}

	@Override
	public AFace<P> getBoundary() {
		return boundary;
	}

	@Override
	public void insert(@NotNull final AVertex<P> vertex) {
		if (vertex.getId() != vertices.size()) {
			throw new IllegalArgumentException(vertex.getId() + " != " + vertices.size());
		} else {
			numberOfVertices++;
			vertices.add(vertex);
		}
	}

	@Override
	public void insertVertex(@NotNull final AVertex<P> vertex) {
		if (vertex.getId() != vertices.size()) {
			throw new IllegalArgumentException(vertex.getId() + " != " + vertices.size());
		} else {
			numberOfVertices++;
			vertices.add(vertex);
		}
	}

	// these methods assume that all elements are contained in the mesh!
	@Override
	public void destroyFace(@NotNull final AFace<P> face) {
		if (!isDestroyed(face)) {
			elementRemoved = true;
			numberOfFaces--;
			face.destroy();
		}
	}

	@Override
	public void destroyEdge(@NotNull final AHalfEdge<P> edge) {
		if (!isDestroyed(edge)) {
			elementRemoved = true;
			numberOfEdges--;
			edge.destroy();
		}
	}

	@Override
	public void destroyVertex(@NotNull final AVertex<P> vertex) {
		if (!isDestroyed(vertex)) {
			elementRemoved = true;
			numberOfVertices--;
			vertex.destroy();
		}
	}

	@Override
	public List<AFace<P>> getFaces() {
		return streamFaces().filter(f -> !f.isDestroyed()).collect(Collectors.toList());
	}

	@Override
	public List<AHalfEdge<P>> getBoundaryEdges() {
		return streamEdges().filter(edge -> isBoundary(edge)).collect(Collectors.toList());
	}

	@Override
	public List<AVertex<P>> getBoundaryVertices() {
		return streamEdges().filter(edge -> isBoundary(edge)).map(edge -> getVertex(edge)).collect(Collectors.toList());
	}

	@Override
	public Stream<AFace<P>> streamFaces(@NotNull Predicate<AFace<P>> predicate) {
		return faces.stream().filter(f -> !f.isDestroyed()).filter(predicate);
	}

	@Override
	public Stream<AHalfEdge<P>> streamEdges() {
		log.info(edges.stream().filter(e -> !e.isDestroyed()).count());
		return edges.stream().filter(e -> !e.isDestroyed());
	}

	@Override
	public Stream<AHalfEdge<P>> streamEdgesParallel() {
		return edges.parallelStream().filter(e -> !e.isDestroyed());
	}

	@Override
	public Stream<AVertex<P>> streamVertices() {
		return vertices.stream().filter(v -> !v.isDestroyed());
	}

	@Override
	public Stream<AVertex<P>> streamVerticesParallel() {
		return vertices.parallelStream().filter(v -> !v.isDestroyed());
	}

	@Override
	public Iterable<AHalfEdge<P>> getEdgeIt() {
		return () -> streamEdges().iterator();
	}

	@Override
	public Collection<AVertex<P>> getVertices() {
		return streamVertices().collect(Collectors.toList());
	}

	@Override
	public int getNumberOfVertices() {
		return numberOfVertices;
	}

	@Override
	public int getNumberOfFaces() {
		return numberOfFaces;
	}

	@Override
	public int getNumberOfEdges() {
		return numberOfEdges;
	}

	@Override
	public boolean tryLock(@NotNull AVertex<P> vertex) {
		return vertex.getLock().tryLock();
	}

	@Override
	public void unlock(@NotNull AVertex<P> vertex) {
		vertex.getLock().unlock();
	}

	public void setPositions(final List<P> positions) {
		assert positions.size() == numberOfVertices;
		if (positions.size() != numberOfVertices) {
			throw new IllegalArgumentException("not equally many positions than vertices: " + positions.size() + " != " + numberOfVertices);
		}

		int j = 0;
		for (int i = 0; i < vertices.size(); i++) {
			if (!vertices.get(i).isDestroyed()) {
				vertices.get(i).setPoint(positions.get(j));
				j++;
			}
		}
	}

	/**
	 * removes all destroyed object from this mesh and re-arranges all indices.
	 */
	public void garbageCollection() {
		Map<Integer, Integer> faceIdMap = new HashMap<>();
		Map<Integer, Integer> edgeIdMap = new HashMap<>();
		Map<Integer, Integer> vertexIdMap = new HashMap<>();

		int i = 0;
		int j = 0;
		for (AFace<P> face : faces) {
			if (face.isDestroyed()) {
				j--;
			} else {
				faceIdMap.put(i, j);
			}
			i++;
			j++;
		}

		i = 0;
		j = 0;
		for (AHalfEdge<P> edge : edges) {
			if (edge.isDestroyed()) {
				j--;
			} else {
				edgeIdMap.put(i, j);
			}
			i++;
			j++;
		}

		i = 0;
		j = 0;
		for (AVertex<P> vertex : vertices) {
			if (vertex.isDestroyed()) {
				j--;
			} else {
				vertexIdMap.put(i, j);
			}
			i++;
			j++;
		}

		faces = faces.stream().filter(f -> !f.isDestroyed()).collect(Collectors.toList());
		edges = edges.stream().filter(e -> !e.isDestroyed()).collect(Collectors.toList());
		vertices = vertices.stream().filter(v -> !v.isDestroyed()).collect(Collectors.toList());

		i = 0;
		for (AFace<P> face : faces) {
			face.setId(faceIdMap.get(face.getId()));
			face.setEdge(edgeIdMap.get(face.getEdge()));
			assert face.getId() == i;
			i++;
		}

		i = 0;
		for (AVertex<P> vertex : vertices) {
			vertex.setId(vertexIdMap.get(vertex.getId()));
			vertex.setEdge(edgeIdMap.get(vertex.getEdge()));
			assert vertex.getId() == i;
			i++;
		}

		i = 0;
		for (AHalfEdge<P> edge : edges) {
			edge.setId(edgeIdMap.get(edge.getId()));
			edge.setEnd(vertexIdMap.get(edge.getEnd()));
			edge.setNext(edgeIdMap.get(edge.getNext()));
			edge.setPrevious(edgeIdMap.get(edge.getPrevious()));
			edge.setTwin(edgeIdMap.get(edge.getTwin()));
			if (edge.getFace() != boundary.getId()) {
				edge.setFace(faceIdMap.get(edge.getFace()));
			}

			assert edge.getId() == i;
			i++;
		}

		assert (getNumberOfVertices() == vertices.size()) && (getNumberOfEdges() == edges.size()) && (getNumberOfFaces() == faces.size());
	}
}