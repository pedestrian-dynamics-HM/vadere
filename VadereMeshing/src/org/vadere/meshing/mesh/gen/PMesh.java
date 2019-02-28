package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class PMesh<P extends IPoint, CE, CF> implements IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> {

	private static Logger log = Logger.getLogger(PMesh.class);

	private int numberOfEdges;
	private int numberOfFaces;
	private int numberOfHoles;
	private int numberOfVertices;

	private List<PFace<P, CE, CF>> faces;
	private List<PFace<P, CE, CF>> holes;
	private PFace<P, CE, CF> boundary;
	private List<PHalfEdge<P, CE, CF>> edges;
	private IPointConstructor<P> pointConstructor;
	private List<PVertex<P, CE, CF>> vertices;
	private @Nullable final Supplier<CE> edgeSupplier;
	private @Nullable final Supplier<CF> faceSupplier;

	public PMesh(@NotNull final IPointConstructor<P> pointConstructor,
	             @Nullable final Supplier<CE> edgeSupplier,
	             @Nullable final Supplier<CF> faceSupplier) {
		clear();
		this.pointConstructor = pointConstructor;
		this.edgeSupplier = edgeSupplier;
		this.faceSupplier = faceSupplier;
	}

	public PMesh(@NotNull final IPointConstructor<P> pointConstructor,
	             @Nullable final Supplier<CE> edgeSupplier) {
		this(pointConstructor, edgeSupplier, null);
	}

	public PMesh(final IPointConstructor<P> pointConstructor) {
		this(pointConstructor, null, null);
	}

	@Override
	public void clear() {
		this.faces = new ArrayList<>();
		this.holes = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();
		this.boundary = new PFace<>(true);
		this.numberOfEdges = 0;
		this.numberOfFaces = 0;
		this.numberOfHoles = 0;
		this.numberOfVertices = 0;
	}

	@Override
	public PMesh<P, CE, CF> construct() {
		return new PMesh<>(pointConstructor);
	}

	@Override
	public void garbageCollection() {
		faces = faces.stream().filter(f -> !isDestroyed(f)).collect(Collectors.toList());
		edges = edges.stream().filter(e -> !isDestroyed(e)).collect(Collectors.toList());
		vertices = vertices.stream().filter(v -> !isDestroyed(v)).collect(Collectors.toList());
	}

	@Override
	public PHalfEdge<P, CE, CF> getNext(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return halfEdge.getNext();
	}

	@Override
	public PHalfEdge<P, CE, CF> getPrev(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return halfEdge.getPrevious();
	}

	@Override
	public PHalfEdge<P, CE, CF> getTwin(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return halfEdge.getTwin();
	}

	@Override
	public PFace<P, CE, CF> getFace(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return halfEdge.getFace();
	}

	@Override
	public IPointConstructor<P> getPointConstructor() {
		return pointConstructor;
	}

	@Override
	public PHalfEdge<P, CE, CF> getEdge(@NotNull final PVertex<P, CE, CF> vertex) {
		return vertex.getEdge();
	}

	@Override
	public PHalfEdge<P, CE, CF> getEdge(@NotNull final PFace<P, CE, CF> face) {
		return face.getEdge();
	}

	@Override
	public P getPoint(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return getVertex(halfEdge).getPoint();
	}

	@Override
	public PVertex<P, CE, CF> getVertex(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return halfEdge.getEnd();
	}

	@Override
	public P getPoint(@NotNull final PVertex<P, CE, CF> vertex) {
		return vertex.getPoint();
	}

	@Override
	public Optional<CF> getData(@NotNull final PFace<P, CE, CF> face) {
		return Optional.ofNullable(face.getData());
	}

	@Override
	public void setData(@NotNull final PFace<P, CE, CF> face, @Nullable final CF data) {
		face.setData(data);
	}

	@Override
	public Optional<CE> getData(@NotNull final PHalfEdge<P, CE, CF> edge) {
		return Optional.ofNullable(edge.getData());
	}

	@Override
	public void setData(@NotNull PHalfEdge<P, CE, CF> edge, @Nullable CE data) {
		edge.setData(data);
	}

	@Override
	public PFace<P, CE, CF> getFace() {
		return faces.stream().filter(face -> !isDestroyed(face)).filter(f -> !isBoundary(f)).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull final PFace<P, CE, CF> face) {
		return face.isBoundary();
	}

	@Override
	public boolean isHole(@NotNull final PFace<P, CE, CF> face) {
		return isBoundary(face) && face != boundary;
	}

	@Override
	public boolean isBoundary(@NotNull final PHalfEdge<P, CE, CF> halfEdge) {
		return halfEdge.isBoundary();
	}

	@Override
	public boolean isDestroyed(@NotNull final PFace<P, CE, CF> face) {
		return face.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull final PHalfEdge<P, CE, CF> edge) {
		return !edge.isValid();
	}

	@Override
	public boolean isDestroyed(@NotNull final PVertex<P, CE, CF> vertex) {
		return vertex.isDestroyed();
	}

	@Override
	public void setTwin(@NotNull final PHalfEdge<P, CE, CF> halfEdge, @NotNull final PHalfEdge<P, CE, CF> twin) {
		halfEdge.setTwin(twin);
		twin.setTwin(halfEdge);
	}

	@Override
	public void setNext(@NotNull final PHalfEdge<P, CE, CF> halfEdge, @NotNull final PHalfEdge<P, CE, CF> next) {
		halfEdge.setNext(next);
		next.setPrevious(halfEdge);
	}

	@Override
	public void setPrev(@NotNull final PHalfEdge<P, CE, CF> halfEdge, @NotNull final PHalfEdge<P, CE, CF> prev) {
		halfEdge.setPrevious(prev);
		prev.setNext(halfEdge);
	}

	@Override
	public void setFace(@NotNull final PHalfEdge<P, CE, CF> halfEdge, @NotNull final PFace<P, CE, CF> face) {
		halfEdge.setFace(face);
	}

	@Override
	public void setEdge(@NotNull final PFace<P, CE, CF> face, @NotNull final PHalfEdge<P, CE, CF> edge) {
		face.setEdge(edge);
	}

	@Override
	public void setEdge(@NotNull final PVertex<P, CE, CF> vertex, @NotNull final PHalfEdge<P, CE, CF> edge) {
		assert edge.getEnd().equals(vertex);
		if(!edge.getEnd().equals(vertex)) {
			throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex + " != " + edge.getEnd());
		}
		vertex.setEdge(edge);
	}

	@Override
	public void setVertex(@NotNull final PHalfEdge<P, CE, CF> halfEdge, @NotNull final PVertex<P, CE, CF> vertex) {
		/*if(halfEdge.getEnd().getEdge() == halfEdge) {
			System.out.println("error44");
		}


		if(!vertex.getEdge().getEnd().equals(vertex)) {
			System.out.println("error2");
		}*/
		halfEdge.setEnd(vertex);
	}

	/*@Override
	public List<PHalfEdge<P>> getEdges(@NotNull final PVertex<P> vertex) {
		return
		//return streamEdges().filter(edge -> !edge.isValid()).filter(edge -> getVertex(edge).equals(vertex)).collect(Collectors.toList());
	}*/

	@Override
	public int getNumberOfVertices() {
		return numberOfVertices;
	}

	@Override
	public int getNumberOfFaces() {
		return numberOfFaces;
	}

	@Override
	public boolean tryLock(@NotNull final PVertex<P, CE, CF> vertex) {
		return vertex.getLock().tryLock();
	}

	@Override
	public void unlock(@NotNull final PVertex<P, CE, CF> vertex) {
		vertex.getLock().unlock();
	}

	@Override
	public PHalfEdge<P, CE, CF> createEdge(@NotNull final PVertex<P, CE, CF> vertex) {
		PHalfEdge<P, CE, CF> edge = new PHalfEdge<>(vertex);
		edges.add(edge);

		if(edgeSupplier != null) {
			edge.setData(edgeSupplier.get());
		}

		numberOfEdges++;
		return edge;
	}

	@Override
	public PHalfEdge<P, CE, CF> createEdge(@NotNull final PVertex<P, CE, CF> vertex, @NotNull final PFace<P, CE, CF> face) {
		PHalfEdge<P, CE, CF> edge = new PHalfEdge<>(vertex, face);
		edges.add(edge);

		if(edgeSupplier != null) {
			edge.setData(edgeSupplier.get());
		}

		numberOfEdges++;
		return edge;
	}

	@Override
	public PFace<P, CE, CF> createFace() {
		return createFace(false);
	}

	@Override
	public PFace<P, CE, CF> createFace(final boolean hole) {
		PFace<P, CE, CF> face = new PFace<>(hole);
		faces.add(face);

		if(faceSupplier != null) {
			setData(face, faceSupplier.get());
		}

		if(hole) {
			numberOfHoles++;
			holes.add(face);
		}
		else {
			numberOfFaces++;
		}
		return face;
	}

	@Override
	public P createPoint(double x, double y) {
		return pointConstructor.create(x, y);
	}


	// TODO: maybe remove insertVertex!
	@Override
	public PVertex<P, CE, CF> createVertex(double x, double y) {
		return createVertex(pointConstructor.create(x, y));
	}

	@Override
	public PVertex<P, CE, CF> createVertex(@NotNull final P point) {
		return new PVertex<>(point);
	}

	@Override
	public PFace<P, CE, CF> getBorder() {
		return boundary;
	}

	@Override
	public void insert(@NotNull final PVertex<P, CE, CF> vertex) {
		numberOfVertices++;
		vertices.add(vertex);
	}

	@Override
	public void insertVertex(@NotNull final PVertex<P, CE, CF> vertex) {
		numberOfVertices++;
		vertices.add(vertex);
	}

	@Override
	public void toHole(@NotNull final PFace<P, CE, CF> face) {
		assert !isHole(face);
		if(!isHole(face)) {
			holes.add(face);
			face.setBoundary(true);
			numberOfHoles++;
			numberOfFaces--;
		}
	}

	@Override
	public void destroyFace(@NotNull final PFace<P, CE, CF> face) {
		//faces.remove(face);
		if(isHole(face)) {
			//holes.remove(face);
			numberOfHoles--;
		}
		else {
			numberOfFaces--;
		}
		face.destroy();
	}

	@Override
	public void destroyEdge(@NotNull final PHalfEdge<P, CE, CF> edge) {
		//edges.remove(edge);
		edge.destroy();
		numberOfEdges--; // we destroy the edge and its twin!
	}

	@Override
	public void setDown(@NotNull final PVertex<P, CE, CF> up, @NotNull final PVertex<P, CE, CF> down) {
		up.setDown(down);
	}

	@Override
	public PVertex<P, CE, CF> getDown(@NotNull final PVertex<P, CE, CF> vertex) {
		return vertex.getDown();
	}

	@Override
	public void destroyVertex(@NotNull final PVertex<P, CE, CF> vertex) {
		//vertices.remove(vertex);
		if(!isDestroyed(vertex)) {
			vertex.destroy();
			numberOfVertices--;
		}
	}

	@Override
	public void setPoint(@NotNull final PVertex<P, CE, CF> vertex, @NotNull final P point) {
		vertex.setPoint(point);
	}

	@Override
	public Stream<PHalfEdge<P, CE, CF>> streamEdges() {
		return edges.stream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<PHalfEdge<P, CE, CF>> streamEdgesParallel() {
		return edges.parallelStream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<PVertex<P, CE, CF>> streamVertices() { return vertices.stream().filter(v -> !isDestroyed(v)); }

	@Override
	public Stream<PVertex<P, CE, CF>> streamVerticesParallel() {
		return vertices.parallelStream().filter(v -> !isDestroyed(v));
	}

	@Override
	public Iterable<PHalfEdge<P, CE, CF>> getEdgeIt() {
		return () -> edges.iterator();
	}

	@Override
	public PVertex<P, CE, CF> getRandomVertex(@NotNull final Random random) {
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
	public Stream<PFace<P, CE, CF>> streamFaces(@NotNull final Predicate<PFace<P, CE, CF>> predicate) {
		return faces.stream().filter(f -> !isDestroyed(f)).filter(predicate);
	}

	@Override
	public Stream<PFace<P, CE, CF>> streamHoles() {
		return holes.stream().filter(h -> !isDestroyed(h));
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
	public IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> toTriangulation(@NotNull final IPointLocator.Type type) {
		return IIncrementalTriangulation.createPTriangulation(type, this);
	}

	@Override
	public void arrangeMemory(@NotNull final Iterable<PFace<P, CE, CF>> faceOrder) {
		try {
			throw new UnsupportedOperationException("not jet implemented.");
		} catch (UnsupportedOperationException e) {
			log.warn(e.getMessage());
		}
	}

	@Override
	public synchronized PMesh<P, CE, CF> clone() {
		try {
			PMesh<P, CE, CF> clone = (PMesh<P, CE, CF>)super.clone();
			clone.pointConstructor = pointConstructor;
			clone.faces = new ArrayList<>();
			clone.holes = new ArrayList<>();
			clone.edges = new ArrayList<>();
			clone.vertices = new ArrayList<>();
			clone.numberOfVertices = numberOfVertices;
			clone.numberOfEdges = numberOfEdges;
			clone.numberOfHoles = numberOfHoles;
			clone.numberOfFaces = numberOfFaces;

			Map<PVertex<P, CE, CF>, PVertex<P, CE, CF>> vertexMap = new HashMap<>();
			Map<PHalfEdge<P, CE, CF>, PHalfEdge<P, CE, CF>> edgeMap = new HashMap<>();
			Map<PFace<P, CE, CF>, PFace<P, CE, CF>> faceMap = new HashMap<>();

			// faces are not complete: missing edge
			for(PVertex<P, CE, CF> v : vertices) {
				PVertex<P, CE, CF> cV = v.clone();
				clone.vertices.add(cV);
				vertexMap.put(v, cV);
			}

			// edges are not complete: missing next, prev, twin, face
			for(PHalfEdge<P, CE, CF> e : edges) {
				PHalfEdge<P, CE, CF> cE = e.clone();
				edgeMap.put(e, cE);
				cE.setEnd(vertexMap.get(e.getEnd()));
				clone.edges.add(cE);
			}

			// faces are complete
			clone.boundary = boundary.clone();
			faceMap.put(boundary, clone.boundary);
			clone.boundary.setEdge(edgeMap.get(boundary.getEdge()));
			for(PFace<P, CE, CF> f : faces) {
				PFace<P, CE, CF> cF = f.clone();
				faceMap.put(f, cF);
				cF.setEdge(edgeMap.get(f.getEdge()));
				clone.faces.add(cF);
			}

			for(PVertex<P, CE, CF> cV : clone.vertices) {
				cV.setEdge(edgeMap.get(cV.getEdge()));
				cV.setDown(null);
			}

			for(PHalfEdge<P, CE, CF> cE : clone.edges) {
				cE.setFace(faceMap.get(cE.getFace()));
				cE.setNext(edgeMap.get(cE.getNext()));
				cE.setPrevious(edgeMap.get(cE.getPrevious()));
				cE.setTwin(edgeMap.get(cE.getTwin()));
			}

			// here we assume that the point-constructor is stateless!

			return clone;

		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.getMessage());
		}
	}
}
