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
import org.vadere.state.attributes.models.seating.model.SeatPosition;
import org.vadere.state.scenario.Pedestrian;

public class TestSeatGroup {

	private TrainModel trainModel;
	private Pedestrian aPerson;
	private SeatGroup aSeatGroup;
	private SeatGroup rightSeatGroup;

	@Before
	public void setUp() {
		trainModel = new TestTopographyAndModelBuilder().getTrainModel();
		aSeatGroup = trainModel.getSeatGroup(0, 0);
		rightSeatGroup = trainModel.getSeatGroup(0, 1);
		aPerson = TestTrainModel.createTestPedestrian();
	}

	@Test
	public void testGetSeat() {
		SeatGroup sg = aSeatGroup;
		assertEquals(trainModel.getSeat(0, 0, 0), sg.getSeat(0));
		
		sg = trainModel.getSeatGroup(1, 0);
		Seat s = sg.getSeat(1);
		assertEquals(trainModel.getSeat(1, 0, 1), s);
//		assertTrue(s.isOccupied());
	}

	@Test
	public void testGetIndex() {
		for (int i = 0; i < trainModel.getSeatGroupCount(); i++) {
			assertEquals(0, trainModel.getSeatGroup(0, 0).getIndex());
		}
	}

	@Test
	public void testGetPersonCount() {
		// as long as existing persons are not recognized:
		for (int i = 0; i < trainModel.getSeatGroupCount(); i++) {
			assertEquals(0, trainModel.getSeatGroup(0, 0).getPersonCount());
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
	
	@Test(expected=IllegalArgumentException.class)
	public void testSeatRelativeToWithInvalidSeat() {
		aSeatGroup.seatRelativeTo(trainModel.getSeat(1, 1, 1), SeatRelativePosition.ACROSS);
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
		testAvailableSeat(aSeatGroup, SeatSide.WINDOW, 0, 2);
	}

	@Test
	public void testAvailableSeatAtSideWindow2() {
		testAvailableSeat(aSeatGroup, SeatSide.WINDOW, 2, 0);
	}

	@Test
	public void testAvailableSeatAtSideAisle1() {
		testAvailableSeat(aSeatGroup, SeatSide.AISLE, 1, 3);
	}

	@Test
	public void testAvailableSeatAtSideAisle2() {
		testAvailableSeat(aSeatGroup, SeatSide.AISLE, 3, 1);
	}

	@Test
	public void testAvailableSeatAtSideWindow1RightSeatGroup() {
		testAvailableSeat(rightSeatGroup, SeatSide.WINDOW, 1, 3);
	}

	@Test
	public void testAvailableSeatAtSideWindow2RightSeatGroup() {
		testAvailableSeat(rightSeatGroup, SeatSide.WINDOW, 3, 1);
	}

	@Test
	public void testAvailableSeatAtSideAisle1RightSeatGroup() {
		testAvailableSeat(rightSeatGroup, SeatSide.AISLE, 0, 2);
	}

	@Test
	public void testAvailableSeatAtSideAisle2RightSeatGroup() {
		testAvailableSeat(rightSeatGroup, SeatSide.AISLE, 2, 0);
	}
	
	@Test
	public void testGetCompartment() {
		assertEquals(trainModel.getCompartment(0), aSeatGroup.getCompartment());
		assertEquals(trainModel.getCompartment(0), rightSeatGroup.getCompartment());
		
		assertEquals(trainModel.getCompartment(1), trainModel.getSeatGroup(1, 3).getCompartment());
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testGetSeatsIsUnmodifiable() {
		aSeatGroup.getSeats().add(null);
	}
	
	@Test
	public void testGetSeats() {
		assertEquals(SeatGroup.SEATS_PER_SEAT_GROUP, aSeatGroup.getSeats().size());
		assertEquals(trainModel.getSeat(0, 0, 0), aSeatGroup.getSeats().get(0));
	}
	
	@Test
	public void testIsFull() {
		sitDownTestPerson(0);
		sitDownTestPerson(1);
		sitDownTestPerson(2);
		assertFalse(aSeatGroup.isFull());
		sitDownTestPerson(3);
		assertTrue(aSeatGroup.isFull());
	}
	
	@Test
	public void testIsAtLeftSide() {
		assertTrue(aSeatGroup.isAtLeftSide());
		assertFalse(rightSeatGroup.isAtLeftSide());
	}
	
	@Test
	public void testGetSeatByPosition() {
		assertEquals(aSeatGroup.getSeat(0), aSeatGroup.getSeatByPosition(SeatPosition.WINDOW_BACKWARD));
		assertEquals(aSeatGroup.getSeat(1), aSeatGroup.getSeatByPosition(SeatPosition.AISLE_BACKWARD));
		assertEquals(aSeatGroup.getSeat(2), aSeatGroup.getSeatByPosition(SeatPosition.WINDOW_FORWARD));
		assertEquals(aSeatGroup.getSeat(3), aSeatGroup.getSeatByPosition(SeatPosition.AISLE_FORWARD));

		assertEquals(rightSeatGroup.getSeat(0), rightSeatGroup.getSeatByPosition(SeatPosition.AISLE_BACKWARD));
		assertEquals(rightSeatGroup.getSeat(1), rightSeatGroup.getSeatByPosition(SeatPosition.WINDOW_BACKWARD));
		assertEquals(rightSeatGroup.getSeat(2), rightSeatGroup.getSeatByPosition(SeatPosition.AISLE_FORWARD));
		assertEquals(rightSeatGroup.getSeat(3), rightSeatGroup.getSeatByPosition(SeatPosition.WINDOW_FORWARD));
	}

	private void testAvailableSeat(SeatGroup seatGroup, SeatSide side, int personSeatIndex, int expectedSeatIndex) {
		seatGroup.getSeat(personSeatIndex).setSittingPerson(aPerson);
		assertEquals(seatGroup.getSeat(expectedSeatIndex), seatGroup.availableSeatAtSide(side));
	}

	private void clearTestSeats() {
		for (int i = 0; i < 4; i++) {
			aSeatGroup.getSeat(i).setSittingPerson(null);
		}
	}
}
