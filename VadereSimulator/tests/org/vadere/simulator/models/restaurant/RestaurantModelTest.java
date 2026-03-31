package org.vadere.simulator.models.restaurant;

import static  org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.restaurant.AttributesRestaurantModel;
import org.vadere.state.attributes.models.restaurant.AttributesSeatGroup;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;

class RestaurantModelTest {

    int TABLE_ID = 0;
    int LEAF_ID = 10;
    double STAY_TIME = 10;

    List<Attributes> attributesList;
    RestaurantModel restaurantModel;
    Topography topography;
    VadereContext ctx;
    Random rdm;
    double simStartTime;

    @BeforeEach
    public void setUp() {
        attributesList = new ArrayList<>();
        restaurantModel = new RestaurantModel();
        topography = new Topography(new AttributesTopography(), null);
        topography.setContextId("RestaurantTestId");
        rdm = new Random(0);
        ctx = new VadereContext();
        VadereContext.add(topography.getContextId(), ctx);
        simStartTime = 0.0;
    }

    private void buildAttributesList() {
        ArrayList<Integer> seats = new ArrayList<>();
        seats.add(1);
        seats.add(2);
        AttributesSeatGroup attributesSeatGroup = new AttributesSeatGroup(TABLE_ID, seats, STAY_TIME);
        ArrayList<AttributesSeatGroup> attributesSeatGroups = new ArrayList<>();
        attributesSeatGroups.add(attributesSeatGroup);
        attributesList.add(new AttributesRestaurantModel(attributesSeatGroups));
    }

    private void addTableToTopography() {
        topography.addTarget(new Target(new AttributesTarget(new VCircle(5, 5,1), TABLE_ID)));
    }

    private void addSeatsToTopography() {
        topography.addTarget(new Target(new AttributesTarget(new VCircle(5,4, 1), 1)));
        topography.addTarget(new Target(new AttributesTarget(new VCircle(5, 6, 1), 2)));
    }

    private void initializeModel() {
        restaurantModel.initialize(attributesList, new Domain(topography), null, rdm);
    }

    private void buildSetUp(boolean table, boolean seats, boolean initialize) {
        buildAttributesList();
        if (table) {
            addTableToTopography();
        }
        if (seats) {
            addSeatsToTopography();
        }
        if (initialize) {
            initializeModel();
        }
    }

    @Test
    public void testInitializeWithMissingTableTarget() {
        buildSetUp(false, true, false);
        assertThrows(IllegalStateException.class, this::initializeModel);
    }

    @Test
    public void testInitializeWithMissingSeatTarget() {
        buildSetUp(true, false, false);
        assertThrows(IllegalStateException.class, this::initializeModel);
    }

    @Test
    public void testInitialize() {
        buildSetUp(true, true, true);
        List<SeatGroup> seatGroups = restaurantModel.getSeatGroups();
        assertEquals(1, seatGroups.size());
        assertEquals(2, seatGroups.get(0).getFreeSeatsTargetIds().size());
    }

    @Test
    public void testUpdatePedestrianOccupiesSeatSitsAndLeafs() {
        buildSetUp(true, true, true);

        Pedestrian ped = addPedestrian(0, new VPoint(0, 0), -1);

        SeatGroup seatGroup = restaurantModel.getSeatGroups().get(0);
        assertEquals(2, seatGroup.getFreeSeatsTargetIds().size());

        movePedestrianToTable(ped);
        restaurantModel.update(simStartTime);

        assertEquals(1, seatGroup.getFreeSeatsTargetIds().size());

        movePedestrianToSeat(ped);

        assertTrue(ped.isSitting());

        restaurantModel.update(simStartTime + STAY_TIME);

        assertEquals(2, seatGroup.getFreeSeatsTargetIds().size());
        assertFalse(ped.isSitting());
        assertEquals(LEAF_ID, ped.getNextTargetId());
    }

