package org.vadere.util.potential;

/**
 * Enumeration whose values are used as node flags in pathfinding algorithms.
 */
public enum PathFindingTag {
	Undefined(true, false), NARROW(true, false), Reachable(true, false), Reached(true, true), Target(
			true, true), Obstacle(false, false), Margin(false, false);

	public final boolean accessible;
	public final boolean frozen;

	PathFindingTag(boolean accessible, boolean frozen) {
		this.accessible = accessible;
		this.frozen = frozen;
	}

	PathFindingTag() {
		this(true, false);
	}
}
