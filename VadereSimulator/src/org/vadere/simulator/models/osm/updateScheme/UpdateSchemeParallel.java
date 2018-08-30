package org.vadere.simulator.models.osm.updateScheme;

import java.util.LinkedList;
import java.util.List;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.Vector2D;

public class UpdateSchemeParallel implements UpdateSchemeOSM {

	private final PedestrianOSM pedestrian;
	private boolean isMoving;

	public UpdateSchemeParallel(PedestrianOSM pedestrian) {
		this.pedestrian = pedestrian;
	}

	@Override
	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod) {
		switch (callMethod) {
			case SEEK:
				updateParallelSeek(timeStepInSec);
				break;
			case RETRY:
				updateParallelSeek(0.0);
			case MOVE:
				updateParallelMove(timeStepInSec);
				break;
			case CONFLICTS:
				updateParallelConflicts(timeStepInSec);
				break;
			case STEPS:
				updateParallelSteps(timeStepInSec);
				break;
			default:
				throw new UnsupportedOperationException();
		}
	}

	protected void updateParallelSeek(double timeStepInSec) {
		pedestrian.setTimeCredit(pedestrian.getTimeCredit() + timeStepInSec);
		pedestrian.setDurationNextStep(pedestrian.getStepSize() / pedestrian.getDesiredSpeed());

		if (pedestrian.getTimeCredit() > pedestrian.getDurationNextStep()) {
			pedestrian.updateNextPosition();
			this.isMoving = true;
		} else {
			this.isMoving = false;
		}
	}

	private void updateParallelMove(double timeStepInSec) {
		if (isMoving) {
			pedestrian.setLastPosition(pedestrian.getPosition());
			pedestrian.setPosition(pedestrian.getNextPosition());
		}
	}

	private void updateParallelConflicts(double timeStepInSec) {
		if (isMoving) {
			List<Agent> others = getCollisionPedestrians();

			boolean undoStep = false;

			for (Agent ped : others) {
				double creditOther = ((PedestrianOSM) ped).getTimeCredit();

				if (creditOther < pedestrian.getTimeCredit()) {
					undoStep = true;
					break;
				} else if (creditOther == pedestrian.getTimeCredit()
						&& ped.getId() < pedestrian.getId()) {
					undoStep = true;
					break;
				}
			}

			if (undoStep) {
				pedestrian.setPosition(pedestrian.getLastPosition());
			}
		}
	}

	private void updateParallelSteps(double timeStepInSec) {
		if (isMoving) {
			// did not want to make a step
			if (pedestrian.getNextPosition().equals(pedestrian.getLastPosition())) {
				pedestrian.setTimeCredit(0);
				pedestrian.setVelocity(new Vector2D(0, 0));
			}
			// made a step
			else if (!pedestrian.getPosition().equals(pedestrian.getLastPosition())) {
				pedestrian.setTimeCredit(pedestrian.getTimeCredit() - pedestrian.getDurationNextStep());

				// compute velocity by forward difference
				pedestrian.setVelocity(new Vector2D(
						pedestrian.getNextPosition().x - pedestrian.getLastPosition().x,
						pedestrian.getNextPosition().y - pedestrian.getLastPosition().y)
								.multiply(1.0 / timeStepInSec));
			}
			// wanted to make a step, but could not
			else {
				pedestrian.setVelocity(new Vector2D(0, 0));
			}
		}
	}

	private List<Agent> getCollisionPedestrians() {
		LinkedList<Agent> result = new LinkedList<>();

		for (Agent ped : pedestrian.getRelevantPedestrians()) {
			if (ped.getId() != pedestrian.getId()) {
				double thisDistance = ped.getPosition().distance(pedestrian.getPosition());

				if (ped.getRadius() + pedestrian.getRadius() > thisDistance) {
					result.add(ped);
				}
			}
		}

		return result;
	}
}
