package org.vadere.simulator.dataprocessing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * This class helps to generate specific sets of positions and sets of pedestrians.
 *
 */
public class PointToPedestrianConverter {

	/**
	 * Converts a collection of points into a list of pedestrians. The only thing
	 * that is set is the position of the pedestrian, all other values will be
	 * equals the default values. Pedestrians may overlap themselves!
	 *
	 * @param points the collection of points that will be converted
	 * @return a list of pedestrians
	 */
	public static List<Agent> getPedestriansAt(final Collection<VPoint> points) {
		return getPedestriansAt(points.toArray(new VPoint[points.size()]));
	}

	/**
	 * Converts a array of points into a list of pedestrians. The only thing
	 * that is set is the position of the pedestrian, all other values will be
	 * equals the default values. Pedestrians may overlap themselves!
	 *
	 * @param points the array of points that will be converted
	 * @return a list of pedestrians
	 */
	public static List<Agent> getPedestriansAt(final VPoint... points) {
		List<Agent> pedestrianList = new ArrayList<>();
		for (VPoint point : points) {
			Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), new Random());
			pedestrian.setPosition(point);
			pedestrianList.add(pedestrian);
		}

		return pedestrianList;
	}
}
