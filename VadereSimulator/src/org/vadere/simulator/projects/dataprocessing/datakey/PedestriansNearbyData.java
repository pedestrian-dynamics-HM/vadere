package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;

public class PedestriansNearbyData {

	private final int pedId1;
	private final int pedId2;
	private int durationTimesteps;
	private int startTimestep;
	private List<VPoint> trajectory;
	private boolean printTrajectory;




	/*private PedestriansNearbyData() {
		this.pedId1 = -1;
		this.pedId2 = -1;
		this.durationTimesteps = 1;
		this.startTimestep = 1;
		this.trajectory = new ArrayList<>();
	}*/

	public int getStartTimestep() {
		return startTimestep;
	}

	public PedestriansNearbyData(int ped1, int ped2, final int durationTimesteps, int startTimestep, List<VPoint> contactTrajectory, boolean printTrajectory){
		this.pedId1 = Math.min(ped1, ped2);
		this.pedId2 = Math.max(ped1, ped2);
		this.durationTimesteps = durationTimesteps;
		this.startTimestep = startTimestep;
		this.trajectory = contactTrajectory;
		this.printTrajectory = printTrajectory;
	}

	public int getPedId1() {
		return pedId1;
	}

	public int getPedId2() {
		return pedId2;
	}

	public int getDurationTimesteps() {
		return durationTimesteps;
	}

	public List<VPoint> getTrajectory() {
		return trajectory;
	}
	public void addToContactTrajectory(VPoint point) {
		trajectory.add(point);
	}
	public void addToContactTrajectory(List<VPoint> points) {
		trajectory.addAll(points);
	}

	public PedestriansNearbyData getUpdatedData(PedestriansNearbyData newData, int sampleEveryNthStep) {
		List<VPoint> traj = getTrajectory();
		traj.addAll(newData.getTrajectory());
		return new PedestriansNearbyData(getPedId1(), getPedId2(), getDurationTimesteps() + sampleEveryNthStep, getStartTimestep(),traj, printTrajectory);
	}


	public String[] toStrings(){
		if (!printTrajectory) {
			return new String[]{Integer.toString(durationTimesteps)};
		}
		StringBuilder ret = new StringBuilder();
		List<VPoint> traj = getTrajectory();
		for (int i = 0; i < traj.size(); i++) {
			VPoint p = traj.get(i);
			ret.append(p.x).append(" ").append(p.y).append("\n");
			if (i != traj.size() -1) {
				ret.append("- - - - ");
			}
		}
		return new String[]{Integer.toString(durationTimesteps), ret.toString()};
	}

}
