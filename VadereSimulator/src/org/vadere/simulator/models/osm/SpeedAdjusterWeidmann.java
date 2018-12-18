package org.vadere.simulator.models.osm;

import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.state.scenario.Pedestrian;

/**
 * Adjusts the desired speed according to the Weidmann fundamental diagram,
 * which means: the higher the density, the lower the speed.
 * 
 * TODO: [priority=low] [task=feature] NOT IMPLEMENTED!
 * 
 */
@Deprecated
public class SpeedAdjusterWeidmann implements SpeedAdjuster {

	@Override
	public double getAdjustedSpeed(Pedestrian ped, double originalSpeed) {
		double result = 1.0;

		double diff = 0; // ped.getSpeedByTargetPotential() -
		// getWeidmannSpeed(getCurrentDensity(ped));

		if (diff > 0) {
			result = 0.0;
		}

		throw new UnsupportedOperationException("method is not implemented jet.");
		// return originalSpeed * result;
	}

	private double getWeidmannSpeed(double density) {
		return 1.34 * (1 - Math.exp(-1.913 * (1 / density - 1 / 5.4)));
	}

	/**
	 * Density = number of visible pedestrians / half circle around pedestrian.
	 * personalDensityFactor reduces artifacts caused by neglect of pedestrian
	 * dimension.
	 */

	/*
	 * private double getCurrentDensity(Pedestrian ped) { return
	 * getVisiblePedestrians(ped).size() * 2.0 *
	 * attributesPotential.getPersonalDensityFactor() /
	 * (Math.pow(attributesPotential.getPedestrianRecognitionDistance(), 2) *
	 * Math.PI); }
	 * 
	 * private HashSet<ScenarioElement> getVisiblePedestrians(Pedestrian ped) {
	 * List<Integer> targetIds = ped.getTargets();
	 * 
	 * HashSet<ScenarioElement> visiblePedestrians = new
	 * HashSet<ScenarioElement>(); double targetPotentialPed =
	 * potentialFieldTarget.getPotential(targetIds, ped.getPosition());
	 * 
	 * for (Pedestrian neighbor : closePedestrians) { double
	 * targetPotentialNeighbor =
	 * potentialFieldTarget.getPotential(targetIds,
	 * neighbor.getPosition());
	 * 
	 * // See OSM Paper if (targetPotentialPed - 0.1 > targetPotentialNeighbor)
	 * { visiblePedestrians.add(neighbor); } }
	 * 
	 * return visiblePedestrians; }
	 */
}
