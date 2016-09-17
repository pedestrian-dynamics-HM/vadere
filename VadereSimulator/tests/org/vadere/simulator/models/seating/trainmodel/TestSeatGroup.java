package org.vadere.simulator.models.seating.trainmodel;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.seating.TestTopographyAndModelBuilder;
import org.vadere.state.attributes.models.seating.SeatFacingDirection;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.SeatSide;
import org.vadere.state.scenario.Pedestrian;

public class TestSeatGroup {

	private TrainModel trainModel;
	private SeatGroup aSeatGroup;
	private Pedestrian aPerson;

	@Before
	public void setUp() {
		trainModel = new TestTopographyAndModelBuilder().getTrainModel();
		aSeatGroup = trainModel.getSeatGroup(0);
		aPerson = TestTrainModel.createTestPedestrian();
	}

	@Test
	public void testGetSeat() {
		SeatGroup sg = trainModel.getSeatGroup(0);
		assertEquals(trainModel.getSeats().get(0), sg.getSeat(0));
		
		sg = trainModel.getSeatGroup(2);
		Seat s = sg.getSeat(1);
		assertEquals(trainModel.getSeats().get(9), s);
//		assertTrue(s.isOccupied());
	}

	@Test
	public void testGetIndex() {
		for (int i = 0; i < trainModel.getSeatGroups().size(); i++) {
			assertEquals(0, trainModel.getSeatGroup(0).getIndex());
		}
	}

