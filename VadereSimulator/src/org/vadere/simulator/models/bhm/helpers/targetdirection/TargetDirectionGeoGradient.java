package org.vadere.simulator.models.bhm.helpers.targetdirection;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.bhm.UtilsBHM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

public class TargetDirectionGeoGradient implements TargetDirection {
	private static final Logger logger = Logger.getLogger(TargetDirectionGeoGradient.class);
	private PedestrianBHM pedestrianBHM;
	private IPotentialFieldTarget targetPotentialField;
	private TargetDirection fallBackStrategy;
	private TargetDirection closeFallBackStrategy;
	private double maxAngleBetweenGradients = Math.PI / 18; // difference of 180 / 18 = 10 degree

	public TargetDirectionGeoGradient(
			@NotNull final PedestrianBHM pedestrianBHM,
			@NotNull final IPotentialFieldTarget targetPotentialField) {
		this.pedestrianBHM = pedestrianBHM;
		this.targetPotentialField = targetPotentialField;
		this.fallBackStrategy = new TargetDirectionGeoOptimum(pedestrianBHM, targetPotentialField);
		this.closeFallBackStrategy = new TargetDirectionEuclidean(pedestrianBHM);
	}

	@Override
	public VPoint getTargetDirection(@NotNull final Target target) {
		VPoint position = pedestrianBHM.getPosition();
		double stepLength = pedestrianBHM.getStepLength();

		VPoint gradient1 = targetPotentialField.getTargetPotentialGradient(position, pedestrianBHM).multiply(-1.0);
		if(gradient1.distanceToOrigin() > GeometryUtils.DOUBLE_EPS) {
			VPoint gradient2 = targetPotentialField.getTargetPotentialGradient(position.add(gradient1.setMagnitude(stepLength)), pedestrianBHM).multiply(-1.0);

			if(gradient2.distanceToOrigin() > GeometryUtils.DOUBLE_EPS && UtilsBHM.angle(gradient1, gradient2) < maxAngleBetweenGradients) {
				//logger.info("gradient direction");
				return gradient1.norm();
			}
		}

		return fallBackStrategy.getTargetDirection(target);
	}
}
