package org.vadere.simulator.models.seating.trainmodel;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TrainGeometry;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The front of the train is on the right side. Indexes increase from the train's front to rear.
 * Indexes of seat groups start at the front of the train on the left side (in driving direction).
 * Indexes of seats in a seat group start at the front side left. Both indexes increase from left to
 * right line-by-line.
 * 
 */
public class TrainModel {

	private static class ScenarioElementInRectPredicate implements Predicate<ScenarioElement> {
		private Rectangle2D box;

		public ScenarioElementInRectPredicate(Rectangle2D box) {
			this.box = box;
		}

		@Override
		public boolean test(ScenarioElement scenarioElement) {
			VPoint p = scenarioElement.getShape().getCentroid();
			return box.contains(p.x, p.y);
		}
	}

	private int entranceAreaCount;

	private List<Compartment> compartments;

	private List<Target> compartmentTargets;
	private List<Source> leftDoors;
	private List<Source> rightDoors;

	private Topography topography;
	private TrainGeometry trainGeometry;

	private Map<Target, Seat> targetSeatMap = new HashMap<>();

	private final Comparator<ScenarioElement> scenarioElementComperatorX = new Comparator<ScenarioElement>() {
		@Override
		public int compare(ScenarioElement o1, ScenarioElement o2) {
			double x1 = o1.getShape().getCentroid().x;
			double x2 = o2.getShape().getCentroid().x;
			return Double.compare(x1, x2);
		}
	};

	public TrainModel(Topography topography, TrainGeometry trainGeometry) {
		this.topography = topography;
		this.trainGeometry = trainGeometry;

		final Rectangle2D leftmostCompartment = trainGeometry.getCompartmentRect(0);
		final double rectHeight = trainGeometry.getBenchWidth() / 2;
		Rectangle2D[] rects = {
				createFilterRect(leftmostCompartment.getMinY(), rectHeight),
				createFilterRect(leftmostCompartment.getMinY() + rectHeight, rectHeight),
				createFilterRect(leftmostCompartment.getMaxY() - trainGeometry.getBenchWidth(), rectHeight),
				createFilterRect(leftmostCompartment.getMaxY() - rectHeight, rectHeight)
		};

		List<List<Target>> seatRows = new ArrayList<>(4);
		for (Rectangle2D r : rects) {
			seatRows.add(findTargets(r));
		}

		if (seatRows.stream().anyMatch(l -> l.size() % 4 != 0) // there must be 4 seat groups per entrance area
				// not all seat rows of same length?
				|| seatRows.stream().map(List::size).distinct().count() != 1) {
			throw new IllegalArgumentException("Improper number of targets in seat rows.");
		}

		final int longRowLength = seatRows.get(0).size();
		this.entranceAreaCount = longRowLength / 4;

		// ******************************
		// create compartments, seat groups, and seats
		// ******************************
		
		// find doors, compartment targets, and persons

		Rectangle2D longAisle = createFilterRect(leftmostCompartment.getMinY() + trainGeometry.getBenchWidth(),
				trainGeometry.getAisleWidth());

		// Sort compartment target by x coordinate. Right-most target must be the first in the list.
		compartmentTargets = findTargets(longAisle).stream()
				.sorted((a, b) -> Double.compare(a.getShape().getBounds().getX(), b.getShape().getBounds().getX()))
				.collect(Collectors.toList());
		if (compartmentTargets.isEmpty()) {
			throw new IllegalArgumentException(
					"This model depends on interim targets. Please create a train scenario with interim destinations.");
		}

		final double innerOffset = trainGeometry.getBenchWidth() / 8;
		Rectangle2D leftDoorsRect = createFilterRect(topography.getBounds().getMinY(),
				leftmostCompartment.getMinY() - topography.getBounds().getMinY() + innerOffset);
		Rectangle2D rightDoorsRect = createFilterRect(leftmostCompartment.getMaxY() - innerOffset,
				topography.getBounds().getMaxY() - leftmostCompartment.getMaxY() + innerOffset);
		leftDoors = findSources(leftDoorsRect);
		rightDoors = findSources(rightDoorsRect);

		Collection<Pedestrian> pedestrians = getPedestrians();
		// sit persons on seats
		for (Pedestrian p : pedestrians) {
			// TODO not implemented
//			for (Seat s : seats) {
//				if (p.getPosition().equals(s.getAssociatedTarget().getShape().getPolygonCentroid())) {
//					p.getTargets().add(s.getAssociatedTarget().getId());
//					s.setSittingPerson(p);
//				}
//			}
		}

		compartments = new ArrayList<>(getCompartmentCount());
		for (int i = 0; i < getCompartmentCount(); i++) {
			compartments.add(new Compartment(this, i, compartmentTargets, seatRows, targetSeatMap));
		}

	}

