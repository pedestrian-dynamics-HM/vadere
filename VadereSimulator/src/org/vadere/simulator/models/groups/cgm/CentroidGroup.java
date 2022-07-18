package org.vadere.simulator.models.groups.cgm;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.GrahamScan;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.logging.Logger;

public class CentroidGroup implements Group {

    // Static Variables
    private static final Logger log = Logger.getLogger(CentroidGroup.class);
    public final static int POTENTIAL_DISTANCE_THRESHOLD = 1000;
    public final static int MAX_NO_VISION_OF_LEADER = 25;

    final private int id;
    final private int size;

    final protected ArrayList<Pedestrian> members;

    private double groupVelocity;

    private final LinkedList<Pedestrian> lostMembers;
    private int reevaluateAllMembersCountdown;

    private Map<Pedestrian, Map<Pedestrian, VPoint>> lastVision;
    private final Map<Pedestrian, Integer> noVisionOfLeaderCount;
    private IPotentialFieldTarget potentialFieldTarget;
    private final CentroidGroupModel model;

    public CentroidGroup(int id, int size,
                         CentroidGroupModel model) {
        this.id = id;
        this.size = size;
        this.model = model;
        this.potentialFieldTarget = model.getPotentialFieldTarget();
        this.members = new ArrayList<>();

        this.lastVision = new HashMap<>();
        this.lostMembers = new LinkedList<>();
        this.reevaluateAllMembersCountdown = 0;
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
        if (o == null) {
            return result;
        }

        if (this == o) {
            result = true;
        } else {

            if (this.getID() == o.getID()) {
                result = true;
            }
        }

        return result;
    }

    @Override
    public List<Pedestrian> getMembers() {
        return members;
    }

    public Stream<Pedestrian> memberStream() {
        return members.stream();
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isMember(Pedestrian ped) {
        return members.contains(ped);
    }

    public Optional<Pedestrian> getMember(int pedId) {
        return members.stream()
                .filter(ped -> ped.getId() == pedId)
                .findAny();
    }

    @Override
    public boolean isFull() {
        boolean result = true;

        if ((members.size() + lostMembers.size()) < size) {
            result = false;
        }

        return result;
    }

    @Override
    public int getOpenPersons() {
        return size - members.size();
    }

    public VPoint getCentroid(boolean with_lost) {

		/*double sumx = 0.0;
		double sumy = 0.0;

		int size = members.size();

		for (Pedestrian p : members) {
			sumx += p.getPosition().getX();
			sumy += p.getPosition().getY();
		}

		double[] result = {sumx / size, sumy / size};

		return new VPoint(result[0], result[1]);*/

        if (members.size() >= 3 && (with_lost || members.size() - lostMembers.size() >= 3)) {
            List<VPoint> pointsOfGroup = new ArrayList<>();
            for (Pedestrian ped : members) {
                if (!isLostMember(ped) || with_lost) {
                    pointsOfGroup.add(ped.getPosition());
                }
            }
            VPolygon convexPolygon = new GrahamScan(pointsOfGroup).getPolytope();
            VPoint centroid = convexPolygon.getCentroid();
            return centroid;
        } else {
            throw new IllegalArgumentException("cannot compute centroid with <= 2 points");
        }
    }

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
            result[1] = sumy / size;
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

        lastVision.put(ped, new HashMap<>());
        noVisionOfLeaderCount.put(ped, 0);
        members.add(ped);
        initGroupVelocity(); // ensure same speed for all members.

    }

    @Override
    public boolean removeMember(Pedestrian ped) {
        members.remove(ped);
        lastVision.remove(ped);
        lostMembers.remove(ped);
        noVisionOfLeaderCount.remove(ped);
        return (members.size() == 0);
    }

    private void initGroupVelocity() {
        double minVelocity = getMembers().stream().map(Agent::getFreeFlowSpeed).min(Double::compareTo).orElseThrow();
        groupVelocity = minVelocity;
        getMembers().forEach(m -> m.setFreeFlowSpeed(minVelocity));
    }

    public double getGroupVelocity() {
        return groupVelocity;
    }

    private int getPairCount() {
        return (members.size() * members.size() - members.size()) / 2;
    }

