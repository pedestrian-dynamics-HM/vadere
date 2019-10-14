package org.vadere.state.scenario;

import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.util.geometry.shapes.VShape;

public class TargetPedestrian extends Target implements DynamicElementRemoveListener<Pedestrian> {

	// Static Variables
	/**
	 * {@link Target}s and {@link TargetPedestrian}s share the same address space for ids!
	 * This can cause conflicts when the target potential is retrieved
	 * in "PotentialFieldTarget.getPotential()". "PotentialFieldTarget"
	 * stores potentials in a map<targetId, potentials>. Therefore, use
	 * a fixed offset to discriminate {@link Target}s and {@link TargetPedestrian}s.
	 */
	public static final int UNIQUE_ID_OFFSET = 100000;

	// Member Variables
	private final Pedestrian pedestrian;
	private boolean isDeleted;

	// Constructors
	public TargetPedestrian(Pedestrian pedestrian) {
		super(new AttributesTarget(pedestrian));
		this.pedestrian = pedestrian;
		this.isDeleted = false;
	}

	// Getters
	@Override
	public int getId() {
		int targetId = pedestrian.getId() + TargetPedestrian.UNIQUE_ID_OFFSET;
		return targetId;
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

	// Setters
	@Override
	public void setShape(VShape newShape) {
		pedestrian.setShape(newShape);
	}

	// Methods
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
	
	@Override
	public TargetPedestrian clone() {
		return  new TargetPedestrian(pedestrian.clone());
	}

}
