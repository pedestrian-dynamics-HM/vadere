package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.SpaceFillingCurve;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.MPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An array-based implementation of {@link IMesh}.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 */
public class AMesh<P extends IPoint, CE, CF> implements IMesh<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>>, Cloneable {
	private final static Logger log = Logger.getLogger(AMesh.class);
	private List<AFace<CF>> faces;
	private boolean elementRemoved;
	private int numberOfVertices;
	private int numberOfEdges;
	private int numberOfFaces;
	private int numberOfHoles;
	private List<AFace<CF>> holes;
	private AFace<CF> boundary;
	private List<AHalfEdge<CE>> edges;
	private IPointConstructor<P> pointConstructor;
	private List<AVertex<P>> vertices;

	public AMesh(final IPointConstructor<P> pointConstructor) {
		clear();
		this.pointConstructor = pointConstructor;
	}

	@Override
	public void clear() {
		this.faces = new ArrayList<>();
		this.holes = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();
		this.boundary = new AFace<>(-1, true);
		this.elementRemoved = false;
		this.numberOfFaces = 0;
		this.numberOfEdges = 0;
		this.numberOfVertices = 0;
		this.numberOfHoles = 0;
	}

	@Override
	public IMesh<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> construct() {
		return new AMesh<>(pointConstructor);
	}

	@Override
	public IPointConstructor<P> getPointConstructor() {
		return pointConstructor;
	}

	@Override
	public AHalfEdge<CE> getNext(@NotNull final AHalfEdge<CE> halfEdge) {
		return edges.get(halfEdge.getNext());
	}

	@Override
	public AHalfEdge<CE> getPrev(@NotNull final AHalfEdge<CE> halfEdge) {
		return edges.get(halfEdge.getPrevious());
	}

	@Override
	public AHalfEdge<CE> getTwin(@NotNull final AHalfEdge<CE> halfEdge) {
		return edges.get(halfEdge.getTwin());
	}

	@Override
	public AFace<CF> getFace(@NotNull final AHalfEdge<CE> halfEdge) {
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
	public AHalfEdge<CE> getEdge(@NotNull final AVertex<P> vertex) {
		return edges.get(vertex.getEdge());
	}

	@Override
	public AHalfEdge<CE> getEdge(@NotNull final AFace<CF> face) {
		try {
			edges.get(face.getEdge());
		}
		catch (ArrayIndexOutOfBoundsException e) {
			edges.get(face.getEdge());
		}
		return edges.get(face.getEdge());
	}

	@Override
	public P getPoint(@NotNull final AHalfEdge<CE> halfEdge) {
		return getVertex(halfEdge).getPoint();
	}

	@Override
	public AVertex<P> getVertex(@NotNull final AHalfEdge<CE> halfEdge) {
		return vertices.get(halfEdge.getEnd());
	}

	// the vertex should not be contained in vertices, only the up/down
	@Override
	public AVertex<P> getDown(@NotNull final AVertex<P> vertex) {
		return vertices.get(vertex.getDown());
	}

	// the vertex should not be contained in vertices, only the up/down
	@Override
	public void setDown(@NotNull final AVertex<P> up, @NotNull AVertex<P> down) {
		up.setDown(down.getId());
	}

	@Override
	public P getPoint(@NotNull final AVertex<P> vertex) {
		return vertex.getPoint();
	}

	@Override
	public Optional<CE> getData(@NotNull final AHalfEdge<CE> edge) {
		return Optional.ofNullable(edge.getData());
	}

	@Override
	public void setData(@NotNull final AHalfEdge<CE> edge, @Nullable final CE data) {
		edge.setData(data);
	}

	@Override
	public Optional<CF> getData(@NotNull final AFace<CF> face) {
		return Optional.ofNullable(face.getData());
	}

	@Override
	public void setData(@NotNull final AFace<CF> edge, @Nullable final CF data) {
		edge.setData(data);
	}

	@Override
	public AFace<CF> getFace() {
		return faces.stream().filter(f -> !isDestroyed(f)).filter(f -> !isBoundary(f)).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull AFace<CF> face) {
		return face.isBorder();
	}

	@Override
	public boolean isBoundary(@NotNull AHalfEdge<CE> halfEdge) {
		return halfEdge.getFace() == boundary.getId() || isBoundary(getFace(halfEdge));
	}

	@Override
	public boolean isHole(@NotNull AFace<CF> face) {
		return isBoundary(face) && face != boundary;
	}

	@Override
	public boolean isDestroyed(@NotNull AFace<CF> face) {
		return face.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull AHalfEdge<CE> edge) {
		return edge.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull AVertex<P> vertex) {
		return vertex.isDestroyed();
	}

	@Override
	public void setTwin(@NotNull AHalfEdge<CE> halfEdge, @NotNull AHalfEdge<CE> twin) {
		halfEdge.setTwin(twin.getId());
		twin.setTwin(halfEdge.getId());
	}

	@Override
	public void setNext(@NotNull AHalfEdge<CE> halfEdge, @NotNull AHalfEdge<CE> next) {
		halfEdge.setNext(next.getId());
		next.setPrevious(halfEdge.getId());
	}

	@Override
	public void setPrev(@NotNull AHalfEdge<CE> halfEdge, @NotNull AHalfEdge<CE> prev) {
		halfEdge.setPrevious(prev.getId());
		prev.setNext(halfEdge.getId());
	}

	@Override
	public void setFace(@NotNull AHalfEdge<CE> halfEdge, @NotNull AFace<CF> face) {
		halfEdge.setFace(face.getId());
	}

	@Override
	public void setEdge(@NotNull AFace<CF> face, @NotNull AHalfEdge<CE> edge) {
		face.setEdge(edge.getId());
	}

	@Override
	public void setEdge(@NotNull AVertex<P> vertex, @NotNull AHalfEdge<CE> edge) {
		assert edge.getEnd() == vertex.getId();
		if(edge.getEnd() != vertex.getId()) {
			throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex.getId() + " != " + edge.getEnd());
		}
		vertex.setEdge(edge.getId());
	}

