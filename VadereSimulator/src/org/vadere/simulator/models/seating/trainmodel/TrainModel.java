package org.vadere.simulator.models.seating.trainmodel;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

	private int numberOfEntranceAreas;

	private List<Seat> seats;

	private List<Target> interimDestinations;
	private List<Source> leftDoors;
	private List<Source> rightDoors;

	private List<Compartment> compartments;
	private List<SeatGroup> seatGroups;

	private Topography topography;
	private TrainGeometry trainGeometry;

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
			throw new IllegalArgumentException("improper number of targets in seat rows.");
		}

		final int longRowLength = seatRows.get(0).size();
		final int numberOfEntranceAreas = longRowLength / 4;
		initialize(numberOfEntranceAreas);

		// now I have 4 long rows of seats
		for (int i = 0, seatGroupIndex = 0; i < longRowLength; i += 2) {

			// left seat group
			makeSeat(seatRows, seatGroupIndex, 0, 0, i);
			makeSeat(seatRows, seatGroupIndex, 1, 1, i);
			makeSeat(seatRows, seatGroupIndex, 2, 0, i + 1);
			makeSeat(seatRows, seatGroupIndex, 3, 1, i + 1);
			seatGroupIndex++;

			// right seat group
			makeSeat(seatRows, seatGroupIndex, 0, 2, i);
			makeSeat(seatRows, seatGroupIndex, 1, 3, i);
			makeSeat(seatRows, seatGroupIndex, 2, 2, i + 1);
			makeSeat(seatRows, seatGroupIndex, 3, 3, i + 1);
			seatGroupIndex++;
		}

		// find doors, interim destinations, and persons

		Rectangle2D longAisle = createFilterRect(leftmostCompartment.getMinY() + trainGeometry.getBenchWidth(),
				trainGeometry.getAisleWidth());

		// Sort interim target by x coordinate. Right-most target must be the first in the list.
		interimDestinations = findTargets(longAisle).stream()
				.sorted((a, b) -> Double.compare(b.getShape().getBounds().getX(), a.getShape().getBounds().getX()))
				.collect(Collectors.toList());
		if (interimDestinations.isEmpty()) {
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
			for (Seat s : seats) {
				if (p.getPosition().equals(s.getAssociatedTarget().getShape().getCentroid())) {
					p.getTargets().add(s.getAssociatedTarget().getId());
					s.setSittingPerson(p);
				}
			}
		}

	}

	private void initialize(int numberOfEntranceAreas) {
		this.numberOfEntranceAreas = numberOfEntranceAreas;
		final int numberOfCompartments = numberOfEntranceAreas + 1;
		final int numberOfSeatGroups = 4 * numberOfEntranceAreas;
		final int numberOfSeats = 4 * numberOfSeatGroups;

		seats = Arrays.asList(new Seat[numberOfSeats]);
		seatGroups = new ArrayList<>(numberOfSeatGroups);
		for (int i = 0; i < numberOfSeatGroups; i++) {
			seatGroups.add(new SeatGroup(seats, i));
		}
		
		compartments = new ArrayList<>(numberOfCompartments);
		for (int i = 0; i < numberOfCompartments; i++) {
			compartments.add(new Compartment(this, i));
		}
	}

	public int getEntranceAreaCount() {
		return numberOfEntranceAreas;
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
	
	public List<SeatGroup> getSeatGroups() {
		return Collections.unmodifiableList(seatGroups);
	}

	/**
	 * 
	 * @param seatGroupIndex The seat group's overall index. Not within-compartment index.
	 */
	public SeatGroup getSeatGroup(int seatGroupIndex) {
		return seatGroups.get(seatGroupIndex);
	}

	public List<Seat> getSeats() {
		return Collections.unmodifiableList(seats);
	}

	/** Return an unmodifiable list of interim destinations. */
	public List<Target> getInterimDestinations() {
		return Collections.unmodifiableList(interimDestinations);
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

	private void makeSeat(List<List<Target>> longRows, int seatGroupIndex, int indexInSeatGroup, int longRowIndex,
			int indexInLongRow) {
		final int number = calculateSeatNumberWithinCompartment(longRowIndex, indexInLongRow);
		final Seat newSeat = new Seat(longRows.get(longRowIndex).get(indexInLongRow), number);
		getSeatGroups().get(seatGroupIndex).setSeat(indexInSeatGroup, newSeat);
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
	int calculateSeatNumberWithinCompartment(int longRowIndex, int indexInLongRow) {
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
		final int index = interimDestinations.indexOf(target);
		if (index == -1) {
			throw new IllegalArgumentException("Given target is not an interim target.");
		}
		System.out.println("index of interim target: " + index + "; index of compartment: " + (index + 2) / 3);
		return getCompartment((index + 2) / 3);
	}

	void checkEntranceAreaIndexRange(int entranceAreaIndex) {
		if (entranceAreaIndex < 0 || entranceAreaIndex >= numberOfEntranceAreas) {
			throw new IllegalArgumentException(
					"Entrance area index must be in range 0 to less than " + numberOfEntranceAreas);
		}
	}

	public int getEntranceAreaIndexForPerson(Pedestrian p) {
		final Source source = p.getSource();
		if (source == null)
			throw new RuntimeException("Person's source is null.");

		final int index = entranceAreaIndexOfSource(source);
		if (index == -1)
			throw new RuntimeException("Person is not spawned at one of the doors.");
		return index;
	}

	int entranceAreaIndexOfSource(Source source) {
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

}
