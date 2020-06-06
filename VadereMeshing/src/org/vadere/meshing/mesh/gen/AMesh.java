package org.vadere.meshing.mesh.gen;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.IEdgeContainerBoolean;
import org.vadere.meshing.mesh.inter.IEdgeContainerDouble;
import org.vadere.meshing.mesh.inter.IEdgeContainerObject;
import org.vadere.meshing.mesh.inter.IVertexContainerBoolean;
import org.vadere.meshing.mesh.inter.IVertexContainerDouble;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertexContainerObject;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.SpaceFillingCurve;
import org.vadere.util.geometry.shapes.IPoint;
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
 */
public class AMesh implements IMesh<AVertex, AHalfEdge, AFace>, Cloneable {
	private final static Logger log = Logger.getLogger(AMesh.class);
	private List<AFace> faces;
	private boolean elementRemoved;
	private int numberOfVertices;
	private int numberOfEdges;
	private int numberOfFaces;
	private int numberOfHoles;
	private List<AFace> holes;
	private AFace boundary;
	private List<AHalfEdge> edges;
	private List<AVertex> vertices;

	//TODO: test the new property structure!
	private Map<String, AObjectArrayList<?>> verticesData;
	private Map<String, AObjectArrayList<?>> halfEdgesData;
	private Map<String, AObjectArrayList<?>> facesData;
	private ArrayList<DoubleArrayList> verticesIndexedDoubleData;
	private Map<String, DoubleArrayList> verticesDoubleData;
	private Map<String, DoubleArrayList> facesDoubleData;
	private Map<String, DoubleArrayList> halfEdgesDoubleData;
	private Map<String, BooleanArrayList> verticesBooleanData;
	private Map<String, BooleanArrayList> facesBooleanData;
	private Map<String, BooleanArrayList> halfEdgesBooleanData;

	public AMesh() {
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

		this.verticesData = new HashMap<>();
		this.halfEdgesData = new HashMap<>();
		this.facesData = new HashMap<>();

		this.verticesIndexedDoubleData = new ArrayList<>();
		this.verticesDoubleData = new HashMap<>();
		this.halfEdgesDoubleData = new HashMap<>();
		this.facesDoubleData = new HashMap<>();

		this.verticesBooleanData = new HashMap<>();
		this.halfEdgesBooleanData = new HashMap<>();
		this.facesBooleanData = new HashMap<>();
	}

