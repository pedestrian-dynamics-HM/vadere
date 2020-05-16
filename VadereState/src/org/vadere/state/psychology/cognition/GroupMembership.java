package org.vadere.state.psychology.cognition;

import java.awt.*;

/**
 * According to the self-categorization theory ("reicher-2010"), people define
 * themselves as a member of social categories (see{@link SelfCategory}). When
 * people share the same {@link SelfCategory}, they often act collectively
 * if they feel as in-group member to this category.
 *
 * For instance, two protesters - which define themselves as in-group members
 * of the category protesters - walk together during a demonstration.
 *
 * This enum shall capture this in- and out-group membership. The membership
 * can be interpreted as a tree:
 *
 * <pre>
 *       Membership
 *         /\
 *        /  \
 *       /    \
 *     In      Out
 *           /  |  \
 *          /   |   \
 *         /    |    \
 * Friendly  Neutral Hostile
 * </pre>
 *
 * Following outcome may be observed when two people with a given membership
 * come together:
 * <ul>
 *     <li>IN_GROUP / IN_GROUP => imitate behavior</li>
 *     <li>IN_GROUP / OUT_GROUP_FRIENDLY => imitate behavior</li>
 *     <li>IN_GROUP / OUT_GROUP_NEUTRAL => ignore behavior of IN_GROUP member</li>
 *     <li>IN_GROUP / OUT_GROUP_HOSTILE => react hostile to IN_GROUP member</li>
 * </ul>
 *
 * Note: The value "OUT_GROUP" exists for convenience if you do not like to handle
 * the subcategories FRIENDLY, NEUTRAL and HOSTILE. "OUT_GROUP" should be handled
 * like "OUT_GROUP_NEUTRAL". I.e., IN_GROUP / OUT_GROUP => ignore behavior of
 * IN_GROUP member.
 */
public enum GroupMembership {
    IN_GROUP,
    OUT_GROUP,
    OUT_GROUP_FRIENDLY,
    OUT_GROUP_NEUTRAL,
    OUT_GROUP_HOSTILE
}
