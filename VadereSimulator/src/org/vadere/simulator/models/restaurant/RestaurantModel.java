package org.vadere.simulator.models.restaurant;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.distributions.AttributesConstantDistribution;
import org.vadere.state.attributes.models.restaurant.AttributesRestaurantModel;
import org.vadere.state.attributes.models.restaurant.AttributesSeatGroup;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ModelClass
public class RestaurantModel implements Model {

    /**
     * Distance from a table, where pedestrians can occupy seats belonging to the associated seating group
     */
    private final static int TABLE_REACH_DISTANCE = 2;

    /**
     * Maximum waiting time a pedestrian would stay at a seating group
     */
    private final static double MAX_WAITING_TIME = 18000;

    private static Logger logger = Logger.getLogger(RestaurantModel.class);

    private Topography topography;

    private AttributesRestaurantModel attrRestaurantModel;

    /**
     * Seating groups that can be accessed by the id of the table (target)
     */
    private Map<Integer, SeatGroup> seatGroupMap;

    /**
     * Memory for the assignments at which table a pedestrian get sat
     */
    private Map<Pedestrian, Integer> pedestrianSeatGroupMap;

    /**
     * Initialize the Restaurant Model with all associated seating groups
     */
    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.attrRestaurantModel = Model.findAttributes(attributesList, AttributesRestaurantModel.class);
        this.topography = domain.getTopography();
        this.seatGroupMap = new HashMap<>();
        this.pedestrianSeatGroupMap = new ConcurrentHashMap<>();

