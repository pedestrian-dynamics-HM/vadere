package org.vadere.util.voronoi;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

/**
 * The Voronoi Diagram is constructed according to the algorithm described in
 * the book Computational Geometry by Mark de Berg et al. (2008) pages 151ff.
 * The algorithm is commonly known as "Fortune's algorithm" and uses the
 * paradigm of a sweep line. Notice: Understanding how the Voronoi Diagram is
 * constructed with this code without understanding the algorithm in advance
 * seems hardly possible to me.
 */
public class VoronoiDiagram {

	private final RectangleLimits limits;

	private PriorityQueue<Event> eventQueue;
	private BeachLine beachLine;
	private List<Face> faces;

	/*
	 * Handle degenerating case where the first rising sites lay on a horizonal
	 * line.
	 */
	private boolean siteOnHorizontalLine;
	private double lastY;

	public VoronoiDiagram(RectangleLimits limits) {
		this.limits = limits;
	}

	/*
	 * voronoiDiagramArea has to contain left lower and right upper coordinates
	 * of area a jts diagram shall be created for.
	 */
	public VoronoiDiagram(final List<VPoint> bounds) {
		this.limits = new RectangleLimits(bounds.get(0).x, bounds.get(0).y,
				bounds.get(1).x, bounds.get(1).y);
	}

	public VoronoiDiagram(final VRectangle rectangle) {
		this.limits = new RectangleLimits(rectangle.x, rectangle.y,
				rectangle.x + rectangle.width, rectangle.y + rectangle.height);
	}

	public void computeVoronoiDiagram(Iterable<? extends VPoint> positions) {
		if (positions == null) {
			return;
		}

		this.eventQueue = new PriorityQueue<Event>();
		this.beachLine = new BeachLine(limits);
		this.faces = new LinkedList<Face>();

		for (VPoint position : positions) {
			/*
			 * ped.prenatal(): only consider pedestrians, that have already
			 * moved away from the source to avoid conflicts. sst: prenatal
			 * removed; It may be better to not to compute the density at areas
			 * with equal positions of pedestrians as this isn't a real
			 * situation anyway. Avoid overlapping of pedestrians or do not
			 * measure density at sources at least (where this case may occur).
			 */
			if ( /* !ped.prenatal() && */limits.isInside(position)) {
				eventQueue.add(new EventSite(position));
			}
		}

		if (!eventQueue.isEmpty()) {
			lastY = eventQueue.iterator().next().getYCoordinate();
			siteOnHorizontalLine = true;
		}

		while (!eventQueue.isEmpty()) {
			Event event = eventQueue.remove();

			if (event.getClass() == EventSite.class) {
				handleSiteEvent((EventSite) event);
			} else if (event.getClass() == EventCircle.class) {
				handleCircleEvent((EventCircle) event);
			} else {
				throw new IllegalStateException(
						"Only Site and Circle Events are reasonable.");
			}
		}

		for (Face f : faces) {
			f.handleOpenFace();
		}
	}

	private void handleSiteEvent(EventSite event) {

		int siteId = event.getSiteId();
		VPoint site = event.getSite();

		// step 1
		if (beachLine.isEmpty()) {
			beachLine.setRoot(siteId, site, faces);
		} else {
			// handle degenerating case
			if (siteOnHorizontalLine && (site.y != lastY)) {
				siteOnHorizontalLine = false;
			}

			// step 2
			BeachLineLeaf arc = beachLine.getArcAboveSite(site);
			removeCircleEvent(arc);

			// step 3
			BeachLineLeaf newLeaf = beachLine.addSite(siteId, site, arc, faces,
					siteOnHorizontalLine);

			// TODO [priority=low] [task=optimization] Rebalancing the trees, to improve computation speed.

			// step 4
			HalfEdge.handleSiteEventEdges(faces, newLeaf, arc,
					siteOnHorizontalLine);

			// step 5
			checkForCircleEvent(newLeaf.getPredecessor());
			checkForCircleEvent(newLeaf.getSuccessor());
		}
	}

	private void handleCircleEvent(EventCircle event) {

		BeachLineLeaf gamma = event.getBeachLineLeaf();

		// step 2
		VPoint vertex = new VPoint(event.getXCoordinate(),
				event.getYCoordinate());

		BeachLineInternal rightNode = beachLine.getRightBreakPoint(gamma);
		BeachLineInternal leftNode = beachLine.getLeftBreakPoint(gamma);

		HalfEdge newHalfEdge = HalfEdge.handleCircleEventEdges(leftNode,
				rightNode, vertex);

		// step 1
		BeachLineInternal newBreakPoint = beachLine.deleteLeaf(gamma);
		newBreakPoint.setHalfEdge(newHalfEdge);

		// TODO [priority=low] [task=optimization] rebalancing

		BeachLineLeaf predecessorLeaf = gamma.getPredecessor();
		removeCircleEvent(predecessorLeaf);

		BeachLineLeaf successorLeaf = gamma.getSuccessor();
		removeCircleEvent(successorLeaf);

		// step 3
		checkForCircleEvent(predecessorLeaf);
		checkForCircleEvent(successorLeaf);
	}

	private void checkForCircleEvent(BeachLineLeaf leaf) {

		if (leaf != null && leaf.hasCircleEvent()) {
			leaf.createCircleEvent();
			eventQueue.add(leaf.getCircleEvent());
		}
	}

	private void removeCircleEvent(BeachLineLeaf leaf) {
		EventCircle circleEvent = leaf.removeCircleEvent();
		if (circleEvent != null) {
			eventQueue.remove(circleEvent);
		}
	}

	public List<Face> getFaces() {
		return faces;
	}

	public RectangleLimits getLimits() {
		return limits;
	}
}
