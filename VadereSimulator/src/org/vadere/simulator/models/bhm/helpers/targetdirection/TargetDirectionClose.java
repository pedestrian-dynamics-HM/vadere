package org.vadere.simulator.models.bhm.helpers.targetdirection;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.bhm.UtilsBHM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.shapes.VPoint;

public class TargetDirectionClose implements TargetDirection {

	private final PedestrianBHM pedestrianBHM;
	private final TargetDirection targetDirection;
	private final TargetDirection closeTargetDirectionFallback;
	private final IPotentialFieldTarget targetPotentialField;

	public TargetDirectionClose(@NotNull final PedestrianBHM pedestrianBHM, @NotNull final IPotentialFieldTarget targetPotentialField, @NotNull final TargetDirection targetDirection) {
		this.pedestrianBHM = pedestrianBHM;
		this.targetDirection = targetDirection;
		this.targetPotentialField = targetPotentialField;
		this.closeTargetDirectionFallback = new TargetDirectionEuclidean(pedestrianBHM);
	}

	@Override
	public VPoint getTargetDirection(@NotNull final Target target) {
		VPoint position = pedestrianBHM.getPosition();
		VPoint direction = targetDirection.getTargetDirection(target);
		VPoint nextPosition = UtilsBHM.getTargetStep(pedestrianBHM, position, direction);
		if(targetPotentialField.getPotential(nextPosition, pedestrianBHM) <= 0) {
			return closeTargetDirectionFallback.getTargetDirection(target);
		} else {
			return direction;
		}
	}
}
