package org.vadere.state.attributes.models.infection;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;

/**
 * AttributesAirTransmissionModel contains user-defined properties related to the
 * <code>AirTransmissionModel</code>.
 * They define properties that are shared by all instances of
 * <ul>
 *     <li><code>Pedestrian</code></li>
 *     <li><code>AerosolCloud</code></li>
 *     <li><code>Droplets</code></li>
 * </ul>
 *
 */
@ModelAttributeClass
public class AttributesAirTransmissionModel extends AttributesExposureModel {

	/**
	 * Equals 1/(pedestrians' average breathing rate).
	 * Unit: seconds
	 */
	private double pedestrianRespiratoryCyclePeriod;

	/**
	 * Defines whether aerosol clouds are considered in the exposure model (true) or not (false).
	 */
	private boolean aerosolCloudsActive;

	private AttributesAirTransmissionModelAerosolCloud aerosolCloudParameters;

	/**
	 * Defines whether droplets are considered in the exposure model (true) or not (false).
	 */
	private boolean dropletsActive;
	private AttributesAirTransmissionModelDroplets dropletParameters;


	public AttributesAirTransmissionModel() {
		super();

		this.pedestrianRespiratoryCyclePeriod = 4;

		this.aerosolCloudsActive = false;
		this.aerosolCloudParameters = new AttributesAirTransmissionModelAerosolCloud();

		this.dropletsActive = false;
		this.dropletParameters = new AttributesAirTransmissionModelDroplets();
	}

	// Getter

	public double getPedestrianRespiratoryCyclePeriod() {
		return pedestrianRespiratoryCyclePeriod;
	}

	public boolean isAerosolCloudsActive() {
		return aerosolCloudsActive;
	}

	public double getAerosolCloudInitialPathogenLoad() {
		return aerosolCloudParameters.getInitialPathogenLoad();
	}

	public double getAerosolCloudHalfLife() {
		return aerosolCloudParameters.getHalfLife();
	}

	public double getAerosolCloudInitialRadius() {
		return aerosolCloudParameters.getInitialRadius();
	}

	public double getAerosolCloudAirDispersionFactor() {
		return aerosolCloudParameters.getAirDispersionFactor();
	}

	public double getAerosolCloudPedestrianDispersionWeight() {
		return aerosolCloudParameters.getPedestrianDispersionWeight();
	}

	public double getAerosolCloudAbsorptionRate() {
		return aerosolCloudParameters.getAbsorptionRate();
	}

	public boolean isDropletsActive() {
		return dropletsActive;
	}

	public double getDropletsEmissionFrequency() {
		return dropletParameters.getEmissionFrequency();
	}

	public double getDropletsDistanceOfSpread() {
		return dropletParameters.getDistanceOfSpread();
	}

	public double getDropletsAngleOfSpreadInDeg() {
		return dropletParameters.getAngleOfSpreadInDeg();
	}

	public double getDropletsLifeTime() {
		return dropletParameters.getLifeTime();
	}

	public double getDropletsPathogenLoad() {
		return dropletParameters.getPathogenLoad();
	}

	public double getDropletsAbsorptionRate() {
		return dropletParameters.getAbsorptionRate();
	}

	public void setAerosolCloudsActive(boolean aerosolCloudsActive) {
		this.aerosolCloudsActive = aerosolCloudsActive;
	}

	public void setAerosolCloudHalfLife(double halfLife) {
		this.aerosolCloudParameters.setHalfLife(halfLife);
	}

	public void setAirDispersionFactor(double airDispersionFactor) {
		this.aerosolCloudParameters.setAirDispersionFactor(airDispersionFactor);
	}

	public void setPedestrianDispersionWeight(double pedestrianDispersionWeight) {
		this.aerosolCloudParameters.setPedestrianDispersionWeight(pedestrianDispersionWeight);
	}

	public void setDropletsActive(boolean dropletsActive) {
		this.dropletsActive = dropletsActive;
	}

	public void setDropletsLifeTime(double lifeTime) {
		this.dropletParameters.setLifeTime(lifeTime);
	}

	public void setDropletsAngleOfSpreadInDeg(double angleOfSpreadInDeg) {
		this.dropletParameters.setAngleOfSpreadInDeg(angleOfSpreadInDeg);
	}
}
