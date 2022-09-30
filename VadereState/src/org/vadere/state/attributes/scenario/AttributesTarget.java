package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;
import org.vadere.state.attributes.AttributesAbsorber;
import org.vadere.state.attributes.AttributesWaiter;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;
/**
 * Attributes of a {@link Target}.
 * @author Ludwig Jaeck
 */
public class AttributesTarget extends AttributesVisualElement {
	/**
	 * This component controls the absorbing behaviour of this target.
	 */
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class)
	private AttributesAbsorber absorber = new AttributesAbsorber(true, 0.1);

	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class)
	private AttributesWaiter waiter = new AttributesWaiter();
	/**
	 * This attribute stores the speed an agent has after leaving this target
	 */
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class)
	private Double leavingSpeed = 0.0;
	/**
	 * This attributes stores the number of agents the target can process at the same time.<br>
	 * <b>NOTE:</b> If set to zero the target can process any number of agents at the same time.
	 */
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private Integer parallelEvents = 0;

	public AttributesTarget() {
		super();
	}

	public AttributesTarget(final int id,final VShape shape) {
		super();
		this.shape = shape;
	}

	public AttributesTarget(final VShape shape, final int id) {
		this.shape = shape;
		this.id = id;
	}
	public AttributesTarget(final VShape shape, final int id,boolean absorbing) {
		this(shape,id);
		setAbsorbing(true);
	}

	public AttributesTarget(Pedestrian pedestrian) {
		this.shape = pedestrian.getShape();
		this.id = pedestrian.getIdAsTarget();
	}

	// Getters...

	public boolean isAbsorbing() {
		return this.absorber.isEnabled();
	}

	public void setAbsorbing(boolean absorbing) {
		checkSealed();
		this.absorber.setEnabled(absorbing);
	}

	public AttributesAbsorber getAbsorberAttributes() {
		return absorber;
	}

	public void setAbsorberAttributes(AttributesAbsorber absorber) {
		this.absorber = absorber;
	}

	public Boolean isWaiting() {
		return this.waiter.isEnabled();
	}

	public void setWaiting(Boolean waiting) {
		checkSealed();
		this.absorber.setEnabled(waiting);
	}

	public AttributesWaiter getWaiterAttributes() {
		return waiter;
	}

	public void setWaiterAttributes(AttributesWaiter waiter) {
		checkSealed();
		this.waiter = waiter;
	}

	public Double getLeavingSpeed() {
		return leavingSpeed;
	}

	public void setLeavingSpeed(Double leavingSpeed) {
		checkSealed();
		this.leavingSpeed = leavingSpeed;
	}

	public Integer getParallelEvents() {
		return parallelEvents;
	}

	public void setParallelEvents(Integer parallelEvents) {
		checkSealed();
		this.parallelEvents = parallelEvents;
	}
}
