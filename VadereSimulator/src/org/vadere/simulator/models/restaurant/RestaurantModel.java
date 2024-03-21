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

    private static Logger logger = Logger.getLogger(RestaurantModel.class);
    private Random random;
    private Domain domain;
    private AttributesAgent attributesAgent;
    private Topography topography;

    private AttributesRestaurantModel attrRestaurantModel;

    private LinkedList<SeatGroup> seatGroups;

    private Map<Integer, SeatGroup> seatGroupMap;

    private Map<Target, Target> seatTableMap;

    private Map<Pedestrian, Target> sittingPedestriansSeatMap;

    private final static int TABLE_REACH_DISTANCE = 2;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.attributesAgent = attributesPedestrian;
        this.attrRestaurantModel = Model.findAttributes(attributesList, AttributesRestaurantModel.class);
        this.topography = domain.getTopography();
        this.seatGroups = new LinkedList<>();
        this.seatGroupMap = new HashMap<>();
        this.seatTableMap = new HashMap<>();
        this.sittingPedestriansSeatMap = new ConcurrentHashMap<>();

        for (AttributesSeatGroup attrSeatGroup : this.attrRestaurantModel.getAttrsSeatGroup()) {
            if (attrSeatGroup.getTableTargetId() != attrRestaurantModel.INVALID_ID) {
                initializeSeatGroup(attrSeatGroup);
            }
        }
    }


    public void initializeSeatGroup(AttributesSeatGroup attrSeatGroup) {
        List<Target> seatGroupTableTargets = this.topography.getTargets(attrSeatGroup.getTableTargetId());
        if (seatGroupTableTargets.size() != 1) {
            throw new IllegalStateException("Improper number of targets for a given target ID.");
        }
        Target tableTarget = seatGroupTableTargets.get(0);
        SeatGroup seatGroup = new SeatGroup(tableTarget, attrSeatGroup.getSeatTargetIds().size());

        //add seatTargets
        for (int seatTargetId: attrSeatGroup.getSeatTargetIds()) {
            List<Target> seatTargets = this.topography.getTargets(seatTargetId);
            if (seatTargets.size() != 1) {
                throw new IllegalStateException("Improper number of targets for a given target ID.");
            }
            Target seatTarget = seatTargets.get(0);
            seatTarget.getAttributes().getWaiterAttributes().setEnabled(true);
            seatTarget.getAttributes().getWaiterAttributes().setDistribution(new AttributesConstantDistribution(attrSeatGroup.getLengthOfStay()));
            seatTarget.getAttributes().getWaiterAttributes().setIndividualWaiting(true);
            seatTarget.getAttributes().getAbsorberAttributes().getDeletionDistance();
            // TODO correct?
            seatTarget.getAttributes().setParallelEvents(1); // only one person can sit on the chair
            seatGroup.addSeatTarget(seatTarget);

            seatTarget.addListener(seatTargetListener);
            this.seatTableMap.put(seatTarget, seatGroupTableTargets.get(0));
        }
        this.seatGroups.add(seatGroup);
        this.seatGroupMap.put(attrSeatGroup.getTableTargetId(), seatGroup);
    }


    @Override
    public void preLoop(double simTimeInSec) {
        // check if source only has table as target?!
    }

    @Override
    public void postLoop(double simTimeInSec) {}

    @Override
    public void update(double simTimeInSec) {
        chooseSeatsForArrivingPedestrians();
        standUpFromSeat(simTimeInSec);
    }

    private void chooseSeatsForArrivingPedestrians() {
        // find pedestrians who are going to their "table" in the restaurant
        Collection<Pedestrian> arrivingPedestrians = topography.getPedestrianDynamicElements().getElements()
                .stream()
                .filter(p -> this.attrRestaurantModel.getTableTargetIds().contains(p.getNextTargetId()))
                .collect(Collectors.toSet());

        for (Pedestrian ped : arrivingPedestrians) {
            SeatGroup seatGroup = seatGroupMap.get(ped.getNextTargetId());
            // check if pedestrian is within a specific distance to the table
            if (seatGroup.getTableTarget().getShape().distance(ped.getPosition()) < TABLE_REACH_DISTANCE) {
                // specify the next seat
                int nextSeatTargetId = seatGroup.nextSeatTargetId();
                //ped.setIdAsTarget(nextSeatTarget.getId());
                LinkedList<Integer> targetIdsList = ped.getTargets();
                // replace next "table" target by "seat" target
                targetIdsList.set(ped.getNextTargetListIndex(), nextSeatTargetId);
                ped.setTargets(targetIdsList);
                // if no seat found -> wait
            }
        }
    }

    private void standUpFromSeat(double simTimeInSec) {
        // check who of the sitting pedestrians is not sitting anymore -> set isSitting = false and remove from list
        for (Pedestrian sittingPed : this.sittingPedestriansSeatMap.keySet()) {
            Target seatTarget = this.sittingPedestriansSeatMap.get(sittingPed);
            if (!seatTarget.getAttributes().isWaiting() ||
                    (seatTarget.getLeavingTimes().containsKey(sittingPed.getId()) &&
                            seatTarget.getLeavingTimes().get(sittingPed.getId()) <= simTimeInSec)) {
                sittingPed.setSitting(false);
                this.sittingPedestriansSeatMap.remove(sittingPed);
            }
        }
    }


    private final TargetListener seatTargetListener = new TargetListener() {
        @Override
        public void reachedTarget(Target target, Agent agent) {
            // is target a seat
            Target table = seatTableMap.get(target);
            if (table != null) {
                Pedestrian sittingPedestrian = topography.getPedestrianDynamicElements().getElement(agent.getId());
                sittingPedestrian.setSitting(true);
                double sittingDirectionX = table.getShape().getCentroid().getX() - sittingPedestrian.getPosition().getX();
                double sittingDirectionY = table.getShape().getCentroid().getY() - sittingPedestrian.getPosition().getY();
                Vector2D sittingDirection = new Vector2D(sittingDirectionX, sittingDirectionY);
                sittingPedestrian.setSittingDirection(sittingDirection);
                sittingPedestriansSeatMap.put(sittingPedestrian, target);
            }
        }
    };

}