	@Override
	public void setVertex(@NotNull AHalfEdge<CE> halfEdge, @NotNull AVertex<P> vertex) {
		halfEdge.setEnd(vertex.getId());
	}

	@Override
	public AHalfEdge<CE> createEdge(@NotNull AVertex<P> vertex) {
		int id = edges.size();
		AHalfEdge<CE> edge = new AHalfEdge<>(id, vertex.getId());
		edges.add(edge);
		numberOfEdges++;
		return edge;
	}

	@Override
	public AHalfEdge<CE> createEdge(@NotNull final AVertex<P> vertex, @NotNull final AFace<CF> face) {
		int id = edges.size();
		AHalfEdge<CE> edge = new AHalfEdge<>(id, vertex.getId(), face.getId());
		edges.add(edge);
		numberOfEdges++;
		return edge;
	}

	@Override
	public AFace<CF> createFace() {
		return createFace(false);
	}

	@Override
	public AFace<CF> createFace(boolean hole) {
		int id = faces.size();
		AFace<CF> face = new AFace<>(id, -1, hole);
		faces.add(face);
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
	public AFace<CF> getBorder() {
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

	@Override
	public void toHole(@NotNull AFace<CF> face) {
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
	public void destroyFace(@NotNull final AFace<CF> face) {
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
	public void destroyEdge(@NotNull final AHalfEdge<CE> edge) {
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
	public void setPoint(AVertex<P> vertex, P point) {
		vertex.setPoint(point);
	}

	@Override
	public Stream<AFace<CF>> streamFaces(@NotNull Predicate<AFace<CF>> predicate) {
		return faces.stream().filter(f -> isAlive(f)).filter(predicate);
	}

	@Override
	public Stream<AFace<CF>> streamHoles() {
		return holes.stream().filter(f -> !isDestroyed(f));
	}

	@Override
	public Stream<AHalfEdge<CE>> streamEdges() {
		return edges.stream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<AHalfEdge<CE>> streamEdgesParallel() {
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
	public Iterable<AHalfEdge<CE>> getEdgeIt() {
		return () -> streamEdges().iterator();
	}

	@Override
	public AVertex<P> getRandomVertex(@NotNull Random random) {
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
	public boolean tryLock(@NotNull AVertex<P> vertex) {
		return vertex.getLock().tryLock();
	}

	@Override
	public void unlock(@NotNull AVertex<P> vertex) {
		vertex.getLock().unlock();
	}

	@Override
    public synchronized AMesh<P, CE, CF> clone() {
        try {
            AMesh<P, CE, CF> clone = (AMesh<P, CE, CF>)super.clone();

            List<AFace<CF>> cFaces = faces.stream().map(f -> f.clone()).collect(Collectors.toList());
            List<AHalfEdge<CE>> cEdges = edges.stream().map(e -> e.clone()).collect(Collectors.toList());
            List<AVertex<P>> cVertices = vertices.stream().map(v -> v.clone()).collect(Collectors.toList());

            clone.faces = cFaces;
            clone.edges = cEdges;
            clone.vertices = cVertices;

            // here we assume that the point-constructor is stateless!
            clone.pointConstructor = pointConstructor;
            clone.boundary = boundary.clone();
            return clone;

        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

	@Override
	public IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> toTriangulation(final @NotNull IPointLocator.Type type) {
		return IIncrementalTriangulation.createATriangulation(type, this);
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
     * <p>Rearranges all indices of faces, vertices and halfEdges of the mesh according to
     * the {@link Iterable} faceOrder. All indices start at 0 and will be incremented one by one.
     * For example, the vertices of the first face of faceOrder will receive id 0,1 and 2.</p>
     *
     * <p>Note: that every mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
     * <p>Assumption: faceOrder contains all faces of this mesh.</p>
     * <p>Invariant: the geometry i.e. the connectivity and the vertex positions will not change.</p>
     *
     * @param faceOrder the new order
     */
    public void arrangeMemory(@NotNull Iterable<AFace<CF>> faceOrder) {
        // clone the old one!
        AMesh<P, CE, CF> cMesh = clone();

        // merge some of them?
        int nullIdentifier = -2;

        // rebuild
        faces.clear();
        edges.clear();
        vertices.clear();
        holes.clear();

        int[] edgeMap = new int[cMesh.edges.size()];
        int[] vertexMap = new int[cMesh.vertices.size()];
        int[] faceMap = new int[cMesh.faces.size()];

        Arrays.fill(edgeMap, nullIdentifier);
        Arrays.fill(vertexMap, nullIdentifier);
        Arrays.fill(faceMap, nullIdentifier);

        // adjust all id's in order of faceOrder
        for(AFace<CF> face : faceOrder) {
            copyFace(face, vertexMap, edgeMap, faceMap, cMesh);
        }

	    // adjust all id's not contained in faceOrder in any order
	    for(AFace<CF> face : cMesh.faces) {
        	if(!isDestroyed(face)) {
		        copyFace(face, vertexMap, edgeMap, faceMap, cMesh);
	        }
	    }

        // repair the rest
        for(AFace<CF> face : faces) {
	        face.setEdge(edgeMap[face.getEdge()]);
        }

        for(AHalfEdge<CE> halfEdge : edges) {
        	if(vertexMap[halfEdge.getEnd()] == nullIdentifier) {
		        vertexMap[halfEdge.getEnd()] = vertices.size();
	        }
            halfEdge.setEnd(vertexMap[halfEdge.getEnd()]);

            // boundary face
            if(halfEdge.getFace() != boundary.getId()) {
                halfEdge.setFace(faceMap[halfEdge.getFace()]);
            }
            else {
                halfEdge.setFace(boundary.getId());
            }

            halfEdge.setTwin(edgeMap[halfEdge.getTwin()]);
            halfEdge.setPrevious(edgeMap[halfEdge.getPrevious()]);
            halfEdge.setNext(edgeMap[halfEdge.getNext()]);
        }

        for(AVertex<P> vertex : vertices) {
            vertex.setDown(vertexMap[vertex.getDown()]);
            vertex.setEdge(edgeMap[vertex.getEdge()]);
        }

        // fix the boundary
        boundary.setEdge(edgeMap[boundary.getEdge()]);
    }

    private void copyFace(@NotNull final AFace<CF> face, @NotNull int[] vertexMap, @NotNull int[] edgeMap, @NotNull int[] faceMap, @NotNull final AMesh<P, CE, CF> cMesh) {
	    // merge some of them?
	    int nullIdentifier = -2;

	    // face not jet copied
	    if(faceMap[face.getId()] == nullIdentifier) {
		    AFace<CF> fClone = face.clone();

		    // 1. face
		    faceMap[face.getId()] = faces.size();
		    fClone.setId(faces.size());
		    faces.add(fClone);

		    if(cMesh.isHole(face)){
		    	holes.add(fClone);
			}

		    // 2. vertices
		    for(AVertex<P> v : cMesh.getVertexIt(face)) {
			    if(vertexMap[v.getId()] == nullIdentifier) {
				    vertexMap[v.getId()] = vertices.size();
				    AVertex<P> cVertex = v.clone();
				    cVertex.setId(vertices.size());
				    vertices.add(cVertex);
			    }
		    }

		    // 3. edges
		    for(AHalfEdge<CE> halfEdge : cMesh.getEdgeIt(face)) {

			    // origin
			    if(edgeMap[halfEdge.getId()] == nullIdentifier) {
				    edgeMap[halfEdge.getId()] = edges.size();
				    AHalfEdge<CE> cHalfEdge = halfEdge.clone();
				    cHalfEdge.setId(edges.size());
				    edges.add(cHalfEdge);
			    }

			    // twin
			    halfEdge = cMesh.getTwin(halfEdge);
			    if(edgeMap[halfEdge.getId()] == nullIdentifier) {
				    // origin
				    edgeMap[halfEdge.getId()] = edges.size();
				    AHalfEdge<CE> cHalfEdge = halfEdge.clone();
				    cHalfEdge.setId(edges.size());
				    edges.add(cHalfEdge);
			    }
		    }
	    }
    }

    /**
     * <p>This method rearranges the indices of faces, vertices and edges according to their positions.
     * After the call, neighbouring faces are near arrange inside the face {@link ArrayList}.</p>
     *
     * <p>Note: that any mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
     */
    public void spatialSort() {
        // get the bound for the space filling curve!
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        List<VPoint> centroids = new ArrayList<>(this.numberOfFaces);

        for(int i = 0; i < this.faces.size(); i++) {
            VPoint incenter = GeometryUtils.getPolygonCentroid(this.getVertices(faces.get(i)));
            centroids.add(incenter);
            maxX = Math.max(maxX, incenter.getX());
            maxY = Math.max(maxY, incenter.getY());

            minX = Math.min(minX, incenter.getX());
            minY = Math.min(minY, incenter.getY());
        }

        SpaceFillingCurve spaceFillingCurve = new SpaceFillingCurve(new VRectangle(minX, minY, maxX-minX, maxY-minY));

        // TODO: implement faster sorting using radix sort see: http://www.diss.fu-berlin.de/diss/servlets/MCRFileNodeServlet/FUDISS_derivate_000000003494/2_kap2.pdf?hosts=
        // page 18
        List<AFace<CF>> sortedFaces = new ArrayList<>(faces.size());
        sortedFaces.addAll(this.faces);
        Collections.sort(sortedFaces, (f1, f2) -> {
            double i1 = spaceFillingCurve.compute(centroids.get(f1.getId()));
            double i2 = spaceFillingCurve.compute(centroids.get(f2.getId()));

            if (i1 < i2) {
                return -1;
            } else if (i1 > i2) {
                return 1;
            } else {
                return 0;
            }
        });
        arrangeMemory(sortedFaces);
    }



	/**
	 * <p>Removes all destroyed object from this mesh and re-arranges all indices.</p>
     *
     * <p>Note: that any mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
	 */
	public void garbageCollection() {
		Map<Integer, Integer> faceIdMap = new HashMap<>();
		Map<Integer, Integer> edgeIdMap = new HashMap<>();
		Map<Integer, Integer> vertexIdMap = new HashMap<>();

		int i = 0;
		int j = 0;
		for (AFace<CF> face : faces) {
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
		for (AHalfEdge<CE> edge : edges) {
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
		for (AFace<CF> face : faces) {
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
		for (AHalfEdge<CE> edge : edges) {
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

		assert (getNumberOfVertices() == vertices.size()) && (getNumberOfEdges() == edges.size()) && (getNumberOfFaces() == faces.size()-holes.size());
	}

	/**
	 * <p>Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0)).</p>
	 *
	 * @return the created mesh
	 */
	public static AMesh<MPoint, ?, ?> createSimpleTriMesh() {
		AMesh<MPoint, ?, ?> mesh = new AMesh<>((x1, y1) -> new MPoint(x1, y1));
		IMesh.createSimpleTriMesh(mesh);
		return mesh;
	}
}