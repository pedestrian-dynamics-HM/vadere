package org.vadere.meshing.mesh.gen.mesh.arrayBased;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.MeshUtils;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An array-based implementation of {@link IMesh}.
 *
 * @author Benedikt Zoennchen
 */
public class AMesh implements IMesh<AVertex, AHalfEdge, AFace>, Cloneable {
	final static Logger log = Logger.getLogger(AMesh.class);
    AMeshDataStorage meshDataStorage; // todo hh: remove when making mesh immutable
    List<AFace> faces;
	boolean elementRemoved;
	int numberOfVertices;
	int numberOfEdges;
	int numberOfFaces;
	int numberOfHoles;
	List<AFace> holes;
	AFace boundary;
	List<AHalfEdge> edges;
	List<AVertex> vertices;

	public AMesh(AMeshDataStorage meshDataStorage) {
		this.meshDataStorage = meshDataStorage;
        clear();
	}

	AMesh() { // todo hh: rework when removing data storage from mesh
		clear();
	}

	@Override
	public void clear() {
		this.faces = new ArrayList<>();
		this.holes = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();
		this.boundary = new AFace(-1, true);
		this.elementRemoved = false;
		this.numberOfFaces = 0;
		this.numberOfEdges = 0;
		this.numberOfVertices = 0;
		this.numberOfHoles = 0;
	}

	@Override
	public IMesh<AVertex, AHalfEdge, AFace> constructEmpty() {
		AMesh mesh = new AMesh();
		mesh.meshDataStorage = new AMeshDataStorage(mesh); // todo hh: remove when making mesh immutable
		return mesh;
	}

	@Override
	public AHalfEdge getNext(@NotNull final AHalfEdge halfEdge) {
		if(halfEdge.getNext() == -1) {
			return null;
		}
		return edges.get(halfEdge.getNext());
	}

	@Override
	public AHalfEdge getPrev(@NotNull final AHalfEdge halfEdge) {
		if(halfEdge.getPrevious() == -1) {
			return null;
		}
		return edges.get(halfEdge.getPrevious());
	}

	@Override
	public AHalfEdge getTwin(@NotNull final AHalfEdge halfEdge) {
		if(halfEdge.getTwin() == -1) {
			return null;
		}
		return edges.get(halfEdge.getTwin());
	}

	@Override
	public AFace getFace(@NotNull final AHalfEdge halfEdge) {
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
	public AHalfEdge getEdge(@NotNull final AVertex vertex) {
		if(vertex.getEdge() == -1) {
			return null;
		}
		return edges.get(vertex.getEdge());
	}

	@Override
	public double getX(@NotNull AVertex vertex) {
		return vertex.getX();
	}

	@Override
	public double getY(@NotNull AVertex vertex) {
		return vertex.getY();
	}

	@Override
	public void setCoords(@NotNull AVertex vertex, double x, double y) {
		vertex.setPoint(new VPoint(x, y));
	}

	@Override
	public AHalfEdge getEdge(@NotNull final AFace face) {
		return edges.get(face.getEdge());
	}

	@Override
	public IPoint getPoint(@NotNull final AHalfEdge halfEdge) {
		return getVertex(halfEdge).getPoint();
	}

	@Override
	public AVertex getVertex(@NotNull final AHalfEdge halfEdge) {
		if(halfEdge.getEnd() == -1) {
			return null;
		}
		return vertices.get(halfEdge.getEnd());
	}

	// the vertex should not be contained in vertices, only the up/down
	@Override
	public AVertex getDown(@NotNull final AVertex vertex) {
		return vertices.get(vertex.getDown());
	}

	// the vertex should not be contained in vertices, only the up/down
	@Override
	public void setDown(@NotNull final AVertex up, @NotNull AVertex down) {
		up.setDown(down.getId());
	}

	@Override
	public IPoint getPoint(@NotNull final AVertex vertex) {
		return vertex.getPoint();
	}

	@Override
	public AFace getFace() {
		return faces.stream().filter(f -> !isDestroyed(f)).filter(f -> !isBoundary(f)).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull AFace face) {
		return face.isBorder();
	}

	@Override
	public boolean isBoundary(@NotNull AHalfEdge halfEdge) {
		return halfEdge.getFace() == boundary.getId() || isBoundary(getFace(halfEdge));
	}

	@Override
	public boolean isHole(@NotNull AFace face) {
		return isBoundary(face) && face != boundary;
	}

	@Override
	public boolean isDestroyed(@NotNull AFace face) {
		return face.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull AHalfEdge edge) {
		return edge.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull AVertex vertex) {
		return vertex.isDestroyed();
	}

	@Override
	public void setTwin(@NotNull AHalfEdge halfEdge, @NotNull AHalfEdge twin) {
		halfEdge.setTwin(twin.getId());
		twin.setTwin(halfEdge.getId());
	}

	@Override
	public void setNext(@NotNull AHalfEdge halfEdge, @NotNull AHalfEdge next) {
		halfEdge.setNext(next.getId());
		next.setPrevious(halfEdge.getId());
	}

	@Override
	public void setPrev(@NotNull AHalfEdge halfEdge, @NotNull AHalfEdge prev) {
		halfEdge.setPrevious(prev.getId());
		prev.setNext(halfEdge.getId());
	}

	@Override
	public void setFace(@NotNull AHalfEdge halfEdge, @NotNull AFace face) {
		halfEdge.setFace(face.getId());
	}

	@Override
	public void setEdge(@NotNull AFace face, @NotNull AHalfEdge edge) {
		face.setEdge(edge.getId());
	}

	@Override
	public void setEdge(@NotNull AVertex vertex, @NotNull AHalfEdge edge) {
		assert edge.getEnd() == vertex.getId();
		if(edge.getEnd() != vertex.getId()) {
			throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex.getId() + " != " + edge.getEnd());
		}
		vertex.setEdge(edge.getId());
	}

