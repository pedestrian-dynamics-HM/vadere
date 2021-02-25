package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class PMesh implements IMesh<PVertex, PHalfEdge, PFace> {

	private static Logger log = Logger.getLogger(PMesh.class);

	private int numberOfEdges;
	private int numberOfFaces;
	private int numberOfHoles;
	private int numberOfVertices;

	private List<PFace> faces;
	private List<PFace> holes;
	private PFace boundary;
	private List<PHalfEdge> edges;
	private List<PVertex> vertices;

	public PMesh() {
		clear();
	}

	@Override
	public void clear() {
		this.faces = new ArrayList<>();
		this.holes = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.vertices = new ArrayList<>();
		this.boundary = new PFace(true);
		this.numberOfEdges = 0;
		this.numberOfFaces = 0;
		this.numberOfHoles = 0;
		this.numberOfVertices = 0;
	}

	@Override
	public PMesh construct() {
		return new PMesh();
	}

	@Override
	public void garbageCollection() {
		faces = faces.stream().filter(f -> !isDestroyed(f)).collect(Collectors.toList());
		edges = edges.stream().filter(e -> !isDestroyed(e)).collect(Collectors.toList());
		vertices = vertices.stream().filter(v -> !isDestroyed(v)).collect(Collectors.toList());
	}

	@Override
	public PHalfEdge getNext(@NotNull final PHalfEdge halfEdge) {
		return halfEdge.getNext();
	}

	@Override
	public PHalfEdge getPrev(@NotNull final PHalfEdge halfEdge) {
		return halfEdge.getPrevious();
	}

	@Override
	public PHalfEdge getTwin(@NotNull final PHalfEdge halfEdge) {
		return halfEdge.getTwin();
	}

	@Override
	public PFace getFace(@NotNull final PHalfEdge halfEdge) {
		return halfEdge.getFace();
	}

	@Override
	public PHalfEdge getEdge(@NotNull final PVertex vertex) {
		return vertex.getEdge();
	}

	@Override
	public double getX(@NotNull final PVertex vertex) {
		return vertex.getX();
	}

	@Override
	public double getY(@NotNull final PVertex vertex) {
		return vertex.getY();
	}

	@Override
	public void setCoords(@NotNull final PVertex vertex, double x, double y) {
		vertex.setPoint(new VPoint(x, y));
	}

	@Override
	public PHalfEdge getEdge(@NotNull final PFace face) {
		return face.getEdge();
	}

	@Override
	public IPoint getPoint(@NotNull final PHalfEdge halfEdge) {
		return getVertex(halfEdge).getPoint();
	}

	@Override
	public PVertex getVertex(@NotNull final PHalfEdge halfEdge) {
		return halfEdge.getEnd();
	}

	@Override
	public IPoint getPoint(@NotNull final PVertex vertex) {
		return vertex.getPoint();
	}

	@Override
	public <CV> Optional<CV> getData(@NotNull final PVertex vertex, @NotNull final String name, @NotNull final Class<CV> clazz) {
		return Optional.ofNullable(vertex.getData(name, clazz));
	}

	@Override
	public <CV> void setData(@NotNull final PVertex vertex, @NotNull final String name, final CV data) {
		vertex.setData(name, data);
	}

	@Override
	public <CE> Optional<CE> getData(@NotNull final PHalfEdge edge, @NotNull final String name, @NotNull final Class<CE> clazz) {
		return Optional.ofNullable(edge.getData(name, clazz));
	}

	@Override
	public <CE> void setData(@NotNull final PHalfEdge edge, @NotNull final String name, @Nullable final CE data) {
		edge.setData(name, data);
	}

	@Override
	public <CF> Optional<CF> getData(@NotNull final PFace face, @NotNull final String name, @NotNull final Class<CF> clazz) {
		return Optional.ofNullable(face.getData(name, clazz));
	}

	@Override
	public <CF> void setData(@NotNull final PFace face, @NotNull final String name, @Nullable final CF data) {
		face.setData(name, data);
	}

	@Override
	public PFace getFace() {
		return faces.stream().filter(face -> !isDestroyed(face)).filter(f -> !isBoundary(f)).findAny().get();
	}

	@Override
	public boolean isBoundary(@NotNull final PFace face) {
		return face.isBoundary();
	}

	@Override
	public boolean isHole(@NotNull final PFace face) {
		return isBoundary(face) && face != boundary;
	}

	@Override
	public boolean isBoundary(@NotNull final PHalfEdge halfEdge) {
		return halfEdge.isBoundary();
	}

	@Override
	public boolean isDestroyed(@NotNull final PFace face) {
		return face.isDestroyed();
	}

	@Override
	public boolean isDestroyed(@NotNull final PHalfEdge edge) {
		return !edge.isValid();
	}

	@Override
	public boolean isDestroyed(@NotNull final PVertex vertex) {
		return vertex.isDestroyed();
	}

	@Override
	public void setTwin(@NotNull final PHalfEdge halfEdge, @NotNull final PHalfEdge twin) {
		halfEdge.setTwin(twin);
		twin.setTwin(halfEdge);
	}

	@Override
	public void setNext(@NotNull final PHalfEdge halfEdge, @NotNull final PHalfEdge next) {
		halfEdge.setNext(next);
		next.setPrevious(halfEdge);
	}

	@Override
	public void setPrev(@NotNull final PHalfEdge halfEdge, @NotNull final PHalfEdge prev) {
		halfEdge.setPrevious(prev);
		prev.setNext(halfEdge);
	}

	@Override
	public void setFace(@NotNull final PHalfEdge halfEdge, @NotNull final PFace face) {
		halfEdge.setFace(face);
	}

	@Override
	public void setEdge(@NotNull final PFace face, @NotNull final PHalfEdge edge) {
		face.setEdge(edge);
	}

	@Override
	public void setEdge(@NotNull final PVertex vertex, @NotNull final PHalfEdge edge) {
		assert edge.getEnd().equals(vertex);
		if(!edge.getEnd().equals(vertex)) {
			throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex + " != " + edge.getEnd());
		}
		vertex.setEdge(edge);
	}

	@Override
	public void setVertex(@NotNull final PHalfEdge halfEdge, @NotNull final PVertex vertex) {
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
	public boolean tryLock(@NotNull final PVertex vertex) {
		return vertex.getLock().tryLock();
	}

	@Override
	public void unlock(@NotNull final PVertex vertex) {
		vertex.getLock().unlock();
	}

	private void addEdge(@NotNull PHalfEdge edge) {
		assert !edges.contains(edge);
		edges.add(edge);
		numberOfEdges++;
	}

	@Override
	public PHalfEdge createEdge(@NotNull final PVertex vertex) {
		PHalfEdge edge = new PHalfEdge(vertex);
		addEdge(edge);
		return edge;
	}

	@Override
	public PHalfEdge createEdge(@NotNull final PVertex vertex, @NotNull final PFace face) {
		PHalfEdge edge = new PHalfEdge(vertex, face);
		addEdge(edge);
		return edge;
	}

	@Override
	public PFace createFace() {
		return createFace(false);
	}

	@Override
	public PFace createFace(final boolean hole) {
		PFace face = new PFace(hole);
		faces.add(face);
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
	public IPoint createPoint(double x, double y) {
		return new VPoint(x, y);
	}


	// TODO: maybe remove insertVertex!
	@Override
	public PVertex createVertex(double x, double y) {
		return createVertex(createPoint(x, y));
	}

	@Override
	public PVertex createVertex(@NotNull final IPoint point) {
		return new PVertex(point);
	}

	@Override
	public PFace getBorder() {
		return boundary;
	}

	@Override
	public void insert(@NotNull final PVertex vertex) {
		numberOfVertices++;
		vertices.add(vertex);
	}

	@Override
	public void insertVertex(@NotNull final PVertex vertex) {
		numberOfVertices++;
		vertices.add(vertex);
	}

	@Override
	public void toHole(@NotNull final PFace face) {
		assert !isHole(face);
		if(!isHole(face)) {
			holes.add(face);
			face.setBoundary(true);
			numberOfHoles++;
			numberOfFaces--;
		}
	}

	@Override
	public void destroyFace(@NotNull final PFace face) {
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
	public void destroyEdge(@NotNull final PHalfEdge edge) {
		//edges.remove(edge);
		edge.destroy();
		numberOfEdges--; // we destroy the edge and its twin!
	}

	@Override
	public void setDown(@NotNull final PVertex up, @NotNull final PVertex down) {
		up.setDown(down);
	}

	@Override
	public PVertex getDown(@NotNull final PVertex vertex) {
		return vertex.getDown();
	}

	@Override
	public void destroyVertex(@NotNull final PVertex vertex) {
		//vertices.remove(vertex);
		if(!isDestroyed(vertex)) {
			vertex.destroy();
			numberOfVertices--;
		}
	}

	@Override
	public void setPoint(@NotNull final PVertex vertex, @NotNull final IPoint point) {
		vertex.setPoint(point);
	}

	@Override
	public Stream<PHalfEdge> streamEdges() {
		return edges.stream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<PHalfEdge> streamEdgesParallel() {
		return edges.parallelStream().filter(e -> !isDestroyed(e));
	}

	@Override
	public Stream<PVertex> streamVertices() { return vertices.stream().filter(v -> !isDestroyed(v)); }

	@Override
	public Stream<PVertex> streamVerticesParallel() {
		return vertices.parallelStream().filter(v -> !isDestroyed(v));
	}

	@Override
	public Iterable<PHalfEdge> getEdgeIt() {
		return () -> edges.iterator();
	}

	@Override
	public PVertex getRandomVertex(@NotNull final Random random) {
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
	public Stream<PFace> streamFaces(@NotNull final Predicate<PFace> predicate) {
		return faces.stream().filter(f -> !isDestroyed(f)).filter(predicate);
	}

	@Override
	public Stream<PFace> streamHoles() {
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
	public IIncrementalTriangulation<PVertex, PHalfEdge, PFace> toTriangulation(@NotNull final IPointLocator.Type type) {
		return IIncrementalTriangulation.createPTriangulation(type, this);
	}

	@Override
	public void arrangeMemory(@NotNull final Iterable<PFace> faceOrder) {
		try {
			throw new UnsupportedOperationException("not jet implemented.");
		} catch (UnsupportedOperationException e) {
			log.warn(e.getMessage());
		}
	}

	@Override
	public synchronized PMesh clone() {
		try {
			PMesh clone = (PMesh)super.clone();
			clone.faces = new ArrayList<>();
			clone.holes = new ArrayList<>();
			clone.edges = new ArrayList<>();
			clone.vertices = new ArrayList<>();
			clone.numberOfVertices = numberOfVertices;
			clone.numberOfEdges = numberOfEdges;
			clone.numberOfHoles = numberOfHoles;
			clone.numberOfFaces = numberOfFaces;

			Map<PVertex, PVertex> vertexMap = new HashMap<>();
			Map<PHalfEdge, PHalfEdge> edgeMap = new HashMap<>();
			Map<PFace, PFace> faceMap = new HashMap<>();

			// faces are not complete: missing edge
			for(PVertex v : vertices) {
				PVertex cV = v.clone();
				clone.vertices.add(cV);
				vertexMap.put(v, cV);
			}

			// edges are not complete: missing next, prev, twin, face
			for(PHalfEdge e : edges) {
				PHalfEdge cE = e.clone();
				edgeMap.put(e, cE);
				cE.setEnd(vertexMap.get(e.getEnd()));
				clone.edges.add(cE);
			}

			// faces are complete
			clone.boundary = boundary.clone();
			faceMap.put(boundary, clone.boundary);
			clone.boundary.setEdge(edgeMap.get(boundary.getEdge()));
			for(PFace f : faces) {
				PFace cF = f.clone();
				faceMap.put(f, cF);
				cF.setEdge(edgeMap.get(f.getEdge()));
				clone.faces.add(cF);
				if(isHole(f)) {
					clone.holes.add(cF);
				}
			}

			for(PVertex cV : clone.vertices) {
				cV.setEdge(edgeMap.get(cV.getEdge()));
				cV.setDown(null);
			}

			for(PHalfEdge cE : clone.edges) {
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
