package org.vadere.simulator.models.sir;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public abstract class SirModelTest {
    public abstract SirModel sirModel();

    @Test
    public void throwIfPedestrianInfectionStatusDoesNotReachExposed() {
        Topography topography = new Topography();
        createPedestrian(topography, new VPoint(1, 1), 1, 2, InfectionStatus.SUSCEPTIBLE);

        Pedestrian pedestrian = topography.getPedestrianDynamicElements().getElement(1);
        pedestrian.setPathogenAbsorbedLoad(pedestrian.getSusceptibility());
        sirModel().updatePedestrianInfectionStatus(pedestrian, 10.0);
        assertEquals(pedestrian.getInfectionStatus(), InfectionStatus.EXPOSED);
        // ToDo: This test does not work; (how) do I test Interfaces?
    }

    @Test
    public void testUpdatePedestrianPathogenAbsorbedLoad() {
    }

    private void createPedestrian(Topography topography, VPoint pedPosition, int pedId, int targetId, InfectionStatus infectionStatus) {
        Pedestrian pedestrian = new Pedestrian(new AttributesAgent(), new Random(1));
        pedestrian.setPosition(pedPosition);
        pedestrian.setInfectionStatus(infectionStatus);
        pedestrian.setId(pedId);

        LinkedList<Integer> targetsPedestrian = new LinkedList<>();
        targetsPedestrian.add(targetId);
        pedestrian.setTargets(targetsPedestrian);

        topography.addElement(pedestrian);
    }
}