	@Test
	public void testGetPersonCount() {
		// as long as existing persons are not recognized:
		for (int i = 0; i < trainModel.getSeatGroups().size(); i++) {
			assertEquals(0, trainModel.getSeatGroup(0).getPersonCount());
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testGetTheAvailableSeatFail() {
		aSeatGroup.getTheAvailableSeat();
	}

	@Test
	public void testGetTheAvailableSeat() {
		final Integer[] is = { 0, 1, 2, 3 };
		final List<Integer> list = Arrays.asList(is);
		final int nTests = 6;
		for (int i = 0; i < nTests; i++) {

			Collections.shuffle(list);
			int j = 0;
			for (; j < 3; j++) {
				getSeatWithIndexFromShuffledList(list, j).setSittingPerson(aPerson);
			}

			assertEquals(getSeatWithIndexFromShuffledList(list, j), aSeatGroup.getTheAvailableSeat());

			for (j = 0; j < 4; j++) {
				aSeatGroup.getSeat(j).setSittingPerson(null);
			}
		}
	}

	private Seat getSeatWithIndexFromShuffledList(final List<Integer> shuffledList, int listIndex) {
		return aSeatGroup.getSeat(shuffledList.get(listIndex));
	}

	@Test(expected=IllegalStateException.class)
	public void testGetTheOccupiedSeatFail1() {
		aSeatGroup.getTheOccupiedSeat();
	}

	@Test(expected=IllegalStateException.class)
	public void testGetTheOccupiedSeatFail2() {
		sitDownTestPerson(0);
		sitDownTestPerson(1);
		aSeatGroup.getTheOccupiedSeat();
	}

	@Test(expected=IllegalStateException.class)
	public void testGetTheOccupiedSeatFail3() {
		sitDownTestPerson(0);
		sitDownTestPerson(1);
		sitDownTestPerson(2);
		aSeatGroup.getTheOccupiedSeat();
	}

	@Test
	public void testGetTheOccupiedSeat() {
		sitDownTestPerson(1);
		assertEquals(aSeatGroup.getSeat(1), aSeatGroup.getTheOccupiedSeat());
	}

	@Test(expected=IllegalStateException.class)
	public void testGetTheTwoAvailableSeatsFail() {
		aSeatGroup.getTheTwoAvailableSeats();
	}

	@Test
	public void testGetTheTwoAvailableSeats() {

		clearTestSeats();
		sitDownTestPerson(1);
		sitDownTestPerson(2);
		availableSeatsAre(0, 3);

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(3);
		availableSeatsAre(1, 2);

	}

	private void availableSeatsAre(int i, int j) {
		List<Seat> list = aSeatGroup.getTheTwoAvailableSeats();		
		assertTrue(aSeatGroup.getSeat(i) == list.get(0) || aSeatGroup.getSeat(i) == list.get(1));
		assertTrue(aSeatGroup.getSeat(j) == list.get(0) || aSeatGroup.getSeat(j) == list.get(1));
	}

	@Test
	public void testOnlySideChoice() {

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(1);
		assertTrue(aSeatGroup.onlySideChoice());

		clearTestSeats();
		sitDownTestPerson(2);
		sitDownTestPerson(3);
		assertTrue(aSeatGroup.onlySideChoice());

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(3);
		assertFalse(aSeatGroup.onlySideChoice());

		clearTestSeats();
		sitDownTestPerson(1);
		sitDownTestPerson(2);
		assertFalse(aSeatGroup.onlySideChoice());

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(2);
		assertFalse(aSeatGroup.onlySideChoice());

	}

	@Test
	public void testOnlyFacingDirectionChoice() {

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(2);
		assertTrue(aSeatGroup.onlyFacingDirectionChoice());

		clearTestSeats();
		sitDownTestPerson(1);
		sitDownTestPerson(3);
		assertTrue(aSeatGroup.onlyFacingDirectionChoice());

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(3);
		assertFalse(aSeatGroup.onlyFacingDirectionChoice());

		clearTestSeats();
		sitDownTestPerson(1);
		sitDownTestPerson(2);
		assertFalse(aSeatGroup.onlyFacingDirectionChoice());

		clearTestSeats();
		sitDownTestPerson(0);
		sitDownTestPerson(1);
		assertFalse(aSeatGroup.onlyFacingDirectionChoice());

	}

	@Test
	public void testSeatRelativeTo() {
		testRelativePosition(0, 2, SeatRelativePosition.ACROSS);
		testRelativePosition(2, 0, SeatRelativePosition.ACROSS);
		testRelativePosition(3, 1, SeatRelativePosition.ACROSS);

		testRelativePosition(0, 3, SeatRelativePosition.DIAGONAL);
		testRelativePosition(3, 0, SeatRelativePosition.DIAGONAL);
		testRelativePosition(2, 1, SeatRelativePosition.DIAGONAL);

		testRelativePosition(2, 3, SeatRelativePosition.NEXT);
		testRelativePosition(3, 2, SeatRelativePosition.NEXT);
		testRelativePosition(0, 1, SeatRelativePosition.NEXT);

	}

	private void testRelativePosition(int actual, int seat, SeatRelativePosition relativePosition) {
		assertEquals(aSeatGroup.getSeat(actual), aSeatGroup.seatRelativeTo(aSeatGroup.getSeat(seat), relativePosition));
	}

	@Test(expected=IllegalStateException.class)
	public void testAvailableSeatAtFacingDirectionFailForward() {
		sitDownTestPerson(2);
		sitDownTestPerson(3);
		aSeatGroup.availableSeatAtFacingDirection(SeatFacingDirection.FORWARD);
	}

	@Test(expected=IllegalStateException.class)
	public void testAvailableSeatAtFacingDirectionFailBackward() {
		sitDownTestPerson(0);
		sitDownTestPerson(1);
		aSeatGroup.availableSeatAtFacingDirection(SeatFacingDirection.BACKWARD);
	}

	@Test
	public void testAvailableSeatAtFacingDirection() {
		
		clearTestSeats();
		sitDownTestPerson(2);
		assertEquals(aSeatGroup.getSeat(3),
				aSeatGroup.availableSeatAtFacingDirection(SeatFacingDirection.FORWARD));

		clearTestSeats();
		sitDownTestPerson(3);
		assertEquals(aSeatGroup.getSeat(2),
				aSeatGroup.availableSeatAtFacingDirection(SeatFacingDirection.FORWARD));

		clearTestSeats();
		sitDownTestPerson(1);
		assertEquals(aSeatGroup.getSeat(0),
				aSeatGroup.availableSeatAtFacingDirection(SeatFacingDirection.BACKWARD));

		clearTestSeats();
		sitDownTestPerson(0);
		assertEquals(aSeatGroup.getSeat(1),
				aSeatGroup.availableSeatAtFacingDirection(SeatFacingDirection.BACKWARD));

	}

	@Test(expected=IllegalStateException.class)
	public void testAvailableSeatAtSideFailWindow() {
		sitDownTestPerson(0);
		sitDownTestPerson(2);
		aSeatGroup.availableSeatAtSide(SeatSide.WINDOW);
	}

	private void sitDownTestPerson(int seatIndex) {
		aSeatGroup.getSeat(seatIndex).setSittingPerson(aPerson);
	}

	@Test(expected=IllegalStateException.class)
	public void testAvailableSeatAtSideFailAisle() {
		sitDownTestPerson(1);
		sitDownTestPerson(3);
		aSeatGroup.availableSeatAtSide(SeatSide.AISLE);
	}

	@Test
	public void testAvailableSeatAtSideWindow1() {
		sitDownTestPerson(0);
		assertEquals(aSeatGroup.getSeat(2), aSeatGroup.availableSeatAtSide(SeatSide.WINDOW));
	}

	@Test
	public void testAvailableSeatAtSideWindow2() {
		sitDownTestPerson(2);
		assertEquals(aSeatGroup.getSeat(0), aSeatGroup.availableSeatAtSide(SeatSide.WINDOW));
	}

	@Test
	public void testAvailableSeatAtSideAisle1() {
		sitDownTestPerson(1);
		assertEquals(aSeatGroup.getSeat(3), aSeatGroup.availableSeatAtSide(SeatSide.AISLE));
	}

	@Test
	public void testAvailableSeatAtSideAisle2() {
		sitDownTestPerson(3);
		assertEquals(aSeatGroup.getSeat(1), aSeatGroup.availableSeatAtSide(SeatSide.AISLE));
	}

	private void clearTestSeats() {
		for (int i = 0; i < 4; i++) {
			aSeatGroup.getSeat(i).setSittingPerson(null);
		}
	}
}
