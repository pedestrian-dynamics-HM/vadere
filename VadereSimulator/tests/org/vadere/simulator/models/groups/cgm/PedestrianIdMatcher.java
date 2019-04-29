package org.vadere.simulator.models.groups.cgm;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.vadere.state.scenario.PedestrianPair;

/**
 * 	Check if a Pair of pedestrians have the specified ids.
 */
public class PedestrianIdMatcher extends TypeSafeMatcher<PedestrianPair> {

	private int idLeft;
	private int idRight;

	public static Matcher<PedestrianPair> containsPedIds(int idLeft, int idRight) {
		return new PedestrianIdMatcher(idLeft, idRight);
	}

	private PedestrianIdMatcher(int idLeft, int idRight){
		this.idLeft = idLeft;
		this.idRight = idRight;
	}

	@Override
	protected boolean matchesSafely(PedestrianPair pedPair) {
		if (pedPair.getLeftId() == idLeft){
			return pedPair.getRightId() == idRight;
		}
		return false;
	}

	@Override
	public void describeTo(Description description) {
		String msg = String.format("PedIds (%d, %d)",
				idLeft,
				idRight);
		description.appendText("Pedestrian order in Pair was expected to be ").appendText(msg);

	}

	@Override
	protected void describeMismatchSafely(PedestrianPair item, Description mismatchDescription) {
		super.describeMismatchSafely(item, mismatchDescription);
		String wasMsg = String.format("PedIds (%d, %d)",
				item.getLeft().getId(),
				item.getRight().getId());
		mismatchDescription.appendText(" ").appendText(wasMsg);
	}

}