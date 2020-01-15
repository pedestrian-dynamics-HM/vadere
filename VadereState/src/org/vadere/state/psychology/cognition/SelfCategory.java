package org.vadere.state.psychology.cognition;

/**
 * According to the self-categorization theory ("reicher-2010"), people define
 * themselves as member of a social category. Often, people act collectively
 * when being in the same category. E.g., protesters - which define themselves
 * as protesers - walk together during a demonstration.
 *
 * Our agents can use these categorizations to to derive a specific behavior.
 * E.g., if an agents is "COOPERATIVE", the pedestrian swaps places
 * with other "COOPERATIVE" pedestrians.
 *
 * Watch out: The self category of an agent can change during a simulation.
 */
public enum SelfCategory {
    TARGET_ORIENTED,
    COOPERATIVE,
    INSIDE_THREAT_AREA,
    OUTSIDE_THREAT_AREA,
    WAIT,
    CHANGE_TARGET
}
