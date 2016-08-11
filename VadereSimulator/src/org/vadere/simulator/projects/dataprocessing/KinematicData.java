package org.vadere.simulator.projects.dataprocessing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * The KinematicData object holds the position (x,y), ID and a data string that
 * indicates whether the ped was created ("c"), deleted ("d") or moved ("m") in
 * this step.
 * 
 *
 */
public class KinematicData implements TimeStepData {
	public final int ID;
	public final double x;
	public final double y;
	public final String data;

	public KinematicData(int ID, double x, double y, String data) {
		this.ID = ID;
		this.x = x;
		this.y = y;
		this.data = data;
	}

	/**
	 * Returns the positions of all pedestrians in the given KinematicData
	 * object.
	 * 
	 * @param kinematics
	 *        the KinematicData object of one time step
	 * @return all positions of all pedestrians that were present in this step.
	 */
	public static Map<Integer, VPoint> getPedPositions(
			List<TimeStepData> kinematics) {
		Map<Integer, VPoint> result = new HashMap<Integer, VPoint>();

		for (TimeStepData data : kinematics) {
			KinematicData kdata = ((KinematicData) data);
			if (kdata.data.equals("m")) {
				result.put(kdata.ID, new VPoint(kdata.x, kdata.y));
			}
		}

		return result;
	}
}
