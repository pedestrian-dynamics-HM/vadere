package org.vadere.simulator.models.potential;

import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTeleporter;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

public class PedestrianRepulsionPotentialCycleTest {
    @Test
    public void testGetAgentPotentialHorizontal(){
        int numberOfPedestrians = 10;
        List<Pedestrian> pedestrians = new ArrayList<>(numberOfPedestrians);
        Random random = new Random();
        for(int i = 0; i < numberOfPedestrians; i++){
            pedestrians.add(new Pedestrian(new AttributesAgent(i), random));
            pedestrians.get(i).setPosition(new VPoint(i, 0));
        }
        int mark = 8;
        int range = 1;
        Pedestrian markedPedestrian = pedestrians.get(mark);
        int start = (mark - range) % numberOfPedestrians;
        int end = (mark + range) % numberOfPedestrians;
        List<Pedestrian> neighbours = new ArrayList<>();
        boolean isNeighbor;
        for(int i = 0; i < numberOfPedestrians; i++){
            isNeighbor = start < end && i > start && i < end;
            isNeighbor = isNeighbor || ((start > end) && (i < end || i > start));
            if(isNeighbor) {
                neighbours.add(pedestrians.get(i));
            }
        }

        Topography noTeleportTopography = mock(Topography.class);
        when(noTeleportTopography.hasTeleporter()).thenReturn(false);
        when(noTeleportTopography.getTeleporter()).thenReturn(null);

        Teleporter teleporter = new Teleporter(new AttributesTeleporter());
        teleporter.getTeleporterShift().x = -18;
        teleporter.getTeleporterShift().y = 0;
        teleporter.getTeleporterPosition().x = 20;
        teleporter.getTeleporterPosition().y = 0;
        Topography withTeleportTopography = mock(Topography.class);
        when(withTeleportTopography.hasTeleporter()).thenReturn(true);
        when(withTeleportTopography.getTeleporter()).thenReturn(teleporter);

        PotentialFieldPedestrianCompactSoftshell potentialFieldPedestrian = new PotentialFieldPedestrianCompactSoftshell();
        potentialFieldPedestrian.initialize(Arrays.asList(new AttributesPotentialCompactSoftshell()), null, null, null);
        PedestrianRepulsionPotentialCycle noTeleportAgent = new PedestrianRepulsionPotentialCycle(potentialFieldPedestrian, noTeleportTopography);
        PedestrianRepulsionPotentialCycle withTeleportAgent = new PedestrianRepulsionPotentialCycle(potentialFieldPedestrian, withTeleportTopography);
        double noTeleport = noTeleportAgent.getAgentPotential(
                markedPedestrian.getPosition(),
                markedPedestrian,
                neighbours);
        double withTeleport = withTeleportAgent.getAgentPotential(
                markedPedestrian.getPosition(),
                markedPedestrian,
                neighbours);
        assertEquals(withTeleport, noTeleport);
    }
    @Test
    public void testGetAgentPotentialVertical(){
        int numberOfPedestrians = 10;
        List<Pedestrian> pedestrians = new ArrayList<>(numberOfPedestrians);
        Random random = new Random();
        for(int i = 0; i < numberOfPedestrians; i++){
            pedestrians.add(new Pedestrian(new AttributesAgent(i), random));
            pedestrians.get(i).setPosition(new VPoint(0, i));
        }
        int mark = 8;
        int range = 1;
        Pedestrian markedPedestrian = pedestrians.get(mark);
        int start = (mark - range) % numberOfPedestrians;
        int end = (mark + range) % numberOfPedestrians;
        List<Pedestrian> neighbours = new ArrayList<>();
        boolean isNeighbor;
        for(int i = 0; i < numberOfPedestrians; i++){
            isNeighbor = start < end && i > start && i < end;
            isNeighbor = isNeighbor || ((start > end) && (i < end || i > start));
            if(isNeighbor) {
                neighbours.add(pedestrians.get(i));
            }
        }

        Topography noTeleportTopography = mock(Topography.class);
        when(noTeleportTopography.hasTeleporter()).thenReturn(false);
        when(noTeleportTopography.getTeleporter()).thenReturn(null);

        Teleporter teleporter = new Teleporter(new AttributesTeleporter());
        teleporter.getTeleporterShift().x = -18;
        teleporter.getTeleporterShift().y = 0;
        teleporter.getTeleporterPosition().x = 20;
        teleporter.getTeleporterPosition().y = 0;
        Topography withTeleportTopography = mock(Topography.class);
        when(withTeleportTopography.hasTeleporter()).thenReturn(true);
        when(withTeleportTopography.getTeleporter()).thenReturn(teleporter);

        PotentialFieldPedestrianCompactSoftshell potentialFieldPedestrian = new PotentialFieldPedestrianCompactSoftshell();
        potentialFieldPedestrian.initialize(Arrays.asList(new AttributesPotentialCompactSoftshell()), null, null, null);
        PedestrianRepulsionPotentialCycle noTeleportAgent = new PedestrianRepulsionPotentialCycle(potentialFieldPedestrian, noTeleportTopography);
        PedestrianRepulsionPotentialCycle withTeleportAgent = new PedestrianRepulsionPotentialCycle(potentialFieldPedestrian, withTeleportTopography);
        double noTeleport = noTeleportAgent.getAgentPotential(
                markedPedestrian.getPosition(),
                markedPedestrian,
                neighbours);
        double withTeleport = withTeleportAgent.getAgentPotential(
                markedPedestrian.getPosition(),
                markedPedestrian,
                neighbours);
        assertEquals(withTeleport, noTeleport);
    }
}
