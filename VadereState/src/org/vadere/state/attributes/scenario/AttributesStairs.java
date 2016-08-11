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
	private int id = -1;
	private int treadCount = 1;
	private Vector2D upwardDirection = new Vector2D(1.0, 0.0);

	public AttributesStairs() {
		// needs to be present for GSON
	}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((shape == null) ? 0 : shape.hashCode());
		result = prime * result + treadCount;
		result = prime * result
				+ ((upwardDirection == null) ? 0 : upwardDirection.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AttributesStairs))
			return false;
		AttributesStairs other = (AttributesStairs) obj;
		if (id != other.id)
			return false;
		if (shape == null) {
			if (other.shape != null)
				return false;
		} else if (!shape.equals(other.shape))
			return false;
		if (treadCount != other.treadCount)
			return false;
		if (upwardDirection == null) {
			if (other.upwardDirection != null)
				return false;
		} else if (!upwardDirection.equals(other.upwardDirection))
			return false;
		return true;
	}

}
