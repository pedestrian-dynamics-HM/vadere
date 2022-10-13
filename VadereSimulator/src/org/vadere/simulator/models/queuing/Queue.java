package org.vadere.simulator.models.queuing;

import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Queue is connected to a Target. All Pedestrians with the Target are out of the queue.
 * After the QueueDetector detect a queue, the queue is active as long as there are Pedestrians
 * in the queue.
 *
 */
public class Queue implements DynamicElementRemoveListener<Pedestrian>, DynamicElementAddListener<Pedestrian> {

	private static final double INITIAL_DISTANCE = 3.0; // 5.0
	private final Map<Integer, Pedestrian> pedestrianInQueue;
	private final Map<Integer, Pedestrian> pedestrianOutQueue;
	private List<VShape> polytopes;
	private final int queueTailId;
	private final int targetId;
	private final QueueDetector detector;
	private final Topography topography;
	private boolean active;

	public Queue(final Topography topography, final int targetId, final QueueDetector detector) {
		this.detector = detector;
		this.topography = topography;
		this.active = false;
		this.pedestrianInQueue = new HashMap<>();
		this.pedestrianOutQueue = new HashMap<>();
		this.polytopes = new ArrayList<>();
		this.targetId = targetId;

		// add Listeners
		this.topography.addElementAddedListener(Pedestrian.class, this);
		this.topography.addElementRemovedListener(Pedestrian.class, this);
		this.queueTailId = topography.getNextFreeTargetID();

		// add Pedestrians
		topography.getElements(Pedestrian.class).stream().forEach(ped -> elementAdded(ped));
	}

	public List<Target> getTarget() {
		return polytopes.stream().map(p -> new Target(new AttributesTarget(p, queueTailId)))
				.collect(Collectors.toList());
	}

	public boolean isQueued(final Pedestrian pedestrian) {
		return pedestrianInQueue.containsKey(pedestrian.getId());
	}

	public boolean isNotQueued(final Pedestrian pedestrian) {
		return pedestrianOutQueue.containsKey(pedestrian.getId());
	}

	@Override
	public void elementRemoved(final Pedestrian ped) {
		pedestrianInQueue.remove(ped.getId());
		pedestrianOutQueue.remove(ped.getId());
	}

	@Override
	public void elementAdded(Pedestrian ped) {
		if (ped.getNextTargetId() == targetId) {
			if (isActive()) {
				ped.getTargets().addFirst(queueTailId);
			}
			pedestrianOutQueue.put(ped.getId(), ped);
		}
	}

	/*public double getValue(double x, double y) {
		return detector.get
				getPotentialField().getValue(new Point((int) Math.round(x / detector.getResolution()),
				(int) Math.round(y / detector.getResolution()))).potential;
	}*/

	public void update() {
		detector.setPolytope(null);
		detector.update();
		polytopes = detector.getTargetPoints().stream().map(p -> new VCircle(p, 0.3)).collect(Collectors.toList());


		if (!polytopes.isEmpty()) {
			// initialize the queue: All pedestrians near any queue tail targets will be queued up.
			List<Pedestrian> queuedPeds = null;
			if (!isActive()) {
				queuedPeds = pedestrianOutQueue.values().stream()
						.filter(ped -> polytopes.stream()
								.anyMatch(p -> p.distance(ped.getPosition()) < INITIAL_DISTANCE))
						.collect(Collectors.toList());
				queuedPeds.stream().forEach(ped -> queuePedestrian(ped));
				this.active = true;
			} else {
				// all pedestrians that have reached their tail target will be queued up
				queuedPeds = pedestrianOutQueue.values().stream()
						.filter(p -> p.getNextTargetId() == targetId).collect(Collectors.toList());

			}
			queuedPeds.stream().forEach(ped -> queuePedestrian(ped));

			/**
			 * Reposition the tail target.
			 */
			topography.getTargets().removeIf(target -> target.getId() == this.queueTailId);
			polytopes.forEach(polytope -> topography.getTargets()
					.add(new TargetQueue(new AttributesTarget(polytope, queueTailId))));
		}
	}

	private void queuePedestrian(final Pedestrian ped) {
		pedestrianOutQueue.remove(ped.getId());
		pedestrianInQueue.put(ped.getId(), ped);
		ped.getTargets().removeIf(id -> id == this.queueTailId);
	}

	public boolean isActive() {
		return active;
	}
}