        // initialize seating groups
        for (AttributesSeatGroup attrSeatGroup : this.attrRestaurantModel.getAttrsSeatGroup()) {
            if (attrSeatGroup.getTableTargetId() != AttributesRestaurantModel.INVALID_ID) {
                initializeSeatGroup(attrSeatGroup);
            }
        }
    }

    /**
     * Initialize a seating group
     * @param attrSeatGroup attributes of the seating group
     */
    public void initializeSeatGroup(AttributesSeatGroup attrSeatGroup) {
        List<Target> seatGroupTables = this.topography.getTargets(attrSeatGroup.getTableTargetId());
        if (seatGroupTables.size() != 1) {
            throw new IllegalStateException("Improper number of targets for a given target ID.");
        }
        Target tableTarget = seatGroupTables.get(0);
        SeatGroup seatGroup = new SeatGroup(tableTarget, attrSeatGroup.getLengthOfStay());

        tableTarget.getAttributes().getAbsorberAttributes().setEnabled(false);
        tableTarget.getAttributes().getWaiterAttributes().setEnabled(true);
        tableTarget.getAttributes().getWaiterAttributes().setIndividualWaiting(false);
        tableTarget.getAttributes().setParallelEvents(0);
        tableTarget.getAttributes().getWaiterAttributes().setDistribution(new AttributesConstantDistribution(0));

        //add seats to seating group
        for (int seatId: attrSeatGroup.getSeatTargetIds()) {
            List<Target> seats = this.topography.getTargets(seatId);
            if (seats.size() != 1) {
                throw new IllegalStateException("Improper number of targets for a given target ID.");
            }
            Target seatTarget = seats.get(0);

            // adjust the settings of the seats (targets), so that they do not remove arriving pedestrians and let them wait
            seatTarget.getAttributes().getAbsorberAttributes().setEnabled(false);
            seatTarget.getAttributes().getWaiterAttributes().setEnabled(true);
            seatTarget.getAttributes().getWaiterAttributes().setIndividualWaiting(true);
            seatTarget.getAttributes().setParallelEvents(1);
            seatTarget.getAttributes().getWaiterAttributes().setDistribution(new AttributesConstantDistribution(MAX_WAITING_TIME));

            seatGroup.addSeat(seatId);

            // add listener that gets notified when a pedestrian sits down on the seat
            seatTarget.addListener(seatTargetListener);
        }
        this.seatGroupMap.put(attrSeatGroup.getTableTargetId(), seatGroup);
    }


    @Override
    public void preLoop(double simTimeInSec) {
        // check if source only has table as target?!
    }

    @Override
    public void postLoop(double simTimeInSec) {}

    /**
     * Update model by assigning arriving pedestrians to seats and removing departing pedestrians
     * @param simTimeInSec current simulation time
     */
    @Override
    public void update(double simTimeInSec) {
        for (SeatGroup seatGroup : seatGroupMap.values()) {
            Collection<List<Pedestrian>> leavingGroups = seatGroup.leaveSeatsForExpiredGroups(simTimeInSec);
            // leaving pedestrians have to stand up
            leavingGroups.forEach(group -> group.forEach(pedestrian -> {
                pedestrian.setSitting(false);
                pedestrianSeatGroupMap.remove(pedestrian);
            }));
        }
        chooseSeatsForArrivingPedestrians(simTimeInSec);
    }

    /**
     * Occupy seats for arriving groups or pedestrians.
     * @param simTime current simulation time
     */
    private void chooseSeatsForArrivingPedestrians(double simTime) {
        Collection<Pedestrian> arrivingPedestrians = topography.getPedestrianDynamicElements().getElements()
                .stream()
                .filter(p -> this.attrRestaurantModel.getTableTargetIds().contains(p.getNextTargetId()))
                .collect(Collectors.toSet());

        Map<LinkedList<Integer>, List<Pedestrian>> arrivingGroups = arrivingPedestrians.stream()
                .filter(ped-> !ped.getGroupIds().isEmpty())
                .collect(Collectors.groupingBy(Pedestrian::getGroupIds));
        List<Pedestrian> arrivingSinglePedestrians = arrivingPedestrians.stream()
                .filter(ped-> ped.getGroupIds().isEmpty()).toList();

        // try to occupy seats for a whole group
        for (Map.Entry<LinkedList<Integer>, List<Pedestrian>> groupEntry : arrivingGroups.entrySet()) {
            List<Pedestrian> pedestrians = groupEntry.getValue();
            int tableId = pedestrians.get(0).getNextTargetId();
            SeatGroup seatGroup = seatGroupMap.get(tableId);
            if (pedestrians.size() > seatGroup.getSeatGroupSize()) {
                for (Pedestrian pedestrian : pedestrians) {
                    LinkedList<Integer> targets = pedestrian.getTargets();
                    targets.pop();
                    pedestrian.setTargets(targets);
                }
            } else if (pedestrians.stream().anyMatch(ped -> seatGroup.getTable().getShape().distance(ped.getPosition()) < TABLE_REACH_DISTANCE)) {
                if (seatGroup.tryAssignPedestriansToFreeSeats(pedestrians, simTime)) {
                    pedestrians.forEach(ped -> pedestrianSeatGroupMap.put(ped, tableId));
                }
            }
        }

        // try to occupy seats for a single pedestrian
        for (Pedestrian pedestrian : arrivingSinglePedestrians) {
            int tableId = pedestrian.getNextTargetId();
            SeatGroup seatGroup = seatGroupMap.get(tableId);
            if (seatGroup.getTable().getShape().distance(pedestrian.getPosition()) < TABLE_REACH_DISTANCE) {
                if (seatGroup.tryAssignPedestriansToFreeSeats(List.of(pedestrian), simTime)) {
                    pedestrianSeatGroupMap.put(pedestrian, tableId);
                }
            }
        }
    }

    /**
     * Listener for the seats belonging to a seating group
     */
    private final TargetListener seatTargetListener = new TargetListener() {
        /**
         * Arriving pedestrians at a seat get sat down and their looking directions get adjusted to the middle of the table
         * @param target seat
         * @param agent pedestrian
         */
        @Override
        public void reachedTarget(Target target, Agent agent) {
            Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElement(agent.getId());
            if (pedestrian == null || pedestrian.isSitting()) {
                return;
            }
            if (!pedestrianSeatGroupMap.containsKey(pedestrian)) {
                return;
            }
            Target table = seatGroupMap.get(pedestrianSeatGroupMap.get(pedestrian)).getTable();
            if (table == null) {
                return;
            }
            pedestrian.setSitting(true);
            double sittingDirectionX = table.getShape().getCentroid().getX() - pedestrian.getPosition().getX();
            double sittingDirectionY = table.getShape().getCentroid().getY() - pedestrian.getPosition().getY();

            Vector2D sittingDirection = new Vector2D(sittingDirectionX, sittingDirectionY);
            pedestrian.setSittingDirection(sittingDirection);
        }
    };

    public TargetListener getSeatTargetListener() {
        return seatTargetListener;
    }

    public List<SeatGroup> getSeatGroups() {
        return new ArrayList<>(seatGroupMap.values());
    }
}
