package org.vadere.state.attributes.scenario;

import org.vadere.state.scenario.Stairs;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

/**
 * Attributes of {@link Stairs} objects.
 * The attributes "treads" specifies how many treads the stair should be comprised of, counting only
 * the small platforms that are NOT on the ground or on the next floor.
 * Upward direction specifies in which direction the treads should head toward. This way a polygon
 * can also be used.
 * Upward direction must not be normalized (it will be normalized to 1.0 in the constructor).
 * 
 *
 */
public class AttributesStairs extends AttributesVisualElement {

	private Integer treadCount = 1;
	private Vector2D upwardDirection = new Vector2D(1.0, 0.0);

	public AttributesStairs() { }

	public AttributesStairs(int id) {
		this.id = id;
		this.treadCount = 1;
		upwardDirection = new Vector2D(1.0, 0.0);
	}

	public AttributesStairs(int id, VShape shape, int treadCount, Vector2D upwardDirection) {
		this(id);
		this.shape = shape;
		this.treadCount = Math.max(1, treadCount);
		this.upwardDirection = upwardDirection.normalize(1.0);

		if (treadCount < 1) {
			Logger.getLogger(getClass()).error("Tread count too small (" + treadCount + "). Setting it to one.");
		}
	}
	public int getTreadCount() {
		return treadCount;
	}

	public Vector2D getUpwardDirection() {
		return upwardDirection;
	}

}
