package org.vadere.simulator.control;

import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.ObstacleDistancePotential;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.TargetPedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

public class TopographyController extends OfflineTopographyController {

	private final Topography topography;
	private final DynamicElementFactory dynamicElementFactory;

	public TopographyController(Topography topography, DynamicElementFactory dynamicElementFactory) {
		super(topography);
		this.topography = topography;
		this.dynamicElementFactory = dynamicElementFactory;
	}

	public Topography getTopography() {
		return this.topography;
	}

	public void preLoop(double simTimeInSec) {
		// add bounding box
		prepareTopography();

		// TODO [priority=medium] [task=feature] create initial cars

		// create initial pedestrians
		for (Pedestrian initialValues : topography
				.getInitialElements(Pedestrian.class)) {
			Pedestrian realPed = (Pedestrian) dynamicElementFactory.createElement(initialValues.getPosition(),
					initialValues.getId(), Pedestrian.class);
			realPed.setIdAsTarget(initialValues.getIdAsTarget());
			if (realPed.getIdAsTarget() != -1) {
				topography.addTarget(new TargetPedestrian(realPed));
			}

			// set the closest target as default, which can be a problem if no target should be used
			/*
			 * if (pedStore.getTargets().size() == 0){
			 * LinkedList<Integer> tmp = new LinkedList<Integer>();
			 * tmp.add(topography.getNearestTarget(pedStore.getPosition()));
			 * realPed.setTargets(tmp);
			 * }
			 * else {
			 * realPed.setTargets(new LinkedList<>(pedStore.getTargets()));
			 * }
			 */
			realPed.setTargets(new LinkedList<>(initialValues.getTargets()));
			realPed.setGroupIds(new LinkedList<>(initialValues.getGroupIds()));
			realPed.setChild(initialValues.isChild());
			realPed.setLikelyInjured(initialValues.isLikelyInjured());

			if (initialValues.getFreeFlowSpeed() > 0) {
				realPed.setFreeFlowSpeed(initialValues.getFreeFlowSpeed());
			}

			if (!Double.isNaN(initialValues.getVelocity().x) && !Double.isNaN(initialValues.getVelocity().y)) {
				realPed.setVelocity(initialValues.getVelocity());
			}
			topography.addElement(realPed);
		}
	}

	public void update(double simTimeInSec) {
		recomputeCells();
	}

	public void postLoop(double simTimeInSec) {
		topography.reset();
	}
}