	public int getEntranceAreaCount() {
		return entranceAreaCount;
	}

	public Compartment getCompartment(int index) {
		return compartments.get(index);
	}

	public SeatGroup getSeatGroup(int compartmentIndex, int seatGroupIndex) {
		return getCompartment(compartmentIndex).getSeatGroup(seatGroupIndex);
	}
	
	public Seat getSeat(int compartmentIndex, int seatGroupIndex, int seatIndex) {
		return getSeatGroup(compartmentIndex, seatGroupIndex).getSeat(seatIndex);
	}
	
	/** Return an unmodifiable list of compartment targets. */
	public List<Target> getCompartmentTargets() {
		return Collections.unmodifiableList(compartmentTargets);
	}

	/**
	 * Return a new list of all pedestrians in the scenario. Pedestrians that
	 * are already defined in the topography are not counted until the
	 * simulation has started.
	 */
	public Collection<Pedestrian> getPedestrians() {
		return new ArrayList<>(topography.getElements(Pedestrian.class));
	}
	
	private List<Target> findTargets(Rectangle2D box) {
		return findScenarioElement(topography.getTargets(), box);
	}

	private List<Source> findSources(Rectangle2D box) {
		return findScenarioElement(topography.getSources(), box);
	}

	private <T extends ScenarioElement> List<T> findScenarioElement(List<T> scenarioElementList, Rectangle2D box) {
		return scenarioElementList.stream()
				.filter(new ScenarioElementInRectPredicate(box))
				.sorted(scenarioElementComperatorX)
				.collect(Collectors.toList());
	}

	/**
	 * Calculate the seat number within a compartment. The seat number
	 * corresponds to the numbering used in the data collection app and data
	 * processing.
	 * 
	 * @param longRowIndex
	 *            the index of a "long row" of seats which goes from the front
	 *            to the rear. Long rows are indexed from left to right.
	 * @param indexInLongRow
	 *            the index of a seat within a long row.
	 */
	static int calculateSeatNumberWithinCompartment(int longRowIndex, int indexInLongRow) {
		int offset;
		if (indexInLongRow < 2) // first half-compartment
			offset = indexInLongRow;
		else
			offset = (indexInLongRow - 2) % 4; // row of 4 abreast seats in compartment

		return offset * 4 + longRowIndex + 1;
	}

	private Rectangle2D createFilterRect(double y, double height) {
		return new Rectangle2D.Double(
				topography.getBounds().getMinX(), y,
				topography.getBounds().getWidth(), height);
	}

	/** Return the compartment containing p's latest target. */
	public Compartment getCompartment(Pedestrian p) {
		final List<Integer> targetIds = p.getTargets();
		if (targetIds.isEmpty()) {
			throw new IllegalStateException("Pedestrian has no targets.");
		}

		final int targetId = targetIds.get(targetIds.size() - 1);
		final Target target = topography.getTarget(targetId);
		return getCompartment(target);
	}

	Compartment getCompartment(Target target) {
		final int index = compartmentTargets.indexOf(target);
		if (index == -1) {
			throw new IllegalArgumentException("Given target is not an compartment target.");
		}
		return getCompartment(index);
	}

	public int getEntranceAreaIndexForPerson(Pedestrian p) {
		final Source source = p.getSource();
		if (source == null)
			throw new RuntimeException("Person's source is null.");

		final int index = getEntranceAreaIndexOfSource(source);
		if (index == -1)
			throw new RuntimeException("Person is not spawned at one of the doors.");
		return index;
	}

	public int getEntranceAreaIndexOfSource(Source source) {
		final double x = source.getShape().getCentroid().getX();
		for (int i = 0; i < getEntranceAreaCount(); i++) {
			if (isXIn(x, trainGeometry.getEntranceAreaRect(i))) {
				return i;
			}
		}
		return -1;
	}
	
	List<Source> getAllDoorSources() {
		final ArrayList<Source> result = new ArrayList<>(leftDoors);
		result.addAll(rightDoors);
		return result;
	}

	List<Source> getLeftDoorSources() {
		return leftDoors;
	}

	List<Source> getRightDoorSources() {
		return rightDoors;
	}

	private boolean isXIn(double x, Rectangle2D rect) {
		return x >= rect.getMinX() && x <= rect.getMaxX();
	}

	public Seat getSeatForTarget(Target target) {
		return targetSeatMap.get(target);
	}

	/** Number of compartments including half-compartments. */
	public int getCompartmentCount() {
		return entranceAreaCount + 1;
	}
	
	public Collection<Seat> getSeats() {
		return targetSeatMap.values();
	}

	public int getSeatGroupCount() {
		return (getCompartmentCount() - 1) * 4;
	}

}
