package org.vadere.state.attributes.models;

import com.fasterxml.jackson.annotation.JsonView;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.CacheType;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.state.util.Views;

@ModelAttributeClass
public class AttributesFloorField extends Attributes {

	private EikonalSolverType createMethod = EikonalSolverType.HIGH_ACCURACY_FAST_MARCHING;

	/**
	 * These attribute values should only be used if createMethod.isUsingCellGrid() == true.
	 *
	 * TODO [refactoring]: However potentialFieldResolution is also used for the @link org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction
	 * for the density computation, i.e. it is the resolution of the matrix used in the discrete convolution. This should be changed!
	 * Furthermore, theare are many unused parameters in {@link AttributesTimeCost}.
	 * Solution:
	 * (1) change AttributesTimeCost timeCostAttributes to ITimeCostFunction like the potential classes in AttributesOSM
	 * (2) split AttributesTimeCost timeCostAttributes into multiple classes
	 * (3) add a new AttributesTimeCost into the top level (i.e. attributesModel) json.
	 */
	private double potentialFieldResolution = 0.1;
	private double obstacleGridPenalty = 0.1;

	/**
	 * The targetAttractionStrength weights the target potential against the agent potential and the obstacle potential.
	 * targetAttractionStrength > 1 means the target becomes more attractive (other parameters: default values).
	 * Thus, agents keep less distance to other agents and to walls.
	 * Choose targetAttractionStrength > 1, if agents get stuck (clogging) before narrow corridors (<1.28m).
	 * Clogging can occur if the gradient of the obstacle potential is larger than the gradient of the target potential.
	 * Then, the global minimum (within walking distance) is in front of the corridor.
	 * If case of clogging, we recommend to choose the targetAttractionStrengh depending on the bottleneck width c.
	 * 						 	  |	 25.0 		for c < 0.8m  (a) -> not tested
	 * targetAttractionStrengh = -|	 25.0-18.8c 	for c < 1.28m (b) -> fulfils condition (d)
	 * 							  |	 1.0 			for c >=1.28m (c) -> default value 1.
	 * (d): | max. grad (target potential) | > | max. grad (obstacle potential) | (both measured at extended corridor center line)
	 *
	 * 			|
	 * 			==========  .
	 * 	   --X-----------             |target|
	 * 			==========	.
	 * 			|
	 * Legend:
	 * 		.  . 	: distance between points > corridor width c
	 * 		X		: agent stuck on minimum in front of narrow corridor
	 * 	    ---- 	: center line
	 */

	private double targetAttractionStrength = 1.0;

	/**
	 * If true, the floor field will be initialized based on a previously cached version. The
	 * cached floor field will be search at {@link #cacheDir}. The file name of the cached floor
	 * field has two parts: "[cacheDir]/[hash-of-AttributesFloorField-Topography]_[targetId].ffcache"
	 *
	 * The hash ensures that only valid version of the cache will be used. If something changed in
	 * the topography or in this attributes settings (except of {@link #cacheDir}) the hash will
	 * differ an a new floor field is created and saved at initialization time.
	 *
	 * Thus the created hash file can be moved to a different location without invalidating the
	 * saved floor field.
	 */
	@JsonView(Views.CacheViewExclude.class)
	private CacheType cacheType = CacheType.NO_CACHE;

	/**
	 * specifies path in which a cache of the target floor field is saved. This path is relative to
	 * the executable. @JsonView(...) allows the cache hashing to move the cache directory without
	 * invalidating existing caches.
	 */
	@JsonView(Views.CacheViewExclude.class)
	private String cacheDir = "";

	private AttributesTimeCost timeCostAttributes;

	public AttributesFloorField() {
		timeCostAttributes = new AttributesTimeCost();
	}

	// Getters...
	public EikonalSolverType getCreateMethod() {
		return createMethod;
	}

	public double getPotentialFieldResolution() {
		return potentialFieldResolution;
	}

	public double getObstacleGridPenalty() {
		return obstacleGridPenalty;
	}

	public double getTargetAttractionStrength() {
		return targetAttractionStrength;
	}

	public AttributesTimeCost getTimeCostAttributes() {
		return timeCostAttributes;
	}

	public boolean isUseCachedFloorField() {
		return cacheType != CacheType.NO_CACHE;
	}

	public String getCacheDir() {
		return cacheDir;
	}

	public void setCreateMethod(EikonalSolverType createMethod) {
		checkSealed();
		this.createMethod = createMethod;
	}

	public void setPotentialFieldResolution(double potentialFieldResolution) {
		checkSealed();
		this.potentialFieldResolution = potentialFieldResolution;
	}

	public void setObstacleGridPenalty(double obstacleGridPenalty) {
		checkSealed();
		this.obstacleGridPenalty = obstacleGridPenalty;
	}

	public void setTargetAttractionStrength(double targetAttractionStrength) {
		checkSealed();
		this.targetAttractionStrength = targetAttractionStrength;
	}

	public CacheType getCacheType() {
		return cacheType;
	}

	public void setCacheType(CacheType cacheType) {
		this.cacheType = cacheType;
	}


	public void setTimeCostAttributes(AttributesTimeCost timeCostAttributes) {
		checkSealed();
		this.timeCostAttributes = timeCostAttributes;
	}

	public void setCacheDir(String cacheDir) {
		checkSealed();
		this.cacheDir = cacheDir;
	}
}