	@Override
	public IMesh<AVertex, AHalfEdge, AFace> construct() {
		return new AMesh();
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
	public boolean getBooleanData(@NotNull final AVertex vertex, @NotNull final String name) {
		if(!verticesBooleanData.containsKey(name)) {
			return false;
		} else {
			BooleanArrayList dataArray = verticesBooleanData.get(name);
			assert dataArray.size() == vertices.size();
			return dataArray.getBoolean(vertex.getId());
		}
	}

	@Override
	public double getDoubleData(@NotNull final AVertex vertex, @NotNull final String name) {
		if(!verticesDoubleData.containsKey(name)) {
			return 0.0;
		} else {
			DoubleArrayList dataArray = verticesDoubleData.get(name);
			assert dataArray.size() == vertices.size();
			return dataArray.getDouble(vertex.getId());
		}
	}

	@Override
	public double getDoubleData(@NotNull final AVertex vertex, @NotNull final int index) {
		if(verticesIndexedDoubleData.size() <= index) {
			return 0.0;
		} else {
			DoubleArrayList dataArray = verticesIndexedDoubleData.get(index);
			assert dataArray.size() == vertices.size();
			return dataArray.getDouble(vertex.getId());
		}
	}

	@Override
	public boolean getBooleanData(@NotNull final AHalfEdge edge, @NotNull final String name) {
		if(!halfEdgesBooleanData.containsKey(name)) {
			return false;
		} else {
			BooleanArrayList dataArray = halfEdgesBooleanData.get(name);
			assert dataArray.size() == edges.size();
			return dataArray.getBoolean(edge.getId());
		}
	}

	@Override
	public double getDoubleData(@NotNull final AHalfEdge edge, @NotNull final String name) {
		if(!halfEdgesDoubleData.containsKey(name)) {
			return 0.0;
		} else {
			DoubleArrayList dataArray = halfEdgesDoubleData.get(name);
			assert dataArray.size() == edges.size();
			return dataArray.getDouble(edge.getId());
		}
	}

	@Override
	public boolean getBooleanData(@NotNull final AFace face, @NotNull final String name) {
		if(!facesBooleanData.containsKey(name)) {
			return false;
		} else {
			BooleanArrayList dataArray = facesBooleanData.get(name);
			assert dataArray.size() == faces.size();
			return dataArray.getBoolean(face.getId());
		}
	}

	@Override
	public double getDoubleData(@NotNull final AFace face, @NotNull final String name) {
		if(!facesDoubleData.containsKey(name)) {
			return 0.0;
		} else {
			DoubleArrayList dataArray = facesDoubleData.get(name);
			assert dataArray.size() == faces.size();
			return dataArray.getDouble(face.getId());
		}
	}

	@Override
	public <CV> Optional<CV> getData(@NotNull final AVertex vertex, @NotNull final String name, @NotNull Class<CV> clazz) {
		if(!verticesData.containsKey(name)) {
			return Optional.ofNullable(null);
		} else {
			ObjectArrayList<CV> dataArray = (ObjectArrayList<CV>) verticesData.get(name);
			assert dataArray.size() == vertices.size();
			return Optional.ofNullable(dataArray.get(vertex.getId()));
		}
	}

	@Override
	public <CV> void setData(@NotNull final AVertex vertex, @NotNull final String name, @Nullable final CV data) {
		if(!verticesData.containsKey(name)) {
			AObjectArrayList<CV> dataArray = new AObjectArrayList<>();
			fill(dataArray, vertices.size());
			verticesData.put(name, dataArray);
		}
		AObjectArrayList<CV> dataArray = (AObjectArrayList<CV>) verticesData.get(name);
		assert dataArray.size() == vertices.size();
		dataArray.set(vertex.getId(), data);
	}

	@Override
	public <CE> Optional<CE> getData(@NotNull final AHalfEdge edge, @NotNull final String name, @NotNull Class<CE> clazz) {
		if(!halfEdgesData.containsKey(name)) {
			return Optional.ofNullable(null);
		} else {
			AObjectArrayList<CE> dataArray = (AObjectArrayList<CE>) halfEdgesData.get(name);
			assert dataArray.size() == edges.size();
			return Optional.ofNullable(dataArray.get(edge.getId()));
		}
	}

	@Override
	public <CE> void setData(@NotNull final AHalfEdge edge, @NotNull final String name, @Nullable final CE data) {
		if(!halfEdgesData.containsKey(name)) {
			AObjectArrayList<CE> dataArray = new AObjectArrayList<>();
			fill(dataArray, edges.size());
			halfEdgesData.put(name, dataArray);
		}
		AObjectArrayList<CE> dataArray = (AObjectArrayList<CE>) halfEdgesData.get(name);
		assert dataArray.size() == edges.size();
		dataArray.set(edge.getId(), data);
	}

	@Override
	public <CF> Optional<CF> getData(@NotNull final AFace face, @NotNull final String name, @NotNull Class<CF> clazz) {
		if(!facesData.containsKey(name)) {
			return Optional.ofNullable(null);
		} else {
			AObjectArrayList<CF> dataArray = (AObjectArrayList<CF>) facesData.get(name);
			assert dataArray.size() == vertices.size();
			return Optional.ofNullable(dataArray.get(face.getId()));
		}
	}

	@Override
	public <CF> void setData(@NotNull final AFace face, @NotNull final String name, @Nullable final CF data) {
		if(!facesData.containsKey(name)) {
			AObjectArrayList<CF> dataArray = new AObjectArrayList<>();
			fill(dataArray, faces.size());
			facesData.put(name, dataArray);
		}
		AObjectArrayList<CF> dataArray = (AObjectArrayList<CF>) facesData.get(name);
		assert dataArray.size() == faces.size();
		dataArray.set(face.getId(), data);
	}

	@Override
	public void setDoubleData(@NotNull final AFace face, @NotNull final String name, final double data) {
		if(!facesDoubleData.containsKey(name)) {
			DoubleArrayList dataArray = new DoubleArrayList(faces.size());
			dataArray.size(faces.size());
			facesDoubleData.put(name, dataArray);
		}
		DoubleArrayList dataArray = facesDoubleData.get(name);
		assert dataArray.size() == faces.size();
		dataArray.set(face.getId(), data);
	}

	@Override
	public void setDoubleData(@NotNull final AVertex vertex, @NotNull final String name, final double data) {
		if(!verticesDoubleData.containsKey(name)) {
			DoubleArrayList dataArray = new DoubleArrayList(vertices.size());
			dataArray.size(vertices.size());
			verticesDoubleData.put(name, dataArray);
		}
		DoubleArrayList dataArray = verticesDoubleData.get(name);
		assert dataArray.size() == vertices.size();
		dataArray.set(vertex.getId(), data);
	}

	@Override
	public void setDoubleData(@NotNull final AVertex vertex, @NotNull final int index, final double data) {
		if(verticesIndexedDoubleData.size() <= index) {
			for(int i = verticesIndexedDoubleData.size(); i <= index; i++) {
				DoubleArrayList dataArray = new DoubleArrayList(vertices.size());
				dataArray.size(vertices.size());
				verticesIndexedDoubleData.add(dataArray);
			}
		}
		DoubleArrayList dataArray = verticesIndexedDoubleData.get(index);
		assert dataArray.size() == vertices.size();
		dataArray.set(vertex.getId(), data);
	}

	private <CE> AObjectArrayList<CE> getObjectArrayEdge(@NotNull final String name, @NotNull final Class<CE> clazz) {
		if(!halfEdgesData.containsKey(name)) {
			AObjectArrayList<CE> dataArray = new AObjectArrayList<>();
			fill(dataArray, edges.size());
			halfEdgesData.put(name, dataArray);
		}
		return (AObjectArrayList<CE>)halfEdgesData.get(name);
	}

	private <CE> AObjectArrayList<CE> getObjectArrayVertex(@NotNull final String name, @NotNull final Class<CE> clazz) {
		if(!verticesData.containsKey(name)) {
			AObjectArrayList<CE> dataArray = new AObjectArrayList<>();
			fill(dataArray, vertices.size());
			verticesData.put(name, dataArray);
		}
		return (AObjectArrayList<CE>)verticesData.get(name);
	}

	private DoubleArrayList getDoubleArrayEdge(@NotNull final String name) {
		if(!halfEdgesDoubleData.containsKey(name)) {
			DoubleArrayList dataArray = new DoubleArrayList(edges.size());
			dataArray.size(edges.size());
			halfEdgesDoubleData.put(name, dataArray);
		}
		return halfEdgesDoubleData.get(name);
	}

	private DoubleArrayList getDoubleArrayVertex(@NotNull final String name) {
		if(!verticesDoubleData.containsKey(name)) {
			DoubleArrayList dataArray = new DoubleArrayList(vertices.size());
			dataArray.size(vertices.size());
			verticesDoubleData.put(name, dataArray);
		}
		return verticesDoubleData.get(name);
	}

	private DoubleArrayList getDoubleArrayFace(@NotNull final String name) {
		if(!facesDoubleData.containsKey(name)) {
			DoubleArrayList dataArray = new DoubleArrayList(faces.size());
			dataArray.size(faces.size());
			facesDoubleData.put(name, dataArray);
		}
		return facesDoubleData.get(name);
	}

	private BooleanArrayList getBooleanArrayEdge(@NotNull final String name) {
		if(!halfEdgesBooleanData.containsKey(name)) {
			BooleanArrayList dataArray = new BooleanArrayList(edges.size());
			dataArray.size(edges.size());
			halfEdgesBooleanData.put(name, dataArray);
		}
		return halfEdgesBooleanData.get(name);
	}

	private BooleanArrayList getBooleanArrayVertex(@NotNull final String name) {
		if(!verticesBooleanData.containsKey(name)) {
			BooleanArrayList dataArray = new BooleanArrayList(vertices.size());
			dataArray.size(vertices.size());
			verticesBooleanData.put(name, dataArray);
		}
		return verticesBooleanData.get(name);
	}

	private BooleanArrayList getBooleanArrayFace(@NotNull final String name) {
		if(!facesBooleanData.containsKey(name)) {
			BooleanArrayList dataArray = new BooleanArrayList(faces.size());
			dataArray.size(faces.size());
			facesBooleanData.put(name, dataArray);
		}
		return facesBooleanData.get(name);
	}

	@Override
	public void setDoubleData(@NotNull final AHalfEdge edge, @NotNull final String name, final double data) {
		DoubleArrayList dataArray = getDoubleArrayEdge(name);
		assert dataArray.size() == edges.size();
		dataArray.set(edge.getId(), data);
	}

	@Override
	public void setBooleanData(@NotNull final AFace face, @NotNull final String name, final boolean data) {
		if(!facesBooleanData.containsKey(name)) {
			BooleanArrayList dataArray = new BooleanArrayList(faces.size());
			dataArray.size(faces.size());
			facesBooleanData.put(name, dataArray);
		}
		BooleanArrayList dataArray = facesBooleanData.get(name);
		assert dataArray.size() == faces.size();
		dataArray.set(face.getId(), data);
	}

	@Override
	public void setBooleanData(@NotNull final AVertex vertex, @NotNull final String name, final boolean data) {
		if(!verticesBooleanData.containsKey(name)) {
			BooleanArrayList dataArray = new BooleanArrayList(vertices.size());
			dataArray.size(vertices.size());
			verticesBooleanData.put(name, dataArray);
		}
		BooleanArrayList dataArray = verticesBooleanData.get(name);
		assert dataArray.size() == vertices.size();
		dataArray.set(vertex.getId(), data);
	}

	@Override
	public void setBooleanData(@NotNull final AHalfEdge edge, @NotNull final String name, final boolean data) {
		if(!halfEdgesBooleanData.containsKey(name)) {
			BooleanArrayList dataArray = new BooleanArrayList(edges.size());
			dataArray.size(edges.size());
			halfEdgesBooleanData.put(name, dataArray);
		}
		BooleanArrayList dataArray = halfEdgesBooleanData.get(name);
		assert dataArray.size() == edges.size();
		dataArray.set(edge.getId(), data);
	}

	private void fill(@NotNull final ObjectArrayList<?> data, final int n) {
		for(int i = 0; i < n; i++) {
			data.add(null);
		}
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
		for (ObjectArrayList edgeProperty : halfEdgesData.values()) {
			edgeProperty.add(null);
		}
		for(DoubleArrayList edgeDoubleProperty : halfEdgesDoubleData.values()) {
			edgeDoubleProperty.add(0.0);
		}
		for(BooleanArrayList edgeBooleanProperty : halfEdgesBooleanData.values()) {
			edgeBooleanProperty.add(false);
		}
		numberOfEdges++;
		return edge;
	}

	@Override
	public AHalfEdge createEdge(@NotNull final AVertex vertex, @NotNull final AFace face) {
		int id = edges.size();
		AHalfEdge edge = new AHalfEdge(id, vertex.getId(), face.getId());
		edges.add(edge);
		for (ObjectArrayList edgeProperty : halfEdgesData.values()) {
			edgeProperty.add(null);
		}
		for(DoubleArrayList edgeDoubleProperty : halfEdgesDoubleData.values()) {
			edgeDoubleProperty.add(0.0);
		}
		for(BooleanArrayList edgeBooleanProperty : halfEdgesBooleanData.values()) {
			edgeBooleanProperty.add(false);
		}
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
		for (ObjectArrayList faceProperty : facesData.values()) {
			faceProperty.add(null);
		}

		for(DoubleArrayList faceDoubleProperty : facesDoubleData.values()) {
			faceDoubleProperty.add(0.0);
		}

		for(BooleanArrayList faceBooleanProperty : facesBooleanData.values()) {
			faceBooleanProperty.add(false);
		}

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
		for (ObjectArrayList vertexProperty : verticesData.values()) {
			vertexProperty.add(null);
		}
		for(DoubleArrayList vertexDoubleProperty : verticesDoubleData.values()) {
			vertexDoubleProperty.add(0.0);
		}
		for(BooleanArrayList vertexBooleanProperty : verticesBooleanData.values()) {
			vertexBooleanProperty.add(false);
		}
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

            // no deep copy of object properties
	        clone.facesData = facesData;
	        clone.verticesData = verticesData;
	        clone.halfEdgesData = halfEdgesData;

	        // deep copy of primitive properties
	        Map<String, DoubleArrayList> clonedFacesDoubleData = new HashMap<>();
	        for(var entry : facesDoubleData.entrySet()) {
		        clonedFacesDoubleData.put(entry.getKey(), entry.getValue().clone());
	        }
	        clone.facesDoubleData = clonedFacesDoubleData;

	        Map<String, DoubleArrayList> clonedHalfEdgesDoubleData = new HashMap<>();
	        for(var entry : halfEdgesDoubleData.entrySet()) {
		        clonedHalfEdgesDoubleData.put(entry.getKey(), entry.getValue().clone());
	        }
	        clone.halfEdgesDoubleData = clonedHalfEdgesDoubleData;

	        Map<String, DoubleArrayList> clonedVerticessDoubleData = new HashMap<>();
	        for(var entry : verticesDoubleData.entrySet()) {
		        clonedVerticessDoubleData.put(entry.getKey(), entry.getValue().clone());
	        }
	        clone.verticesDoubleData = clonedVerticessDoubleData;

	        ArrayList<DoubleArrayList> clonedVerticessIndexedDoubleData = new ArrayList<>();
	        for(var entry : verticesIndexedDoubleData) {
		        clonedVerticessIndexedDoubleData.add(entry.clone());
	        }
	        clone.verticesIndexedDoubleData = clonedVerticessIndexedDoubleData;

	        Map<String, BooleanArrayList> clonedFacesBooleanData = new HashMap<>();
	        for(var entry : facesBooleanData.entrySet()) {
		        clonedFacesBooleanData.put(entry.getKey(), entry.getValue().clone());
	        }
	        clone.facesBooleanData = clonedFacesBooleanData;

	        Map<String, BooleanArrayList> clonedHalfEdgesBooleanData = new HashMap<>();
	        for(var entry : halfEdgesBooleanData.entrySet()) {
		        clonedHalfEdgesBooleanData.put(entry.getKey(), entry.getValue().clone());
	        }
	        clone.halfEdgesBooleanData = clonedHalfEdgesBooleanData;

	        Map<String, BooleanArrayList> clonedVerticessBooleanData = new HashMap<>();
	        for(var entry : verticesBooleanData.entrySet()) {
		        clonedVerticessBooleanData.put(entry.getKey(), entry.getValue().clone());
	        }
	        clone.verticesBooleanData = clonedVerticessBooleanData;

            return clone;

        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

	@Override
	public IIncrementalTriangulation<AVertex, AHalfEdge, AFace> toTriangulation(final @NotNull IPointLocator.Type type) {
		return IIncrementalTriangulation.createATriangulation(type, this);
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
    public void arrangeMemory(@NotNull final Iterable<AFace> faceOrder) {
        // clone the old one!
        AMesh cMesh = clone();

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
        for(AFace face : faceOrder) {
            copyFace(face, vertexMap, edgeMap, faceMap, cMesh);
        }

	    // adjust all id's not contained in faceOrder in any order
	    for(AFace face : cMesh.faces) {
        	if(!isDestroyed(face)) {
		        copyFace(face, vertexMap, edgeMap, faceMap, cMesh);
	        }
	    }

        // repair the rest
        for(AFace face : faces) {
	        face.setEdge(edgeMap[face.getEdge()]);
        }

        for(AHalfEdge halfEdge : edges) {
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

        for(AVertex vertex : vertices) {
            vertex.setDown(vertexMap[vertex.getDown()]);
            vertex.setEdge(edgeMap[vertex.getEdge()]);
        }

        // fix the boundary
        boundary.setEdge(edgeMap[boundary.getEdge()]);

        // fix properties
	    rearrangeFacesData(faceMap, nullIdentifier);
	    rearrangeHalfEdgesData(edgeMap, nullIdentifier);
	    rearrangeVerticesData(vertexMap, nullIdentifier);
    }

	@Override
	public <CV> IVertexContainerObject<AVertex, AHalfEdge, AFace, CV> getObjectVertexContainer(@NotNull final String name, final Class<CV> clazz) {
		return new IVertexContainerObject<>() {
			private final ObjectArrayList<CV> list = getObjectArrayVertex(name, clazz);

			@Override
			public CV getValue(@NotNull final AVertex v) {
				return list.get(v.getId());
			}

			@Override
			public void setValue(@NotNull final AVertex v, CV value) {
				list.set(v.getId(), value);
			}
		};
	}

	@Override
	public <CV> IEdgeContainerObject<AVertex, AHalfEdge, AFace, CV> getObjectEdgeContainer(@NotNull final String name, final Class<CV> clazz) {
		return new IEdgeContainerObject<>() {
			private final ObjectArrayList<CV> list = getObjectArrayEdge(name, clazz);

			@Override
			public CV getValue(@NotNull final AHalfEdge edge) {
				return list.get(edge.getId());
			}

			@Override
			public void setValue(@NotNull final AHalfEdge edge, CV value) {
				list.set(edge.getId(), value);
			}
		};
	}

	@Override
	public IEdgeContainerBoolean<AVertex, AHalfEdge, AFace> getBooleanEdgeContainer(@NotNull final String name) {
		return new IEdgeContainerBoolean<>() {
			private final BooleanArrayList list = getBooleanArrayEdge(name);

			@Override
			public boolean getValue(@NotNull final AHalfEdge vertex) {
				return list.getBoolean(vertex.getId());
			}

			@Override
			public void setValue(@NotNull final AHalfEdge vertex, final boolean value) {
				list.set(vertex.getId(), value);
			}
		};
	}

	@Override
	public IEdgeContainerDouble<AVertex, AHalfEdge, AFace> getDoubleEdgeContainer(@NotNull final String name) {
    	return new IEdgeContainerDouble<>() {
			private final DoubleArrayList list = getDoubleArrayEdge(name);

			@Override
			public double getValue(@NotNull final AHalfEdge edge) {
				return list.getDouble(edge.getId());
			}

			@Override
			public void setValue(@NotNull final AHalfEdge edge, final double value) {
				list.set(edge.getId(), value);
			}
		};
	}

	@Override
	public IVertexContainerDouble<AVertex, AHalfEdge, AFace> getDoubleVertexContainer(@NotNull final String name) {
		return new IVertexContainerDouble<>() {
			private DoubleArrayList list = getDoubleArrayVertex(name);

			@Override
			public double getValue(@NotNull final AVertex vertex) {
				return list.getDouble(vertex.getId());
			}

			@Override
			public void setValue(@NotNull final AVertex vertex, final double value) {
				list.set(vertex.getId(), value);
			}

			@Override
			public void reset() {
				verticesDoubleData.remove(name);
				list = getDoubleArrayVertex(name);
			}
		};
	}

	@Override
	public IVertexContainerBoolean<AVertex, AHalfEdge, AFace> getBooleanVertexContainer(@NotNull String name) {
		return new IVertexContainerBoolean<>() {
			private final BooleanArrayList list = getBooleanArrayVertex(name);

			@Override
			public boolean getValue(@NotNull final AVertex vertex) {
				return list.getBoolean(vertex.getId());
			}

			@Override
			public void setValue(@NotNull final AVertex vertex, final boolean value) {
				list.set(vertex.getId(), value);
			}
		};
	}

	private void rearrangeVerticesData(@NotNull int[] vertexMap, int nullIdentifier) {
	    int numberOfDestroyed = 0;
    	for(int i = 0; i < vertexMap.length; i++) {
	    	if(vertexMap[i] != nullIdentifier) {
			    for(var list : verticesData.values()) {
				    list.swap(vertexMap[i], i);
			    }

			    for(var list : verticesDoubleData.values()) {
				    double tmp = list.getDouble(vertexMap[i]);
				    list.set(vertexMap[i], list.getDouble(i));
				    list.set(i, tmp);
			    }

			    for(var list : verticesIndexedDoubleData) {
				    double tmp = list.getDouble(vertexMap[i]);
				    list.set(vertexMap[i], list.getDouble(i));
				    list.set(i, tmp);
			    }

			    for(var list : verticesBooleanData.values()) {
				    boolean tmp = list.getBoolean(vertexMap[i]);
				    list.set(vertexMap[i], list.getBoolean(i));
				    list.set(i, tmp);
			    }
		    } else {
			    numberOfDestroyed++;
		    }
	    }

	    for(var list : verticesDoubleData.values()) {
	    	list.size(vertexMap.length - numberOfDestroyed);
	    	list.trim(vertexMap.length - numberOfDestroyed);
	    }

	    for(var list : verticesIndexedDoubleData) {
		    list.size(vertexMap.length - numberOfDestroyed);
		    list.trim(vertexMap.length - numberOfDestroyed);
	    }

	    for(var list : verticesBooleanData.values()) {
			list.size(vertexMap.length- numberOfDestroyed);
		    list.trim(vertexMap.length- numberOfDestroyed);
	    }

	    for(var list : verticesData.values()) {
	    	list.size(vertexMap.length - numberOfDestroyed);
	    	list.trim(vertexMap.length- numberOfDestroyed);
	    }
    }

	private void rearrangeHalfEdgesData(@NotNull int[] edgeMap, int nullIdentifier) {
		int numberOfDestroyed = 0;
    	for(int i = 0; i < edgeMap.length; i++) {
			if(edgeMap[i] != nullIdentifier) {
				for(var list : halfEdgesData.values()) {
					list.swap(edgeMap[i], i);
				}

				for(var list : halfEdgesDoubleData.values()) {
					double tmp = list.getDouble(edgeMap[i]);
					list.set(edgeMap[i], list.getDouble(i));
					list.set(i, tmp);
				}

				for(var list : halfEdgesBooleanData.values()) {
					boolean tmp = list.getBoolean(edgeMap[i]);
					list.set(edgeMap[i], list.getBoolean(i));
					list.set(i, tmp);
				}
			} else {
				numberOfDestroyed++;
			}
		}


		for(var list : halfEdgesDoubleData.values()) {
			list.trim(edgeMap.length - numberOfDestroyed);
		}

		for(var list : halfEdgesBooleanData.values()) {
			list.trim(edgeMap.length - numberOfDestroyed);
		}

		for(var list : halfEdgesData.values()) {
			list.trim(edgeMap.length - numberOfDestroyed);
		}
	}

    private void rearrangeFacesData(@NotNull int[] faceMap,  int nullIdentifier) {
    	int numberOfDestroyed = 0;
	    for(int i = 0; i < faceMap.length; i++) {
	    	if(faceMap[i] != nullIdentifier) {
			    for(var list : facesData.values()) {
				    list.swap(faceMap[i], i);
			    }

			    for(var list : facesDoubleData.values()) {
				    double tmp = list.getDouble(faceMap[i]);
				    list.set(faceMap[i], list.getDouble(i));
				    list.set(i, tmp);
			    }

			    for(var list : facesBooleanData.values()) {
				    boolean tmp = list.getBoolean(faceMap[i]);
				    list.set(faceMap[i], list.getBoolean(i));
				    list.set(i, tmp);
			    }
		    } else {
			    numberOfDestroyed++;
		    }
	    }

	    for(var list : facesDoubleData.values()) {
	    	list.trim(faceMap.length - numberOfDestroyed);
	    }

	    for(var list : facesBooleanData.values()) {
	    	list.trim(faceMap.length - numberOfDestroyed);
	    }

	    for(var list : facesData.values()) {
	    	list.trim(faceMap.length - numberOfDestroyed);
	    }
    }

    private void copyFace(@NotNull final AFace face, @NotNull int[] vertexMap, @NotNull int[] edgeMap, @NotNull int[] faceMap, @NotNull final AMesh cMesh) {
	    // merge some of them?
	    int nullIdentifier = -2;

	    // face not jet copied
	    if(faceMap[face.getId()] == nullIdentifier) {
		    AFace fClone = face.clone();

		    // 1. face
		    faceMap[face.getId()] = faces.size();
		    fClone.setId(faces.size());
		    faces.add(fClone);

		    if(cMesh.isHole(face)){
		    	holes.add(fClone);
			}

		    // 2. vertices
		    for(AVertex v : cMesh.getVertexIt(face)) {
			    if(vertexMap[v.getId()] == nullIdentifier) {
				    vertexMap[v.getId()] = vertices.size();
				    AVertex cVertex = v.clone();
				    cVertex.setId(vertices.size());
				    vertices.add(cVertex);
			    }
		    }

		    // 3. edges
		    for(AHalfEdge halfEdge : cMesh.getEdgeIt(face)) {

			    // origin
			    if(edgeMap[halfEdge.getId()] == nullIdentifier) {
				    edgeMap[halfEdge.getId()] = edges.size();
				    AHalfEdge cHalfEdge = halfEdge.clone();
				    cHalfEdge.setId(edges.size());
				    edges.add(cHalfEdge);
			    }

			    // twin
			    halfEdge = cMesh.getTwin(halfEdge);
			    if(edgeMap[halfEdge.getId()] == nullIdentifier) {
				    // origin
				    edgeMap[halfEdge.getId()] = edges.size();
				    AHalfEdge cHalfEdge = halfEdge.clone();
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
    private void spatialSort() {
        // get the bound for the space filling curve!
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        List<VPoint> centroids = new ArrayList<>(this.numberOfFaces);

	    for (AFace face : faces) {
		    VPoint incenter = GeometryUtils.getPolygonCentroid(getVertices(face));
		    centroids.add(incenter);
		    maxX = Math.max(maxX, incenter.getX());
		    maxY = Math.max(maxY, incenter.getY());

		    minX = Math.min(minX, incenter.getX());
		    minY = Math.min(minY, incenter.getY());
	    }

        SpaceFillingCurve spaceFillingCurve = new SpaceFillingCurve(new VRectangle(minX, minY, maxX-minX, maxY-minY));

        // TODO: implement faster sorting using radix sort see: http://www.diss.fu-berlin.de/diss/servlets/MCRFileNodeServlet/FUDISS_derivate_000000003494/2_kap2.pdf?hosts=
        // page 18
        List<AFace> sortedFaces = new ArrayList<>(faces.size());
        sortedFaces.addAll(this.faces);
        sortedFaces.sort((f1, f2) -> {
	        double i1 = spaceFillingCurve.compute(centroids.get(f1.getId()));
	        double i2 = spaceFillingCurve.compute(centroids.get(f2.getId()));
	        return Double.compare(i1, i2);
        });
        arrangeMemory(sortedFaces);
    }



	/**
	 * <p>Removes all destroyed object from this mesh and re-arranges all indices.</p>
     *
     * <p>Note: that any mapping id to vertex or id to halfEdge or id to face has to be recomputed!</p>
	 */
	public void garbageCollection() {
		int nullIdentifier = -2;
		int[] faceIdMap = new int[faces.size()];
		int[] edgeIdMap = new int[edges.size()];
		int[] vertexIdMap = new int[vertices.size()];

		Arrays.fill(faceIdMap, nullIdentifier);
		Arrays.fill(edgeIdMap, nullIdentifier);
		Arrays.fill(vertexIdMap, nullIdentifier);

		int i = 0;
		int j = 0;
		for (AFace face : faces) {
			if (face.isDestroyed()) {
				j--;
			} else {
				faceIdMap[i] = j;
			}
			i++;
			j++;
		}

		i = 0;
		j = 0;
		for (AHalfEdge edge : edges) {
			if (edge.isDestroyed()) {
				j--;
			} else {
				edgeIdMap[i] = j;
			}
			i++;
			j++;
		}

		i = 0;
		j = 0;
		for (AVertex vertex : vertices) {
			if (vertex.isDestroyed()) {
				j--;
			} else {
				vertexIdMap[i] = j;
			}
			i++;
			j++;
		}

		faces = faces.stream().filter(f -> !f.isDestroyed()).collect(Collectors.toList());
		edges = edges.stream().filter(e -> !e.isDestroyed()).collect(Collectors.toList());
		vertices = vertices.stream().filter(v -> !v.isDestroyed()).collect(Collectors.toList());

		i = 0;
		for (AFace face : faces) {
			face.setId(faceIdMap[face.getId()]);
			face.setEdge(edgeIdMap[face.getEdge()]);
			assert face.getId() == i;
			i++;
		}

		i = 0;
		for (AVertex vertex : vertices) {
			vertex.setId(vertexIdMap[vertex.getId()]);
			vertex.setEdge(edgeIdMap[vertex.getEdge()]);
			assert vertex.getId() == i;
			i++;
		}

		i = 0;
		for (AHalfEdge edge : edges) {
			edge.setId(edgeIdMap[edge.getId()]);
			edge.setEnd(vertexIdMap[edge.getEnd()]);
			edge.setNext(edgeIdMap[edge.getNext()]);
			edge.setPrevious(edgeIdMap[edge.getPrevious()]);
			edge.setTwin(edgeIdMap[edge.getTwin()]);
			if (edge.getFace() != boundary.getId()) {
				edge.setFace(faceIdMap[edge.getFace()]);
			}

			assert edge.getId() == i;
			i++;
		}

		// fix properties
		rearrangeFacesData(faceIdMap, nullIdentifier);
		rearrangeHalfEdgesData(edgeIdMap, nullIdentifier);
		rearrangeVerticesData(vertexIdMap, nullIdentifier);

		assert (getNumberOfVertices() == vertices.size()) && (getNumberOfEdges() == edges.size()) && (getNumberOfFaces() == faces.size()-holes.size());
	}

	/**
	 * <p>Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0)).</p>
	 *
	 * @return the created mesh
	 */
	public static AMesh createSimpleTriMesh() {
		AMesh mesh = new AMesh();
		IMesh.createSimpleTriMesh(mesh);
		return mesh;
	}
}