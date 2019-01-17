package org.vadere.simulator.models.groups.cgm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
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
	private  IPotentialFieldTarget potentialFieldTarget;

	public CentroidGroup(int id, int size,
				 IPotentialFieldTarget potentialFieldTarget) {
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
	public boolean equals(org.vadere.simulator.models.groups.Group o) {
		boolean result = false;
		if (o == null){
			return result;
		}

		if (this == o) {
			result = true;
		} else if (o instanceof Group) {
			org.vadere.simulator.models.groups.Group other = o;

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

	@Deprecated
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

	/**
	 *
	 * @param ped
	 * @return
	 */
	@Deprecated
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

		double[] result = new double[2];

		if (size == 0) {
			VPoint pedPoint = ped.getPosition();
			result[0] = pedPoint.getX();
			result[1] = pedPoint.getY();
		} else {
			result[0] = sumx / size;
			result[1] =  sumy / size;
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
		initGroupVelocity(); // ensure same speed for all members.

	}

	@Override
	public void removeMember(Pedestrian ped){
		members.remove(ped);
		lastVision.remove(ped);
		lostMembers.remove(ped);
		noVisionOfLeaderCount.remove(ped);
	}

	private void initGroupVelocity(){
		getMembers().forEach(m -> m.setFreeFlowSpeed(groupVelocity));
	}

	public double getGroupVelocity() {
		return groupVelocity;
	}

	/**
	 *
	 * @param ped	Pedestrian to which the distances is measured.
	 * @return		The maxDist within the group.
	 */
	public double getMaxDistToPedInGroup(Pedestrian ped){
		return members.stream()
				.mapToDouble(p-> p.getPosition().distance(ped.getPosition()))
				.max()
				.orElse(0.0);
	}

	/**
	 *
	 * @param ped	Pedestrian to which the distances is measured.
	 * @return		Id of other pedestrian
	 */
	public int getMaxDistPedIdInGroup(Pedestrian ped){
		double maxDist = getMaxDistToPedInGroup(ped);
		return members.stream()
				.filter(p-> p.getPosition().distance(ped.getPosition()) == maxDist)
				.findFirst().get().getId();
	}

	/**
	 *
	 * @return 	The maximal euclidean distance between two group members.
	 */
	public double getMaxDistInGroup(){
		double maxDist = 0.0;
		for (int i = 0; i < members.size(); i++) {
			for (int j = 1; j < members.size(); j++) {
				double tmpDist = members.get(i).getPosition().distance(members.get(j).getPosition());
				maxDist = tmpDist > maxDist ? tmpDist : maxDist;
			}
		}
		return maxDist;
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

	/**
	 * Calculates the distance of the given pedestrian to the centroid of the group based
	 * on the sum of the potential fields of all other group members.
	 *
	 * @param ped Pedestrian
	 * @return Distance to group centroid
	 */
	public double getRelativeDistanceCentroid(Pedestrian ped) {
		double result = 0.0;
		VPoint pedLocation = ped.getPosition();

		double potentialSum = 0.0;
		int size = 0;
		for (Pedestrian p : members) {
			if (!ped.equals(p) && !isLostMember(p)) {
				potentialSum += potentialFieldTarget.getPotential(p.getPosition(), p);
				size++;
			}
		}

		double pedDistance = potentialFieldTarget.getPotential(pedLocation, ped);

		if (size != 0) {

			result = (potentialSum / size) - pedDistance;

			if (result > POTENTIAL_DISTANCE_THRESHOLD) {
				result = 0;
			}
		}

		return result;
	}

	public Pedestrian getPacemaker(Pedestrian ped) {
		Pedestrian pacemaker = members.get(0);

		double smallestDistance = potentialFieldTarget.getPotential(pacemaker.getPosition(), ped);

		for (Pedestrian p : members) {
			double pedDistance = potentialFieldTarget.getPotential(p.getPosition(), p);
			if (pedDistance < smallestDistance) {
				pacemaker = p;
				smallestDistance = pedDistance;
			}
		}

		if (ped.getId() == pacemaker.getId() || isLostMember(ped)) {
			pacemaker = null;
		}

		return pacemaker;
	}

	@Override
	public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
		this.potentialFieldTarget = potentialFieldTarget;
	}

	@Override
	public IPotentialFieldTarget getPotentialFieldTarget() {
		return potentialFieldTarget;
	}
}
