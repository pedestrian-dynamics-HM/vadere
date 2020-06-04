package org.vadere.gui.components.model;


import java.awt.*;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

public abstract class SimulationModel<T extends DefaultSimulationConfig> extends DefaultModel {

	public final T config;
	private ConcurrentHashMap<Integer, Color> colorMap;
	private Random random;

	@SuppressWarnings("unchecked")
	public SimulationModel(final T config) {
		super(config);
		this.config = config;
		this.colorMap = new ConcurrentHashMap<>();
		this.colorMap.put(-1, config.getPedestrianDefaultColor());
		this.random = new Random();
	}

	public abstract Collection<Agent> getAgents();

	public abstract Collection<Pedestrian> getPedestrians();

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

	public IMesh<?, ?, ?> getFloorFieldMesh() {
		return new PMesh();
	}

	public abstract void setAgentColoring(@NotNull final AgentColoring agentColoring);

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

    public abstract boolean isAlive(int pedId);

	public Color getGroupColor(@NotNull final  Pedestrian ped) {
		if (ped.getGroupIds().isEmpty() || (!ped.getGroupSizes().isEmpty() && ped.getGroupSizes().getFirst() == 1)) {
			return config.getPedestrianDefaultColor();
		}

		int groupId = ped.getGroupIds().getFirst();
		Color c = colorMap.get(groupId);
		if (c == null) {
			c = new Color(Color.HSBtoRGB(random.nextFloat(), 1f, 0.75f));
			colorMap.put(groupId, c);
		}
		return c;
	}

	@Override
	public synchronized void notifyObservers() {
		// synchronized (config) {
		if (config.hasChanged()) {
			setChanged();
			config.clearChange();
			if(config.getAgentColoring() != AgentColoring.RANDOM) {
				config.clearRandomColors();
			}
		}
		// }
		super.notifyObservers();
	}

}
