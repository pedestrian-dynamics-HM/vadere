package org.vadere.simulator.models.seating;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.vadere.state.scenario.Et423Geometry;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TrainGeometry;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Indexes of seat groups start at the front of the train on the left side (in travel direction).
 * Indexes of seats in a seat group start at the front side left. Both indexes increase from left to
 * right line-by-line.
 * 
 * Warning: Currently it only works with train scenarios that are generated using the Et423Geometry!
 * 
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

	private List<SeatGroup> seatGroups;

	private Topography topography;

	private final Comparator<ScenarioElement> scenarioElementComperatorX = new Comparator<ScenarioElement>() {
		@Override
		public int compare(ScenarioElement o1, ScenarioElement o2) {
			double x1 = o1.getShape().getCentroid().x;
			double x2 = o2.getShape().getCentroid().x;
			return Double.compare(x1, x2);
		}
	};

	public TrainModel(Topography topography) {
		this.topography = topography;

		// Assuming that the train scenario is generated using the S-Bahn geometry
		final TrainGeometry trainGeometry = new Et423Geometry(); // FIXME geometry should not be hardcoded; but how can the SeatingModel know the geometry?

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
			makeSeat(seatRows, seatGroupIndex, 1, 1, i + 1);
			makeSeat(seatRows, seatGroupIndex, 2, 0, i);
			makeSeat(seatRows, seatGroupIndex, 3, 1, i + 1);
			seatGroupIndex++;

			// right seat group
			makeSeat(seatRows, seatGroupIndex, 0, 2, i);
			makeSeat(seatRows, seatGroupIndex, 1, 3, i + 1);
			makeSeat(seatRows, seatGroupIndex, 2, 2, i);
			makeSeat(seatRows, seatGroupIndex, 3, 3, i + 1);
			seatGroupIndex++;
		}

		// find doors, interim destinations, and persons

		Rectangle2D longAisle = createFilterRect(leftmostCompartment.getMinY() + trainGeometry.getBenchWidth(),
				trainGeometry.getAisleWidth());
		interimDestinations = findTargets(longAisle);

		Rectangle2D leftDoorsRect = createFilterRect(leftmostCompartment.getMinY() - 0.5, 1);
		Rectangle2D rightDoorsRect = createFilterRect(leftmostCompartment.getMaxY() - 0.5, 1);
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

	public int getNumberOfEntranceAreas() {
		return numberOfEntranceAreas;
	}

	public List<SeatGroup> getSeatGroups() {
		return Collections.unmodifiableList(seatGroups);
	}

	public List<Seat> getSeats() {
		return Collections.unmodifiableList(seats);
	}

	/** Return a new list of all pedestrians in the scenario. */
	public List<Pedestrian> getPedestrians() {
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

	private void initialize(int numberOfEntranceAreas) {
		this.numberOfEntranceAreas = numberOfEntranceAreas;
		final int numberOfSeatGroups = 4 * numberOfEntranceAreas;
		final int numberOfSeats = 4 * numberOfSeatGroups;

		seats = Arrays.asList(new Seat[numberOfSeats]);
		seatGroups = new ArrayList<>(numberOfSeatGroups);
		for (int i = 0; i < numberOfSeatGroups; i++) {
			seatGroups.add(new SeatGroup(this, i));
		}
	}

	private void makeSeat(List<List<Target>> longRows, int seatGroupIndex, int indexInSeatGroup, int longRowIndex,
			int indexInLongRow) {
		getSeatGroups().get(seatGroupIndex).setSeat(indexInSeatGroup,
				new Seat(longRows.get(longRowIndex).get(indexInLongRow)));
	}

	private Rectangle2D createFilterRect(double y, double height) {
		return new Rectangle2D.Double(
				topography.getBounds().getMinX(), y,
				topography.getBounds().getWidth(), height);
	}

}