	@Override
	public void setVertex(@NotNull AHalfEdge halfEdge, @NotNull AVertex vertex) {
		halfEdge.setEnd(vertex.getId());
	}

	@Override
	public AHalfEdge createEdge(@NotNull AVertex vertex) {
		int id = edges.size();
		AHalfEdge edge = new AHalfEdge(id, vertex.getId());
		edges.add(edge);
		meshDataStorage.onEdgeCreated();
		numberOfEdges++;
		return edge;
	}

	@Override
	public AHalfEdge createEdge(@NotNull final AVertex vertex, @NotNull final AFace face) {
		int id = edges.size();
		AHalfEdge edge = new AHalfEdge(id, vertex.getId(), face.getId());
		edges.add(edge);
		meshDataStorage.onEdgeCreated();
		numberOfEdges++;
		return edge;
	}

	@Override
	public AFace createFace() {
		return createFace(false);
	}

	@Override
	public AFace createFace(boolean hole) {
		int id = faces.size();
		AFace face = new AFace(id, -1, hole);
		faces.add(face);
		meshDataStorage.onFaceCreated();

		if(!hole) {
			numberOfFaces++;
		}
		else {
			holes.add(face);
			numberOfHoles++;
		}
		return face;
	}

	@Override
	public IPoint createPoint(final double x, final double y) {
		return new VPoint(x, y);
	}

	@Override
	public AVertex createVertex(final double x, final double y) {
		return createVertex(createPoint(x, y));
	}

	@Override
	public AVertex createVertex(@NotNull final IPoint point) {
		int id = vertices.size();
		meshDataStorage.onVertexCreated();
		return new AVertex(id, point);
	}

	@Override
	public AFace getBorder() {
		return boundary;
	}

	@Override
	public void insert(@NotNull final AVertex vertex) {
		if (vertex.getId() != vertices.size()) {
			throw new IllegalArgumentException(vertex.getId() + " != " + vertices.size());
		} else {
			numberOfVertices++;
			vertices.add(vertex);
		}
	}

	@Override
	public void insertVertex(@NotNull final AVertex vertex) {
		if (vertex.getId() != vertices.size()) {
			throw new IllegalArgumentException(vertex.getId() + " != " + vertices.size());
		} else {
			numberOfVertices++;
			vertices.add(vertex);
		}
	}

	@Override
	public void toHole(@NotNull AFace face) {
		assert !isDestroyed(face);
		if(!isHole(face)) {
			holes.add(face);
			face.setBorder(true);
			numberOfHoles++;
			numberOfFaces--;
		}
	}

