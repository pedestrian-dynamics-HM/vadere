package org.vadere.simulator.models.groups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.models.potential.fields.PotentialFieldTarget;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

public class CentroidGroup implements Group {

	public final static int POTENTIAL_DISTANCE_THRESHOLD = 1000;
	public final static int MAX_NO_VISION_OF_LEADER = 25;

	final private int id;
	final private int size;

	final protected List<Pedestrian> members;

	private double groupVelocity;

	private final List<Pedestrian> lostMembers;
	private Map<Pedestrian, Map<Pedestrian, VPoint>> lastVision;
	private final Map<Pedestrian, Integer> noVisionOfLeaderCount;
	private final PotentialFieldTarget potentialFieldTarget;

	public CentroidGroup(int id, int size,
			PotentialFieldTarget potentialFieldTarget) {
		this.id = id;
		this.size = size;
		this.potentialFieldTarget = potentialFieldTarget;
		members = new ArrayList<>();

		this.lastVision = new HashMap<>();
		this.lostMembers = new LinkedList<>();
		this.noVisionOfLeaderCount = new HashMap<>();
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		return getID();
	}

	@Override
	public boolean equals(Group o) {
		boolean result = false;

		if (this == o) {
			result = true;
		} else if (o instanceof CentroidGroup) {
			Group other = o;

			if (this.getID() == other.getID()) {
				result = true;
			}
		}

		return result;
	}

	@Override
	public List<Pedestrian> getMembers() {
		return members;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public boolean isMember(Pedestrian ped) {
		return members.contains(ped);
	}

	@Override
	public boolean isFull() {
		boolean result = true;

		if (members.size() < size) {
			result = false;
		}

		return result;
	}

	@Override
	public int getOpenPersons() {
		return size - members.size();
	}

	public VPoint getCentroid() {

		double sumx = 0.0;
		double sumy = 0.0;

		int size = members.size();

		for (Pedestrian p : members) {
			sumx += p.getPosition().getX();
			sumy += p.getPosition().getY();
		}

		double[] result = {sumx / size, sumy / size};

		return new VPoint(result[0], result[1]);
	}

	public VPoint getCentroidOthers(Pedestrian ped) {

		double sumx = 0.0;
		double sumy = 0.0;

		int size = 0;

		for (Pedestrian p : members) {
			if (!ped.equals(p) && !isLostMember(p)) {
				sumx += p.getPosition().getX();
				sumy += p.getPosition().getY();

				size++;
			}
		}

		double[] result = {sumx / size, sumy / size};

		if (size == 0) {
			VPoint pedPoint = ped.getPosition();
			result[0] = pedPoint.getX();
			result[1] = pedPoint.getY();
		}

		return new VPoint(result[0], result[1]);
	}

	@Override
	public void addMember(Pedestrian ped) {

		if (members.size() == 0) {
			this.groupVelocity = ped.getFreeFlowSpeed();
		} else if (isFull()) {
			throw new IllegalArgumentException("Group is full.");
		}

		lastVision.put(ped, new HashMap<Pedestrian, VPoint>());
		noVisionOfLeaderCount.put(ped, 0);

		members.add(ped);

	}

	public double getGroupVelocity() {
		return groupVelocity;
	}

	void setLastVision(Pedestrian ped, Pedestrian p) {
		lastVision.get(ped).put(p, p.getPosition());
	}

	VPoint getLastVision(Pedestrian ped, Pedestrian leader) {
		return lastVision.get(ped).get(leader);
	}

	boolean isLostMember(Pedestrian p) {
		return lostMembers.contains(p);
	}

	void setLostMember(Pedestrian ped) {
		lostMembers.add(ped);
	}

	void wakeFromLostMember(Pedestrian ped) {
		lostMembers.remove(ped);
	}

	int getNoVisionOfLeaderCount(Pedestrian ped) {
		return noVisionOfLeaderCount.get(ped);
	}

	void resetNoVisionOfLeaderCount(Pedestrian ped) {
		noVisionOfLeaderCount.put(ped, 0);
	}

	void incrementNoVisionOfLeaderCount(Pedestrian ped) {
		int currentNoVisions = noVisionOfLeaderCount.get(ped) + 1;
		noVisionOfLeaderCount.put(ped, currentNoVisions);
	}

	public double getRelativeDistanceCentroid(Pedestrian ped) {
		double result;
		VPoint pedLocation = ped.getPosition();

		double pedDistance = potentialFieldTarget.getTargetPotential(pedLocation, ped);
		double centroidDistance = potentialFieldTarget.getTargetPotential(getCentroidOthers(ped), ped);

		result = centroidDistance - pedDistance;

		if (result > POTENTIAL_DISTANCE_THRESHOLD) {
			result = 0;
		}

		return result;
	}

	public Pedestrian getLeader(Pedestrian ped) {
		Pedestrian result = members.get(0);

		double smallestDistance = potentialFieldTarget.getTargetPotential(result.getPosition(), ped);

		for (Pedestrian p : members) {
			double pedDistance = potentialFieldTarget.getTargetPotential(p.getPosition(), p);
			if (pedDistance < smallestDistance) {
				result = p;
				smallestDistance = pedDistance;
			}
		}

		if (ped.getId() == result.getId() || isLostMember(ped)) {
			result = null;
		}

		return result;
	}
}
