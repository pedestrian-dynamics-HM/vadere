package org.vadere.simulator.models.restaurant;
import java.util.*;
import java.util.stream.Collectors;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

public class SeatGroup {

    /**
     * Table belonging to the seating group
     */
    private final Target table;

    /**
     * Arrival times (time when getting seats) for a group of pedestrians
     * (that belong together according to the group model)
     */
    private Map<List<Pedestrian>, Double> arrivalTimes;

    /**
     * Ids of the seats (targets) belonging to the seating group with reference if they are free
     */
    private LinkedHashMap<Integer, Boolean> seats;

    /**
     * Time a group of pedestrians stays at the table
     */
    private final double tableTime;


    public SeatGroup (Target tableTarget, double tableTime) {
        this.table = tableTarget;
        this.seats = new LinkedHashMap<>();
        this.arrivalTimes = new HashMap<>();
        this.tableTime = tableTime;
    }

    public SeatGroup (Target table, List<Integer> seats, double tableTime) {
        this.table = table;
        this.seats = (LinkedHashMap<Integer, Boolean>) seats.stream().collect(Collectors.toMap(i -> i, i -> true));
        this.tableTime = tableTime;
    }

    /**
     * Get the table belonging to the seating group
     * @return table
     */
    public Target getTable() {
        return this.table;
    }

    /**
     * Add seat to the seating group
     * @param seatId id of the seat (target)
     */
    public void addSeat(int seatId) {
        seats.put(seatId, true);
    }

    /**
     * Requests seats for a group of pedestrians.
     * If enough free seats are available, the seats get occupied, else the request gets denied
     * @param pedestrians group of pedestrians
     * @param simTime current simulation time
     * @return true, if enough seats are available, else false
     */
    public boolean tryAssignPedestriansToFreeSeats(List<Pedestrian> pedestrians, double simTime) {
        List<Integer> freeSeats = getFreeSeatsTargetIds();
        if (freeSeats.size() < pedestrians.size()) {
            return false;
        }
        for (int i = 0; i < pedestrians.size(); i++) {
            int seat = freeSeats.get(i);
            Pedestrian pedestrian = pedestrians.get(i);
            seats.put(seat, false);

            LinkedList<Integer> targetIds = pedestrian.getTargets();
            targetIds.set(pedestrian.getNextTargetListIndex(), seat);
            Pedestrian.setGroupTarget(List.of(pedestrian), targetIds);
        }
        arrivalTimes.put(pedestrians, simTime);
        return true;
    }

    /**
     * Updates the seatGroup by removing the pedestrian groups, that stayed enough time at the table
     * @param simTime current simulation time
     * @return list of groups of pedestrians that left the table
     */
    public Collection<List<Pedestrian>> leaveSeatsForExpiredGroups(double simTime) {
        Collection<List<Pedestrian>> groups = new HashSet<>();
        for (Map.Entry<List<Pedestrian>, Double> entry : arrivalTimes.entrySet()) {
            if (entry.getValue() + tableTime <= simTime) {
                for (Pedestrian pedestrian : entry.getKey()) {
                    int seat = pedestrian.getTargets().pop();
                    seats.put(seat, true);
                }
                groups.add(entry.getKey());
            }
        }
        groups.forEach(pedestrians -> arrivalTimes.remove(pedestrians));
        return groups;
    }

    /**
     * Get all seats belonging to this seating group, that are not occupied by pedestrians
     * @return list of free seats
     */
    public List<Integer> getFreeSeatsTargetIds() {
        return seats.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get the maximum number of pedestrian that can sit at a seating group
     * @return size of the seating group
     */
    public int getSeatGroupSize() {
        return seats.size();
    }

}