    @Test
    public void testUpdateGroupOccupiesSeatsSitsAndLeafs() {
        buildSetUp(true, true, true);

        Pedestrian ped1 = addPedestrian(0, new VPoint(0, 0), 0);
        Pedestrian ped2 = addPedestrian(1, new VPoint(0, 0), 0);

        SeatGroup seatGroup = restaurantModel.getSeatGroups().get(0);
        assertEquals(2, seatGroup.getFreeSeatsTargetIds().size());

        movePedestrianToTable(ped1);
        restaurantModel.update(simStartTime);

        assertEquals(0, seatGroup.getFreeSeatsTargetIds().size());

        movePedestrianToSeat(ped1);
        movePedestrianToSeat(ped2);

        assertTrue(ped1.isSitting());
        assertTrue(ped2.isSitting());

        restaurantModel.update(simStartTime + STAY_TIME);

        assertEquals(2, seatGroup.getFreeSeatsTargetIds().size());
        assertFalse(ped1.isSitting());
        assertFalse(ped2.isSitting());
        assertEquals(LEAF_ID, ped1.getNextTargetId());
        assertEquals(LEAF_ID, ped2.getNextTargetId());
    }

    @Test
    public void testUpdatePedestrianWaitsWhenSeatsAreNotFree() {
        buildSetUp(true, true, true);

        Pedestrian ped1 = addPedestrian(0, new VPoint(0, 0), 0);
        Pedestrian ped2 = addPedestrian(1, new VPoint(0, 0), 0);

        movePedestrianToTable(ped1);
        restaurantModel.update(simStartTime);
        movePedestrianToSeat(ped1);
        movePedestrianToSeat(ped2);

        SeatGroup seatGroup = restaurantModel.getSeatGroups().get(0);
        assertEquals(0, seatGroup.getFreeSeatsTargetIds().size());

        Pedestrian ped3 = addPedestrian(2, new VPoint(0, 0), -1);

        movePedestrianToTable(ped3);
        assertEquals(0, ped3.getNextTargetId());

        restaurantModel.update(simStartTime + STAY_TIME);

        assertEquals(LEAF_ID, ped1.getNextTargetId());
        assertEquals(LEAF_ID, ped2.getNextTargetId());
        assertEquals(1, seatGroup.getFreeSeatsTargetIds().size());
        assertNotEquals(TABLE_ID, ped3.getNextTargetId());
        assertNotEquals(LEAF_ID, ped3.getNextTargetId());
    }

    @Test
    public void testUpdateGroupLeafsWhenNotEnoughSeatsInSeatGroup() {
        buildSetUp(true, true, true);

        Pedestrian ped1 = addPedestrian(0, new VPoint(0, 0), 0);
        Pedestrian ped2 = addPedestrian(1, new VPoint(0, 0), 0);
        Pedestrian ped3 = addPedestrian(2, new VPoint(0, 0), 0);

        movePedestrianToTable(ped1);
        restaurantModel.update(simStartTime);

        SeatGroup seatGroup = restaurantModel.getSeatGroups().get(0);
        assertEquals(2, seatGroup.getFreeSeatsTargetIds().size());
        assertEquals(LEAF_ID, ped1.getNextTargetId());
        assertEquals(LEAF_ID, ped2.getNextTargetId());
        assertEquals(LEAF_ID, ped3.getNextTargetId());
    }

    private Pedestrian addPedestrian(int id, VPoint location, int groupId) {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), rdm);
        pedestrian.setPosition(location);
        pedestrian.setTargets(new LinkedList<>(List.of(TABLE_ID, LEAF_ID)));
        pedestrian.setId(id);
        LinkedList<Integer> groupIds = new LinkedList<>();
        if (groupId >= 0) {
            groupIds.add(groupId);
        }
        pedestrian.setGroupIds(groupIds);
        topography.addElement(pedestrian);
        return pedestrian;
    }

    private void movePedestrianToTable(Pedestrian pedestrian) {
        pedestrian.setPosition(topography.getTarget(0).getShape().getCentroid());
    }

    private void movePedestrianToSeat(Pedestrian pedestrian) {
        assert(pedestrian.getNextTargetId() == 1 || pedestrian.getNextTargetId() == 2);
        pedestrian.setPosition(topography.getTarget(pedestrian.getNextTargetId()).getShape().getCentroid());
        restaurantModel.getSeatTargetListener().reachedTarget(topography.getTarget(pedestrian.getNextTargetId()), pedestrian);
    }

}