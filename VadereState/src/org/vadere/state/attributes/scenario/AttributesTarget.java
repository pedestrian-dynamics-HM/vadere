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
	@VadereAttribute
	private AttributesAbsorber absorber = new AttributesAbsorber();
	@VadereAttribute
	private AttributesWaiter waiter = new AttributesWaiter();
	@VadereAttribute
	private Double leavingSpeed = 0.0;

	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private Integer parallelWaiters = 0;


	// TODO should be "reachedDistance"; agents do not necessarily get deleted/absorbed


	/**
	 *  Distribution types:<br>
	 *  <ul>
	 *  <li>"binomial"
	 *  <li>"constant"
	 *  <li>"empirical"
	 *  <li>"linearInterpolation"
	 *  <li>"mixed"
	 *  <li>"negativeExponential"
	 *  <li>"normal"
	 *  <li>"poisson"
	 *  <li>"singleSpawn"
	 *  <li>"timeSeries"
	 *  </ul>
	 */
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

	public AttributesTarget(Pedestrian pedestrian) {
		this.shape = pedestrian.getShape();
		this.id = pedestrian.getIdAsTarget();
		this.parallelWaiters = 0;
	}

	// Getters...

	public boolean isAbsorbing() {
		return this.absorber.isEnabled();
	}

	public void setAbsorbing(boolean absorbing) {
		checkSealed();
		this.absorber.setEnabled(absorbing);
	}

	public int getParallelWaiters() {
		return parallelWaiters;
	}

	public void setParallelWaiters(int parallelWaiters) {
		checkSealed();
		this.parallelWaiters = parallelWaiters;
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
}
