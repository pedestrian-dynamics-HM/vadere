package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

@ModelClass
public class PotentialFieldTargetRingExperiment implements IPotentialFieldTargetGrid {

	private AttributesPotentialRingExperiment attributes;

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
	public double getPotential(IPoint pos, Agent ped) {
		Vector2D pedestrian = new Vector2D(ped.getPosition());
		Vector2D center = new Vector2D(attributes.getCenter());

		Vector2D centerToPedestrian = pedestrian.sub(center);
		VPoint rotatedVector = centerToPedestrian.rotate(Math.PI / 2);
		Vector2D tangent = new Vector2D(rotatedVector);

		double stepLength = attributes.getPedestrianRadius();

		if (ped instanceof PedestrianOSM) {
			PedestrianOSM pedestrianOSM = (PedestrianOSM) ped;
			stepLength = pedestrianOSM.getDesiredStepSize();
		}

		Vector2D normalizedTangent = tangent.normalize(stepLength);
		Vector2D bestNextPosition = pedestrian.add(normalizedTangent);

		return bestNextPosition.distance(pos);
	}

	@Override
	public Vector2D getTargetPotentialGradient(VPoint pos, Agent ped) {
		Vector2D pedestrian = new Vector2D(ped.getPosition());
		Vector2D center = new Vector2D(attributes.getCenter());

		Vector2D centerToPedestrian = pedestrian.sub(center);
		VPoint rotatedVector = centerToPedestrian.rotate(Math.PI / 2);

		return new Vector2D(rotatedVector);
	}

    @Override
    public IPotentialField getSolution() {
        throw new UnsupportedOperationException("not jet implemented.");
    }

	@Override
	public Function<Agent, IMesh<?, ?, ?>> getDiscretization() {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	@Override
	public double getPotential(@NotNull IPoint pos, int targetId) {
		throw new UnsupportedOperationException("not jet implemented.");
	}


	@Override
    public PotentialFieldTargetRingExperiment clone() {
        try {
            PotentialFieldTargetRingExperiment clone = (PotentialFieldTargetRingExperiment)super.clone();
            clone.attributes = (AttributesPotentialRingExperiment) attributes.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
    }

    @Override
	public HashMap<Integer, CellGrid> getCellGrids() {
		throw new UnsupportedOperationException("not jet implemented.");
	}

	@Override
	public void initialize(List<Attributes> attributesList, Domain topography,
	                       AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}

}
