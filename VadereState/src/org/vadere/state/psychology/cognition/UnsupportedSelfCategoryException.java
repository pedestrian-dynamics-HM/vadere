package org.vadere.state.psychology.cognition;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * Throw this exception if a (locomotion) class does not handle a specific {@link SelfCategory}.
 */
public class UnsupportedSelfCategoryException extends RuntimeException {

    public UnsupportedSelfCategoryException(@NotNull SelfCategory unsupportedSelfCategory, @NotNull Class implementingClass) {
        super(String.format("SelfCategory \"%s\" is not supported by class \"%s\"!",
                unsupportedSelfCategory,
                implementingClass.getSimpleName())
        );
    }

    public static void throwIfPedestriansNotTargetOrientied(Collection<? extends Pedestrian> pedestrians, Class caller) {
        for (Pedestrian pedestrian : pedestrians) {
            SelfCategory selfCategory = pedestrian.getSelfCategory();

            if (selfCategory != SelfCategory.TARGET_ORIENTED) {
                throw new UnsupportedSelfCategoryException(selfCategory, caller);
            }
        }
    }
}
