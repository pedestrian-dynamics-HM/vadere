package org.vadere.state.psychology.cognition;

/**
 * According to the self-categorization theory ("reicher-2010"), people define
 * themselves as a member of social categories. People can have multiple
 * categories, e.g., father, football fan, colleague. At a specific point in
 * time, only one category is salient.
 *
 * Our agents use these categorizations to derive a specific behavior:
 * <ul>
 *     <li>
 *         TARGET_ORIENTED agents try to reach the target as quickly as possible.
 *     </li>
 *     <li>
 *         E.g., if an agents is "COOPERATIVE" (category), the pedestrian swaps places
 *         (behavior) with other "COOPERATIVE" pedestrians (only if both feel committed
 *         to this category as in-group member, see {@łink GroupMembership}.
 *         </li>
 *     <li>
 *         THREATENED agents maximize the distance to a threat.
 *     </li>
 * </ul>
 * Usually, the self category of an agent changes during a simulation.
 */
public enum SelfCategory {
    TARGET_ORIENTED,
    COOPERATIVE,
    THREATENED,
    COMMON_FATE,
    WAIT,
    CHANGE_TARGET,
    EVADE,
    SOCIAL_DISTANCING,
}
