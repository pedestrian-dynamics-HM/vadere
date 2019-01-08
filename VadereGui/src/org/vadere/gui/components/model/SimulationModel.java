package org.vadere.gui.components.model;


import java.util.Collection;
import java.util.function.Function;

import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.models.potential.solver.calculators.mesh.PotentialPoint;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.cellgrid.IPotentialPoint;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

public abstract class SimulationModel<T extends DefaultSimulationConfig> extends DefaultModel {

	public final T config;

	@SuppressWarnings("unchecked")
	public SimulationModel(final T config) {
		super(config);
		this.config = config;
	}

	public abstract Collection<Agent> getAgents();

	public abstract int getTopographyId();

	public abstract double getSimTimeInSec();

	public abstract Function<IPoint, Double> getPotentialField();

	public abstract boolean isFloorFieldAvailable();

	@Override
	public void resetTopographySize() {
		fireChangeViewportEvent(new VRectangle(getTopographyBound()));
	}

	public T getConfig() {
		return config;
	}

	public IMesh<? extends IPotentialPoint, ?, ?, ?, ?, ?> getDiscretization() {
		return new PMesh<IPotentialPoint, Object, Object>((x,y) -> new PotentialPoint(x,y));
	}

    /*public double getPotential(final int x, final int y) {
        double result = 0.0;

        VPoint pos = pixelToWorld(new VPoint(x, y));
        // VPoint pos = new VPoint(x,y);

        Optional<Function<VPoint, Double>> optPotentialField = getPotentialField();

        if (optPotentialField.isPresent()) {
            CellGrid potentialField = optPotentialField.get();
            int incX = 1;
            int incY = 1;

            Point gridPoint = potentialField.getNearestPointTowardsOrigin(pos);

            if (gridPoint.x + 1 >= potentialField.getNumPointsX()) {
                incX = 0;
            }

            if (gridPoint.y + 1 >= potentialField.getNumPointsY()) {
                incY = 0;
            }


            VPoint gridPointCoord = potentialField.pointToCoord(gridPoint);

            double z1 = potentialField.getValue(gridPoint).potential;
            double z2 = potentialField.getValue(new Point(gridPoint.x + incX, gridPoint.y)).potential;
            double z3 = potentialField.getValue(new Point(gridPoint.x + incX, gridPoint.y + incY)).potential;
            double z4 = potentialField.getValue(new Point(gridPoint.x, gridPoint.y + incY)).potential;

            double t = (pos.x - gridPointCoord.x) / potentialField.getResolution();
            double u = (pos.y - gridPointCoord.y) / potentialField.getResolution();

            result = InterpolationUtil.bilinearInterpolation(z1, z2, z3, z4, t, u);
        }

        return result;
    }*/

	@Override
	public void notifyObservers() {
		// synchronized (config) {
		if (config.hasChanged()) {
			setChanged();
			config.clearChange();
			if(!config.isUseRandomPedestrianColors()) {
				config.clearRandomColors();
			}
		}
		// }
		super.notifyObservers();
	}

}
