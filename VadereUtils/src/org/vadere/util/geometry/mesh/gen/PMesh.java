package org.vadere.util.geometry.mesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.triangulation.IPointConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class PMesh<P extends IPoint> implements IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> {

	private static Logger log = LogManager.getLogger(PMesh.class);

	private List<PFace<P>> faces;
	private List<PFace<P>> holes;
	private PFace<P> boundary;
	private List<PHalfEdge<P>> edges;
	private IPointConstructor<P> pointConstructor;
	private List<PVertex<P>> vertices;

	public PMesh(final IPointConstructor<P> pointConstructor) {
		this.faces = new ArrayList<>();
		this.holes = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();
		this.boundary = new PFace<>(true);
		this.pointConstructor = pointConstructor;
	}

	@Override
	public PMesh<P> construct() {
		return new PMesh<>(pointConstructor);
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
	public IPointConstructor<P> getPointConstructor() {
		return pointConstructor;
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
		return faces.stream().filter(face -> !isDestroyed(face)).filter(f -> !isBoundary(f)).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull PFace<P> face) {
		return face.isBorder();
	}

	@Override
	public boolean isHole(@NotNull PFace<P> face) {
		return isBoundary(face) && face != boundary;
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
	public boolean isDestroyed(@NotNull PVertex<P> vertex) {
		return vertex.isDestroyed();
	}

	@Override
	public void setTwin(final PHalfEdge<P> halfEdge, final PHalfEdge<P> twin) {
		halfEdge.setTwin(twin);
		twin.setTwin(halfEdge);
	}

	@Override
	public void setNext(final PHalfEdge<P> halfEdge, final PHalfEdge<P> next) {
		halfEdge.setNext(next);
		next.setPrevious(halfEdge);
	}

	@Override
	public void setPrev(final PHalfEdge<P> halfEdge, final PHalfEdge<P> prev) {
		halfEdge.setPrevious(prev);
		prev.setNext(halfEdge);
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
		assert edge.getEnd().equals(vertex);
		if(!edge.getEnd().equals(vertex)) {
			throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex + " != " + edge.getEnd());
		}
		vertex.setEdge(edge);
	}

	@Override
	public void setVertex(@NotNull PHalfEdge<P> halfEdge, @NotNull PVertex<P> vertex) {
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
	public Collection<PVertex<P>> getVertices() {
		return vertices;
	}

	@Override
	public int getNumberOfVertices() {
		return vertices.size();
	}

	@Override
	public int getNumberOfFaces() {
		return faces.size()-holes.size();
	}

	@Override
	public boolean tryLock(@NotNull PVertex<P> vertex) {
		return vertex.getLock().tryLock();
	}

	@Override
	public void unlock(@NotNull PVertex<P> vertex) {
		vertex.getLock().unlock();
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
	public PFace<P> createFace(boolean hole) {
		PFace<P> face = new PFace<>(hole);
		faces.add(face);

		if(hole) {
			holes.add(face);
		}
		return face;
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
	public PFace<P> getBorder() {
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
	public void toHole(@NotNull PFace<P> face) {
		assert !isHole(face);
		if(!isHole(face)) {
			holes.add(face);
			face.setBoundary(true);
		}
	}

	@Override
	public void destroyFace(@NotNull PFace<P> face) {
		faces.remove(face);
		if(isHole(face)) {
			holes.remove(face);
		}
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
		//vertices.remove(vertex);
		vertex.destroy();
	}

	@Override
	public Stream<PHalfEdge<P>> streamEdges() {
		return edges.stream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<PHalfEdge<P>> streamEdgesParallel() {
		return edges.parallelStream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<PVertex<P>> streamVertices() { return vertices.stream().filter(v -> !isDestroyed(v)); }

	@Override
	public Stream<PVertex<P>> streamVerticesParallel() {
		return vertices.parallelStream().filter(v -> !isDestroyed(v));
	}

	@Override
	public Iterable<PHalfEdge<P>> getEdgeIt() {
		return () -> edges.iterator();
	}

	@Override
	public List<PFace<P>> getFaces() {
		return streamFaces().filter(face -> !face.isBorder()).filter(face -> !face.isDestroyed()).collect(Collectors.toList());
	}

	@Override
	public List<PFace<P>> getFacesWithHoles() {
		return streamFaces().filter(face -> !face.isDestroyed()).collect(Collectors.toList());
	}

	@Override
	public List<PHalfEdge<P>> getBoundaryEdges() {
		return streamEdges().filter(edge -> edge.isBoundary()).filter(edge -> !edge.isDestroyed()).collect(Collectors.toList());
	}
	@Override
	public List<PVertex<P>> getBoundaryVertices() {
		return streamEdges().filter(edge -> edge.isBoundary()).filter(edge -> isDestroyed(edge)).map(edge -> getVertex(edge)).collect(Collectors.toList());
	}

	@Override
	public Stream<PFace<P>> streamFaces(@NotNull Predicate<PFace<P>> predicate) {
		return faces.stream().filter(predicate);
	}

	@Override
	public Stream<PFace<P>> streamHoles() {
		return holes.stream();
	}

	@Override
    public int getNumberOfEdges() {
        return edges.size();
    }

	@Override
	public ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> toTriangulation(final @NotNull IPointLocator.Type type) {
		return ITriangulation.createPTriangulation(type, this);
	}

	@Override
	public void arrangeMemory(@NotNull Iterable<PFace<P>> faceOrder) {
		try {
			throw new UnsupportedOperationException("not jet implemented.");
		} catch (UnsupportedOperationException e) {
			log.warn(e.getMessage());
		}
	}

	@Override
	public synchronized IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> clone() {
		try {
			PMesh<P> clone = (PMesh<P>)super.clone();
			clone.pointConstructor = pointConstructor;
			clone.faces = new ArrayList<>();
			clone.holes = new ArrayList<>();
			clone.edges = new ArrayList<>();
			clone.vertices = new ArrayList<>();

			Map<PVertex<P>, PVertex<P>> vertexMap = new HashMap<>();
			Map<PHalfEdge<P>, PHalfEdge<P>> edgeMap = new HashMap<>();
			Map<PFace<P>, PFace<P>> faceMap = new HashMap<>();

			// faces are not complete: missing edge
			for(PVertex<P> v : vertices) {
				PVertex<P> cV = v.clone();
				clone.vertices.add(cV);
				vertexMap.put(v, cV);
			}

			// edges are not complete: missing next, prev, twin, face
			for(PHalfEdge<P> e : edges) {
				PHalfEdge<P> cE = e.clone();
				edgeMap.put(e, cE);
				cE.setEnd(vertexMap.get(e.getEnd()));
				clone.edges.add(cE);
			}

			// faces are complete
			clone.boundary = boundary.clone();
			faceMap.put(boundary, clone.boundary);
			clone.boundary.setEdge(edgeMap.get(boundary.getEdge()));
			for(PFace<P> f : faces) {
				PFace<P> cF = f.clone();
				faceMap.put(f, cF);
				cF.setEdge(edgeMap.get(f.getEdge()));
				clone.faces.add(cF);
			}

			for(PVertex<P> cV : clone.vertices) {
				cV.setEdge(edgeMap.get(cV.getEdge()));
				cV.setDown(null);
			}

			for(PHalfEdge<P> cE : clone.edges) {
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
