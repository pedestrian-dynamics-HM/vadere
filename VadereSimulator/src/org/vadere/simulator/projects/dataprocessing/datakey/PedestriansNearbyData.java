package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PedestriansNearbyData {

	private final int pedId1;
	private final int pedId2;
	private int durationTimesteps;
	private int startTimestep;
	private List<VPoint> trajectory;
	private boolean printTrajectory;
	private boolean printForPostVis; // is an ugly one time thing that shouldn't be merged




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

	public PedestriansNearbyData(int ped1, int ped2, final int durationTimesteps, int startTimestep, List<VPoint> contactTrajectory, boolean printTrajectory, boolean printForPostVis){
		this.pedId1 = Math.min(ped1, ped2);
		this.pedId2 = Math.max(ped1, ped2);
		this.durationTimesteps = durationTimesteps;
		this.startTimestep = startTimestep;
		this.trajectory = contactTrajectory;
		this.printTrajectory = printTrajectory;
		this.printForPostVis = printForPostVis;
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

	public boolean isPrintForPostVis() {
		return printForPostVis;
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
		return new PedestriansNearbyData(getPedId1(), getPedId2(), getDurationTimesteps() + sampleEveryNthStep, getStartTimestep(),traj, printTrajectory, printForPostVis);
	}


	public String[] toStrings(){
		// printForPostVis is an ugly one time thing that shouldn't be merged
		if (printForPostVis) {
			StringBuilder ret = new StringBuilder();
			List<VPoint> traj = getTrajectory();
			for (int i = 0; i < traj.size(); i++) {
				VPoint p = traj.get(i);
				VPoint pNext;
				if (i != traj.size() -1) {
					pNext = traj.get(i + 1);
				} else {
					pNext = traj.get(i);
				}
				Random rand = new Random();
				double r1 = rand.nextDouble();
				r1 = r1*0.3 - 0.15;
				double r2 = rand.nextDouble();
				r2 = r2*0.3 - 0.15;
				ret.append(hashCode()).append(" ").append(startTimestep*0.4 + i*0.4).append(" ").append(startTimestep*0.4 + (i+1)*0.4).append(" ").append(p.x + r2).append(" ").append(p.y + r1).append(" ").append(pNext.x + r1).append(" ").append(pNext.y + r2).append(" 9999");
				if (i != traj.size() -1) {
					ret.append("\n");
				}
			}
			return new String[]{ret.toString()};
		}
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