    public ArrayList<PedestrianPair> getMemberPairs() {
        ArrayList<PedestrianPair> ret = new ArrayList<>(getPairCount());
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                Pedestrian m1 = members.get(i);
                Pedestrian m2 = members.get(j);
                ret.add(PedestrianPair.of(m1, m2));
            }
        }
        return ret;
    }

    public ArrayList<Pair<PedestrianPair, Double>> getEuclidDist() {
        ArrayList<Pair<PedestrianPair, Double>> ret = new ArrayList<>(getPairCount());

        for (PedestrianPair p : getMemberPairs()) {
            double dist = p.getLeft().getPosition().distance(p.getRight().getPosition());
            ret.add(Pair.of(p, dist));
        }
        return ret;
    }

    public boolean isCentroidWithinObstacle() {
        if (members.size() - lostMembers.size() <= 2) {
            return getPairIntersectObstacle()
                    .stream()
                    .filter(pair -> !isLostMember(pair.getKey().getLeft()) && !isLostMember(pair.getKey().getRight()))
                    .map(pedestrianPairBooleanPair -> pedestrianPairBooleanPair.getValue())
                    .anyMatch(val -> val);
        } else {
            VPoint centroid = getCentroid(false);
            return model.getTopography().getObstacles()
                    .stream()
                    .map(Obstacle::getShape)
                    .anyMatch(s -> s.contains(centroid));
        }
    }


    public ArrayList<Pair<PedestrianPair, Boolean>> getPairIntersectObstacle() {
        ArrayList<Pair<PedestrianPair, Boolean>> ret = new ArrayList<>(getPairCount());

        for (PedestrianPair p : getMemberPairs()) {
            VLine pedLine = new VLine(p.getLeft().getPosition(), p.getRight().getPosition());
            boolean intersectsObs = model.getTopography().getObstacles()
                    .stream()
                    .map(Obstacle::getShape)
                    .anyMatch(s -> s.intersects(pedLine));
            ret.add(Pair.of(p, intersectsObs));
        }
        return ret;
    }

    public ArrayList<Pair<PedestrianPair, Double>> getPotentialDist() {
        ArrayList<Pair<PedestrianPair, Double>> ret = new ArrayList<>(getPairCount());

        for (PedestrianPair p : getMemberPairs()) {
            double potential1 = potentialFieldTarget.getPotential(p.getLeftPosition(), p.getLeft());
            double potential2 = potentialFieldTarget.getPotential(p.getRightPosition(), p.getRight());
            double potentialDiff = Math.abs(potential1 - potential2);
            ret.add(Pair.of(p, potentialDiff));
        }
        return ret;
    }

    private Map<Pedestrian, Double> getAveragePotentialDiff() {
        ArrayList<Pair<PedestrianPair, Double>> potentialDiffs = getPotentialDist();
        HashMap<Pedestrian, Double> potentialDiffOthers = new HashMap<>();
        for (Pedestrian member : members) {
            double averageDiff = potentialDiffs.stream()
                    .filter(pair -> pair.getKey().getLeftId() == member.getId())
                    .map(pair -> pair.getRight())
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);
            potentialDiffOthers.put(member, averageDiff);
        }
        return potentialDiffOthers;
    }

    public void reevaluateLostMember(Pedestrian ped) {
        if (isGroupTarget(ped.getNextTargetId())) {
            wakeFromLostMember(ped);
            // take 7 (instead of 8) to reinsert lost member in members to avoid fluctuations
            if (Math.abs(getRelativeDistanceCentroid(ped, false, true)) > 7) {
                setLostMember(ped);
            }
        }
        reevaluateAllMembersCountdown--;
        if (reevaluateAllMembersCountdown <= 0) {
            reevaluateAll();
        }
    }

    private void reevaluateAll() {
        if (this.lostMembers.size() > this.members.size() / 2) {
            List<Pedestrian> membersMostCommonTarget = getMembersMostCommonTarget();
            if (membersMostCommonTarget.size() > this.members.size() / 2) {

                Map<Boolean, List<Pedestrian>> partitions = membersMostCommonTarget.stream()
                        .collect(Collectors.partitioningBy(p -> Math.abs(getRelativeDistanceCentroid(p,
                                true, false)) < 8));
                if (partitions.get(true).size() > lostMembers.size()) {
                    lostMembers.clear();
                    lostMembers.addAll(partitions.get(false));
                    lostMembers.addAll(members.stream()
                            .filter(m -> !membersMostCommonTarget.contains(m)).collect(Collectors.toList()));
                }
            }
            reevaluateAllMembersCountdown = (lostMembers.size()) * 10;
        }
    }

    private List<Pedestrian> getMembersMostCommonTarget() {
        List<Integer> targetIds = members.stream().map(m -> m.getNextTargetId()).collect(Collectors.toList());
        int mostCommonTarget = -1;
        for (int id : new HashSet<>(targetIds)) {
            int frequencyTarget = Collections.frequency(targetIds, id);
            if (frequencyTarget > mostCommonTarget) {
                mostCommonTarget = frequencyTarget;
            }
        }
        int finalMostCommonTarget = mostCommonTarget;
        return members.stream()
                .filter(member -> member.getNextTargetId() == finalMostCommonTarget)
                .collect(Collectors.toList());
    }

    public boolean isGroupTarget(int targetId) {
        Optional<Pedestrian> ped = members.stream()
                .filter(memb -> !isLostMember(memb))
                .findAny();
        return ped.map(pedestrian -> pedestrian.getNextTargetId() == targetId).orElse(true);
    }

    /**
     * The method returns the maximal euclidean distance to another group member.
     * The other group member is the Key (left element) of the Pair.
     *
     * @param ped Pedestrian to which the distances is measured.
     * @return Returns a Pair (Ped, Double)
     */
    public Pair<Pedestrian, Double> getMaxDistPedIdInGroup(Pedestrian ped) {
        Pair<PedestrianPair, Double> maxDist = getEuclidDist().stream()
                // only compare member-Pairs in which the ped is a part of. Either left or right
                .filter(pair -> pair.getKey().getLeft().equals(ped) || pair.getKey().getRight().equals(ped))
                // return the pair with the max distance
                .max(Map.Entry.comparingByValue()).orElse(null);

        if (maxDist == null) {
            return Pair.of(ped, 0.0);
        } else if (maxDist.getKey().getLeft().equals(ped)) {
            return Pair.of(maxDist.getKey().getRight(), maxDist.getValue());
        } else {
            return Pair.of(maxDist.getKey().getLeft(), maxDist.getValue());
        }

    }

    public LinkedList<Pedestrian> getLostMembers() {
        return lostMembers;
    }

    void setLastVision(Pedestrian ped, Pedestrian p) {
        lastVision.get(ped).put(p, p.getPosition());
    }

    VPoint getLastVision(Pedestrian ped, Pedestrian leader) {
        return lastVision.get(ped).get(leader);
    }

    boolean isLostMember(Pedestrian p) {
        if (!lostMembers.isEmpty()) {
            List<Pedestrian> l = lostMembers;
        }
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
    public double getRelativeDistanceCentroid(Pedestrian ped, boolean includeLostMembers, boolean useGroupTarget) {
        double result = 0.0;
        VPoint pedLocation = ped.getPosition();

        double potentialSum = 0.0;
        int size = 0;
        List<Pedestrian> membersSameTarget;
        if (lostMembers.size() == 0) {
            membersSameTarget = members;
        } else if (useGroupTarget) {
            if (!isGroupTarget(ped.getNextTargetId())) {
                // cannot compute relative of ped to others
                log.error("cannot compute relative distance centroid with different targets");
                return 0.0;
            }
            membersSameTarget = members.stream()
                    .filter(member -> isGroupTarget(member.getNextTargetId()))
                    .collect(Collectors.toList());
        } else {
            // include all members with same target as ped (used for reevaluation)
            membersSameTarget = members.stream()
                    .filter(member -> member.getNextTargetId() == ped.getNextTargetId())
                    .collect(Collectors.toList());
        }

        for (Pedestrian p : membersSameTarget) {
            if (!ped.equals(p)) {
                if (!isLostMember(p) || includeLostMembers) {
                    potentialSum += potentialFieldTarget.getPotential(p.getPosition(), p);
                    size++;
                }
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

    public void setGroupTargetList(LinkedList<Integer> targetIds, int agentId) {
        for (Pedestrian p : this.getMembers()) {
            if (p.getId() != agentId && !isLostMember(p)) {
                p.setTargets(targetIds);
                p.setIsCurrentTargetAnAgent(false);
                p.setNextTargetListIndex(0);
            }
        }
        if (this.members.stream().filter(ped -> !isLostMember(ped)).map(ped -> ped.getNextTargetId()).collect(Collectors.toSet()).size() != 1) {
            log.error("Group " + this.getID() + ": all groups members should have same target after targetchange but did not.");
        }
    }

    /**
     * if the targets of one group member are changed, also changed the targets of the other group members.
     *
     * @param targetIds
     */
    @Override
    public void agentTargetsChanged(LinkedList<Integer> targetIds, int agentId) {
//        for (Pedestrian member: members) {  // TODO Unit Tests need to be adjusted
//            if (member.getId() != agentId && !member.getTargets().equals(targetIds)) {
//                member.setTargets(targetIds);
//            }
//        }
    }

    /**
     * if one group member has reached its target and is assigned the next target, also assign the next target for the
     * other members. Important for speed adjusting and TargetPotentials which do only work, if targets are shared.
     *
     * @param nextSpeed
     */
    @Override
    public void agentNextTargetSet(double nextSpeed, int agentId) {
        Optional<Pedestrian> current = getMember(agentId);
        if (current.isPresent()) {
            if (!isLostMember(current.get())) {
                for (Pedestrian p : this.getMembers()) {
                    if (p.getId() != agentId && !isLostMember(p)) {
                        if (p.getNextTargetListIndex() < (p.getTargets().size() - 1)) {
                            p.incrementNextTargetListIndex();
                        }
                        if (nextSpeed >= 0) {
                            p.setFreeFlowSpeed(nextSpeed);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void agentElementEncountered(ScenarioElement element, int agentId) {
        //if one group member encountered a target changer the other group members already
        // get their target changed in setGroupTarget()
        if (element instanceof TargetChanger) {

            //to avoid recursive calls, consider only first element encounter
            boolean eventPending = this.getMembers().stream()
                    .filter(p -> !isLostMember(p))
                    .filter(p -> p.getElementsEncountered(TargetChanger.class).contains(element.getId()))
                    .count() == 1;

            if (eventPending) {
                Optional<Pedestrian> optCurrent = getMember(agentId);
                if (optCurrent.isPresent()) {
                    Pedestrian current = optCurrent.get();
                    if (!isLostMember(current)) {
                        setGroupTargetList(current.getTargets(), agentId);
                        for (Pedestrian p : this.getMembers()) {
                            if (p.getId() != agentId && !isLostMember(p)) {
                                p.elementEncountered(TargetChanger.class, (TargetChanger) element);
                            }
                        }
                    }
                }
            }

        }
    }
}
