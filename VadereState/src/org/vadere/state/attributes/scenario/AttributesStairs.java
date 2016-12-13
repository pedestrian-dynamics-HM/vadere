package org.vadere.state.attributes.scenario;

import org.apache.log4j.Logger;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.Stairs;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VShape;

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
public class AttributesStairs extends Attributes {

	private VShape shape = null;
	private int id = ID_NOT_SET;
	private int treadCount = 1;
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

	public void setShape(VShape shape) {
		this.shape = shape;
	}

	public VShape getShape() {
		return shape;
	}

	public int getId() {
		return id;
	}

	public int getTreadCount() {
		return treadCount;
	}

	public Vector2D getUpwardDirection() {
		return upwardDirection;
	}

}
