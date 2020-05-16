package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;

public class PedestriansNearbyData {

	private final int pedId1;
	private final int pedId2;
	private int durationTimesteps;
	private int startTimestep;
	private List<VPoint> trajectory;




	public int getStartTimestep() {
		return startTimestep;
	}

	public PedestriansNearbyData(int ped1, int ped2, final int durationTimesteps, int startTimestep, List<VPoint> contactTrajectory){
		this.pedId1 = Math.min(ped1, ped2);
		this.pedId2 = Math.max(ped1, ped2);
		this.durationTimesteps = durationTimesteps;
		this.startTimestep = startTimestep;
		this.trajectory = contactTrajectory;
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
		return new PedestriansNearbyData(getPedId1(), getPedId2(), getDurationTimesteps() + sampleEveryNthStep, getStartTimestep(),traj);
	}


	public String[] toStrings(){
		StringBuilder ret = new StringBuilder();
		List<VPoint> traj = getTrajectory();
		for (int i = 0; i < traj.size(); i++) {
			VPoint p = traj.get(i);
			ret.append(p.x).append(" ").append(p.y);
			if (i != traj.size() -1) {
				ret.append("\r\n").append("- - - - ");
			}
		}
		return new String[]{Integer.toString(durationTimesteps), ret.toString()};
	}

}
