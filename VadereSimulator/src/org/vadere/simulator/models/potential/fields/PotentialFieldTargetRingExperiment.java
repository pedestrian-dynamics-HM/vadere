package org.vadere.simulator.models.potential.fields;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.potential.CellGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class PotentialFieldTargetRingExperiment implements IPotentialTargetGrid {

	private final AttributesPotentialRingExperiment attributes;

	public PotentialFieldTargetRingExperiment(AttributesPotentialRingExperiment attributes) {
		this.attributes = attributes;
	}

	@Override
	public void preLoop(double simTimeInSec) {}

	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void update(double simTimeInSec) {}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	/**
	 * The pedestrians should move on a circular course.
	 * 
	 * Therefore, calculate the vector centerToPedestrian = pedestrian - center (of ring).
	 * Rotate this vector by 90 degree (counter-clockwise) to get the tangent vector to the circle.
	 * Afterwards, rate "pos" and check if it lies in the same direction as tangent vector.
	 */
	@Override
	public double getTargetPotential(VPoint pos, Agent ped) {
		Vector2D pedestrian = new Vector2D(ped.getPosition());
		Vector2D center = new Vector2D(attributes.getCenter());

		Vector2D centerToPedestrian = pedestrian.sub(center);
		VPoint rotatedVector = centerToPedestrian.rotate(Math.PI / 2);
		Vector2D tangent = new Vector2D(rotatedVector);

		double stepLength = attributes.getPedestrianRadius();

		if (ped instanceof PedestrianOSM) {
			PedestrianOSM pedestrianOSM = (PedestrianOSM) ped;
			stepLength = pedestrianOSM.getStepSize();
		}

		Vector2D normalizedTangent = tangent.normalize(stepLength);
		Vector2D bestNextPosition = pedestrian.add(normalizedTangent);

		double potential = bestNextPosition.distance(pos);

		return potential;
	}

	@Override
	public Vector2D getTargetPotentialGradient(VPoint pos, Agent ped) {
		Vector2D pedestrian = new Vector2D(ped.getPosition());
		Vector2D center = new Vector2D(attributes.getCenter());

		Vector2D centerToPedestrian = pedestrian.sub(center);
		VPoint rotatedVector = centerToPedestrian.rotate(Math.PI / 2);
		Vector2D tangent = new Vector2D(rotatedVector);

		return tangent;
	}

	@Override
	public HashMap<Integer, CellGrid> getCellGrids() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}

}
