package org.vadere.state.psychology.cognition;

import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class UnsupportedSelfCategoryExceptionTest {

    private List<Pedestrian> createPedestrians(int totalPedestrians) {
        List<Pedestrian> pedestrians = new ArrayList<>();

        long seed = 0;
        Random random = new Random(seed);

        for (int i = 0; i < totalPedestrians; i++) {
            AttributesAgent attributesAgent = new AttributesAgent(i);
            Pedestrian pedestrian = new Pedestrian(attributesAgent, random);
            pedestrian.setPosition(new VPoint(i, i));

            pedestrians.add(pedestrian);
        }

        return pedestrians;
    }

    @Test
    public void throwIfPedestriansNotTargetOrientiedThrowsNoExceptionIfTargetOriented() {
        SelfCategory expectedSelfCategory = SelfCategory.TARGET_ORIENTED;

        List<Pedestrian> pedestrians = createPedestrians(2);
        pedestrians.stream().forEach(pedestrian -> pedestrian.setSelfCategory(expectedSelfCategory));

        UnsupportedSelfCategoryException.throwIfPedestriansNotTargetOrientied(pedestrians, this.getClass());

        pedestrians.stream().forEach(pedestrian -> assertEquals(expectedSelfCategory, pedestrian.getSelfCategory()));
    }

    @Test
    public void throwIfPedestriansNotTargetOrientiedThrowsExceptionIfNotTargetOriented() {
        SelfCategory[] selfCategories = SelfCategory.values();

        int actualTotalExceptions = 0;
        int expectedTotalExceptions = selfCategories.length - 1;

        for (int i = 0; i < selfCategories.length; i++) {
            SelfCategory currentCategory = selfCategories[i];

            List<Pedestrian> pedestrians = createPedestrians(2);
            pedestrians.stream().forEach(pedestrian -> pedestrian.setSelfCategory(currentCategory));

            try {
                UnsupportedSelfCategoryException.throwIfPedestriansNotTargetOrientied(pedestrians, this.getClass());
            } catch (UnsupportedSelfCategoryException exception) {
                actualTotalExceptions++;
            }
        }

        assertEquals(expectedTotalExceptions, actualTotalExceptions);
    }

    @Test(expected = UnsupportedSelfCategoryException.class)
    public void throwIfPedestriansNotTargetOrientiedThrowsExceptionIfAtLeastOneNotTargetOriented() {
        List<Pedestrian> pedestrians = createPedestrians(2);
        pedestrians.get(0).setSelfCategory(SelfCategory.TARGET_ORIENTED);
        pedestrians.get(1).setSelfCategory(SelfCategory.COOPERATIVE);

        UnsupportedSelfCategoryException.throwIfPedestriansNotTargetOrientied(pedestrians, this.getClass());
    }

}
