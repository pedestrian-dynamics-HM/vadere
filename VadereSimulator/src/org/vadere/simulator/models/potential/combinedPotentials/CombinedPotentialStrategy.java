package org.vadere.simulator.models.potential.combinedPotentials;

/**
 * All available potential strategies.
 *
 * Use these enum values to create objects implementing {@link org.vadere.simulator.models.potential.combinedPotentials.ICombinedPotentialStrategy}
 * by using the factory pattern.
 */
public enum CombinedPotentialStrategy {
    TARGET_ATTRACTION_STRATEGY,
    TARGET_REPULSION_STRATEGY,
    PEDESTRIAN_REPULSION_STRATEGY,
}