	// these methods assume that all elements are contained in the mesh!
	@Override
	public void destroyFace(@NotNull final AFace face) {
		if (!isDestroyed(face)) {
			elementRemoved = true;
			numberOfFaces--;

			if(isHole(face)) {
				numberOfHoles--;
			}

			face.destroy();
		}
	}

	@Override
	public void destroyEdge(@NotNull final AHalfEdge edge) {
		if (!isDestroyed(edge)) {
			elementRemoved = true;
			numberOfEdges--;
			edge.destroy();
		}
	}

	@Override
	public void destroyVertex(@NotNull final AVertex vertex) {
		if (!isDestroyed(vertex)) {
			elementRemoved = true;
			numberOfVertices--;
			vertex.destroy();
		}
	}

	@Override
	public void setPoint(@NotNull final AVertex vertex, @NotNull final IPoint point) {
		vertex.setPoint(point);
	}

	@Override
	public Stream<AFace> streamFaces(@NotNull final Predicate<AFace> predicate) {
		return faces.stream().filter(f -> isAlive(f)).filter(predicate);
	}

	@Override
	public Stream<AFace> streamHoles() {
		return holes.stream().filter(f -> !isDestroyed(f));
	}

	@Override
	public Stream<AHalfEdge> streamEdges() {
		return edges.stream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<AHalfEdge> streamEdgesParallel() {
		return edges.parallelStream().filter(e -> !e.isDestroyed());
	}

	@Override
	public Stream<AVertex> streamVertices() {
		return vertices.stream().filter(v -> !v.isDestroyed());
	}

	@Override
	public Stream<AVertex> streamVerticesParallel() {
		return vertices.parallelStream().filter(v -> !v.isDestroyed());
	}

	@Override
	public Iterable<AHalfEdge> getEdgeIt() {
		return () -> streamEdges().iterator();
	}

	@Override
	public AVertex getRandomVertex(@NotNull Random random) {
		int startIndex = random.nextInt(vertices.size());
		int index = startIndex;

		// look above
		while (index < vertices.size() && isDestroyed(vertices.get(index))) {
			index++;
		}

		// look below
		if(isDestroyed(vertices.get(index))) {
			index = startIndex - 1;

			while (index >= 0 && isDestroyed(vertices.get(index))) {
				index--;
			}
		}

		return vertices.get(index);
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
	public int getNumberOfHoles() {
		return numberOfHoles;
	}

	@Override
	public boolean tryLock(@NotNull AVertex vertex) {
		return vertex.getLock().tryLock();
	}

	@Override
	public void unlock(@NotNull AVertex vertex) {
		vertex.getLock().unlock();
	}

	@Override
    public synchronized AMesh clone() {
        try {
            AMesh clone = (AMesh)super.clone();

            List<AFace> cFaces = faces.stream().map(f -> f.clone()).collect(Collectors.toList());
            List<AHalfEdge> cEdges = edges.stream().map(e -> e.clone()).collect(Collectors.toList());
            List<AVertex> cVertices = vertices.stream().map(v -> v.clone()).collect(Collectors.toList());

            clone.faces = cFaces;
            clone.edges = cEdges;
            clone.vertices = cVertices;

            // here we assume that the point-constructor is stateless!
            clone.boundary = boundary.clone();
            return clone;

        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

	public void setPositions(final List<IPoint> positions) {
		assert positions.size() == numberOfVertices;
		if (positions.size() != numberOfVertices) {
			throw new IllegalArgumentException("not equally many positions than vertices: " + positions.size() + " != " + numberOfVertices);
		}

		int j = 0;
		for (AVertex vertex : vertices) {
			if (!vertex.isDestroyed()) {
				vertex.setPoint(positions.get(j));
				j++;
			}
		}
	}



	@Override
	public IMeshDataStorage<AVertex, AHalfEdge, AFace> createEmptyDataStorage() {
		return new AMeshDataStorage(this);
	}

	/**
	 * <p>Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0)).</p>
	 *
	 * @return the created mesh
	 */
	public static AMeshWithDataStorage createSimpleTriMesh() {
		IMeshWithDataStorage<AVertex, AHalfEdge, AFace> meshWithDataStorage = AMeshWithDataStorage.constructEmpty();
		MeshUtils.createSimpleTriMesh(meshWithDataStorage.getMesh()); // todo hh: rework
		return (AMeshWithDataStorage) meshWithDataStorage;
	}
}