package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GenVoronoiSegmentInsertion<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IRefiner<V, E, F> {

	private static Logger logger = Logger.getLogger(GenVoronoiVertexInsertion.class);
	private final GenConstrainedDelaunayTriangulator<V, E, F> cdt;
	private IIncrementalTriangulation<V, E, F> triangulation;

	// Improvements: maybe mark edges which should not be flipped instead of using a Set is slower.
	private final PSLG pslg;
	private final VPolygon segmentBound;

	private boolean initialized;
	private boolean generated;

	// Improvements: use multiple unsorted queues to improve performance
	private PriorityQueue<F> active;

	// TODO: use the mesh property container
	private Set<F> activeSet;
	private Set<F> accepted;
	private Map<F, VTriangle> triangles;
	private boolean createHoles;
	private Function<IPoint, Double> edgeLenFunction;
	//TODO make it a parameter
	private final static int MAX_POINTS = 200_000;
	private double delta = 1.5;

	private VoronoiSegPlacement<V, E, F> placementStrategy;

	public GenVoronoiSegmentInsertion(@NotNull final PSLG pslg,
	                                  @NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier,
	                                  final boolean createHoles,
	                                  @NotNull Function<IPoint, Double> edgeLenFunction) {
		this.initialized = false;
		this.generated = false;
		this.active = new PriorityQueue<>(new GenVoronoiSegmentInsertion.FaceQualityComparator());
		this.accepted = new HashSet<>();
		this.activeSet = new HashSet<>();
		this.triangles = new HashMap<>();
		this.createHoles = createHoles;
		this.edgeLenFunction = edgeLenFunction;
		this.pslg = pslg.addLines(generateLines(pslg));
		this.segmentBound = pslg.getSegmentBound();

		/**
		 * This prevent the flipping of constrained edges
		 */
		this.cdt = new GenConstrainedDelaunayTriangulator<>(meshSupplier, pslg, false);
		this.placementStrategy = new VoronoiSegPlacement<>(cdt.getMeshBuilder().getMesh(), edgeLenFunction);
	}

	public GenVoronoiSegmentInsertion(@NotNull final IIncrementalTriangulation<V, E, F> triangulation,
	                                  @NotNull final Function<IPoint, Double> circumRadiusFunc) {
		this.triangulation = triangulation;
		this.initialized = false;
		this.generated = false;
		this.active = new PriorityQueue<>(new GenVoronoiSegmentInsertion.FaceQualityComparator());
		this.accepted = new HashSet<>();
		this.activeSet = new HashSet<>();
		this.triangles = new HashMap<>();
		this.createHoles = false;
		this.edgeLenFunction = circumRadiusFunc;
		this.pslg = null;
		this.cdt = null;
		this.segmentBound = GeometryUtils.polygonFromPoints2D(
				getTriangulation().getMeshBuilder().getMesh().vertices().getAllOf(triangulation.getMesh().faces().getOuterBorder()));
		this.placementStrategy = new VoronoiSegPlacement<>(triangulation.getMesh(), circumRadiusFunc);
	}

	private boolean isAccepted(@NotNull final E edge) {
		return edgeLenFunction.apply(getMesh().edges().toLine(edge).midPoint()) / getMesh().faces().toTriangle(getMesh().faces().getOf(edge)).getCircumscribedRadius() < 1.5;
	}

	private void scan() {
		for(F face : getMesh().faces().getAll()) {
			if(!isAccepted(face) && isActive(face)) {
				triangles.put(face, getMesh().faces().toTriangle(face));
				active.add(face);
				activeSet.add(face);
			}
		}
	}

	private void splitBoundaryEdges() {
		boolean split;
		do {
			split = false;
			List<E> boundaryEdges = getMesh().edges().getBoundaryEdges();
			for(E edge : boundaryEdges) {
				VLine line = getMesh().edges().toLine(edge);
				if(line.length() > edgeLenFunction.apply(line.midPoint())) {
					getMeshBuilder().changeConnectivity().splitEdge(edge, true);
					split = true;
				}
			}
		} while (split);
	}

	@Override
	public void refine() {
		if(!refinementFinished()) {
			if(!initialized) {
				if(triangulation == null) {
					cdt.generate(true);
					triangulation = cdt.getTriangulation();
				}
				splitBoundaryEdges();
				scan();
				initialized = true;
			}

			if(!active.isEmpty()) {
				F face = active.poll();
				activeSet.remove(face);
				refine(face);
			}
		} else if(!isFinished()) {
			finish(true);
		}
		else {
			logger.info("finished");
		}
	}

	private void refine(@NotNull final F face) {
		E shortestEdge = null;
		VLine shortestLine = null;
		var edges = getMesh().edges();
		var faces = getMesh().faces();

		for(E edge : edges.iterableFor(face)) {
			VLine tmpLine = edges.toLine(edge);
			if(isAccepted(faces.getTwin(edge)) && (shortestEdge == null || shortestLine.length() > tmpLine.length())) {
				shortestEdge = edge;
				shortestLine = tmpLine;
			}
		}

		VPoint x = placementStrategy.computePlacement(shortestEdge, triangles.get(face));
		Optional<F> optionalF = getMesh().readConnectivity().locateMarch(x.getX(), x.getY(), faces.getOf(shortestEdge));

		if(optionalF.isPresent() && !faces.isBoundary(optionalF.get())) {
			V v = getMeshBuilder().vertices().create(x.getX(), x.getY());
			getTriangulation().insertVertex(v, optionalF.get());

			// no point was inserted
			if(edges.getOf(v) == null) {
				accepted.add(face);

				for(F f : faces.surroundingIterableFor(face)) {
					if(activeSet.remove(f)) {
						active.remove(f);
					}

					if(!isAccepted(f) && isActive(f)) {
						triangles.put(f, getMeshBuilder().getMesh().faces().toTriangle(f));
						active.add(f);
						activeSet.add(f);
					}
				}

			} else {
				// update triangles
				for(E ev : edges.iterableFor(v)) {

					E eRing = edges.getPrev(ev);
					F f1 = faces.getOf(eRing);
					F f2 = faces.getTwin(eRing);

					if(activeSet.remove(f1)) {
						active.remove(f1);
					}

					if(!isAccepted(f1) && isActive(f1)) {
						triangles.put(f1, faces.toTriangle(f1));
						active.add(f1);
						activeSet.add(f1);
					}

					if(activeSet.remove(f2)) {
						active.remove(f2);
					}

					if(!isAccepted(f2) && isActive(f2)) {
						triangles.put(f2, faces.toTriangle(f2));
						active.add(f2);
						activeSet.add(f2);
					}
				}
			}
		}
	}

	@Override
	public boolean isFinished() {
		return generated;
	}

	private boolean refinementFinished() {
		return initialized == true && (active.isEmpty() || getMesh().vertices().count() >= MAX_POINTS);
	}

	@Override
	public IIncrementalTriangulation generate(final boolean finalize) {
		if(!isFinished()) {
			while (!refinementFinished()) {
				refine();
			}
			finish(false);
		}
		return getTriangulation();
	}

	private void finish(boolean finalize) {
		generated = true;
		if(finalize) {
			getTriangulation().finish();
			removeTriangles();
		}
	}

	public void removeTriangles() {
		if(createHoles) {
			for(VPolygon hole : pslg.getHoles()) {
				Predicate<F> mergeCondition = f -> hole.contains(getMesh().faces().toTriangle(f).midPoint());
				Optional<F> optFace = getMesh().faces().stream().filter(mergeCondition).findAny();
				if(optFace.isPresent()) {
					Optional<F> optionalF = getMeshBuilder().changeConnectivity().createHole(optFace.get(), mergeCondition, true);
				}
			}

			Predicate<F> mergeCondition = f -> !pslg.getSegmentBound().contains(getMesh().faces().toTriangle(f).midPoint());
			getMeshBuilder().changeConnectivity().shrinkBorder(mergeCondition, true);
		}
	}

	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return getTriangulation().getMeshBuilder();
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return getTriangulation().getMeshDataStorage();
	}

	private boolean isAccepted(@NotNull final F face) {
		if(getMesh().faces().isBoundary(face) || accepted.contains(face)) {
			return true;
		}
		else {
			double r = getMesh().faces().toTriangle(face).getCircumscribedRadius();
			boolean accepted =  r * Math.sqrt(3) <= edgeLenFunction.apply(getMesh().faces().toTriangle(face).midPoint())  /*&& getTriangulation().faceToQuality(face) >= minQuality*/;
			if(accepted) {
				this.accepted.add(face);
			}
			return accepted;
		}
	}

	private boolean isActive(@NotNull final F face) {
		if(getMesh().faces().isBoundary(face)) {
			return false;
		}

		// This might be expensive!
		if(!segmentBound.contains(getMesh().faces().toTriangle(face).midPoint())) {
			return false;
		}

		for(F neighbour : getMesh().faces().surroundingIterableFor(face)) {
			if(isAccepted(neighbour)) {
				return true;
			}
		}

		return false;
	}

	// TODO duplicated code
	private List<VLine> generateLines(@NotNull final PSLG pslg) {
		List<VLine> polyLines = new ArrayList<>();

		polyLines.addAll(pslg.getSegmentBound().getLinePath());

		for(VPolygon polygon : pslg.getHoles()) {
			polyLines.addAll(polygon.getLinePath());
		}

		List<VLine> lines = new ArrayList<>();
		for(VLine line : polyLines) {
			List<VLine> splitLines = new ArrayList<>();
			splitLines.add(line);
			while (!splitLines.isEmpty()) {
				List<VLine> newSplitLines = new ArrayList<>();
				for(VLine splitLine : splitLines) {
					VPoint midPoint = splitLine.midPoint();
					double desiredLen = edgeLenFunction.apply(midPoint) * Math.sqrt(3);
					double len = splitLine.length();
					if(len >  desiredLen) {
						newSplitLines.add(new VLine(splitLine.getVPoint1(), midPoint));
						newSplitLines.add(new VLine(midPoint, splitLine.getVPoint2()));
					} else {
						lines.add(splitLine);
					}
				}
				splitLines = newSplitLines;
			}
		}

		return lines;
	}

	private final class FaceQualityComparator implements Comparator<F> {

		@Override
		public int compare(F o1, F o2) {
			return Double.compare(-triangles.get(o1).getCircumscribedRadius(), -triangles.get(o2).getCircumscribedRadius());
		}
	}
}
