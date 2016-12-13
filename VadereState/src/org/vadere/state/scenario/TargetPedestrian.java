package org.vadere.state.scenario;

import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.util.geometry.shapes.VShape;

public class TargetPedestrian extends Target implements DynamicElementRemoveListener<Pedestrian> {

	private final Pedestrian pedestrian;
	private boolean isDeleted;

	public TargetPedestrian(Pedestrian pedestrian) {
		super(new AttributesTarget(pedestrian));
		this.pedestrian = pedestrian;
		this.isDeleted = false;
	}

	@Override
	public void setShape(VShape newShape) {
		pedestrian.setShape(newShape);
	}

	@Override
	public VShape getShape() {
		return pedestrian.getShape();
	}

	@Override
	public boolean isTargetPedestrian() {
		return true;
	}

	public Pedestrian getPedestrian() {
		return this.pedestrian;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	@Override
	public void elementRemoved(Pedestrian pedestrian) {
		isDeleted = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;

		TargetPedestrian that = (TargetPedestrian) o;

		if (isDeleted != that.isDeleted)
			return false;
		if (!pedestrian.equals(that.pedestrian))
			return false;

		return true;
	}
}
