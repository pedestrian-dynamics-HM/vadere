package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IVertex;
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
	                                  @NotNull final IMeshSupplier<V, E, F> meshSupplier,
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
		this.placementStrategy = new VoronoiSegPlacement<>(cdt.getMesh(), edgeLenFunction);
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
				getTriangulation().getMesh().getVertices(triangulation.getMesh().getBorder()));
		this.placementStrategy = new VoronoiSegPlacement<>(triangulation.getMesh(), circumRadiusFunc);
	}

	private boolean isAccepted(@NotNull final E edge) {
		return edgeLenFunction.apply(getMesh().toLine(edge).midPoint()) / getMesh().toTriangle(getMesh().getFace(edge)).getCircumscribedRadius() < 1.5;
	}

	private void scan() {
		for(F face : getTriangulation().getMesh().getFaces()) {
			if(!isAccepted(face) && isActive(face)) {
				triangles.put(face, getMesh().toTriangle(face));
				active.add(face);
				activeSet.add(face);
			}
		}
	}

	private void splitBoundaryEdges() {
		boolean split;
		do {
			split = false;
			List<E> boundaryEdges = getMesh().getBoundaryEdges();
			for(E edge : boundaryEdges) {
				VLine line = getMesh().toLine(edge);
				if(line.length() > edgeLenFunction.apply(line.midPoint())) {
					getTriangulation().splitEdge(edge, true);
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
		for(E edge : getMesh().getEdgeIt(face)) {
			VLine tmpLine = getMesh().toLine(edge);
			if(isAccepted(getMesh().getTwinFace(edge)) && (shortestEdge == null || shortestLine.length() > tmpLine.length())) {
				shortestEdge = edge;
				shortestLine = tmpLine;
			}
		}

		VPoint x = placementStrategy.computePlacement(shortestEdge, triangles.get(face));
		Optional<F> optionalF = getTriangulation().locateMarch(x.getX(), x.getY(), getMesh().getFace(shortestEdge));

		if(optionalF.isPresent() && !getMesh().isBoundary(optionalF.get())) {
			V v = getMesh().createVertex(x.getX(), x.getY());
			getTriangulation().insertVertex(v, optionalF.get());

			// no point was inserted
			if(getMesh().getEdge(v) == null) {
				accepted.add(face);

				for(F f : getMesh().getFaceIt(face)) {
					if(activeSet.remove(f)) {
						active.remove(f);
					}

					if(!isAccepted(f) && isActive(f)) {
						triangles.put(f, getMesh().toTriangle(f));
						active.add(f);
						activeSet.add(f);
					}
				}

			} else {
				// update triangles
				for(E ev : getMesh().getEdgeIt(v)) {

					E eRing = getMesh().getPrev(ev);
					F f1 = getMesh().getFace(eRing);
					F f2 = getMesh().getTwinFace(eRing);

					if(activeSet.remove(f1)) {
						active.remove(f1);
					}

					if(!isAccepted(f1) && isActive(f1)) {
						triangles.put(f1, getMesh().toTriangle(f1));
						active.add(f1);
						activeSet.add(f1);
					}

					if(activeSet.remove(f2)) {
						active.remove(f2);
					}

					if(!isAccepted(f2) && isActive(f2)) {
						triangles.put(f2, getMesh().toTriangle(f2));
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
		return initialized == true && (active.isEmpty() || getMesh().getNumberOfVertices() >= MAX_POINTS);
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
				Predicate<F> mergeCondition = f -> hole.contains(getMesh().toTriangle(f).midPoint());
				Optional<F> optFace = getMesh().streamFaces().filter(mergeCondition).findAny();
				if(optFace.isPresent()) {
					Optional<F> optionalF = getTriangulation().createHole(optFace.get(), mergeCondition, true);
				}
			}

			Predicate<F> mergeCondition = f -> !pslg.getSegmentBound().contains(getMesh().toTriangle(f).midPoint());
			getTriangulation().shrinkBorder(mergeCondition, true);

		}
	}

	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return getTriangulation().getMesh();
	}

	private boolean isAccepted(@NotNull final F face) {
		if(getMesh().isBoundary(face) || accepted.contains(face)) {
			return true;
		}
		else {
			double r = getMesh().toTriangle(face).getCircumscribedRadius();
			boolean accepted =  r * Math.sqrt(3) <= edgeLenFunction.apply(getMesh().toTriangle(face).midPoint())  /*&& getTriangulation().faceToQuality(face) >= minQuality*/;
			if(accepted) {
				this.accepted.add(face);
			}
			return accepted;
		}
	}

	private boolean isActive(@NotNull final F face) {
		if(getMesh().isBoundary(face)) {
			return false;
		}

		// This might be expensive!
		if(!segmentBound.contains(getMesh().toTriangle(face).midPoint())) {
			return false;
		}

		for(F neighbour : getMesh().getFaceIt(face)) {
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
