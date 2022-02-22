package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesEmbedShape;

import java.util.ArrayList;
import java.util.Arrays;

import org.vadere.state.attributes.models.infection.AttributesTransmissionModelAerosolCloud;
import org.vadere.state.attributes.models.infection.AttributesTransmissionModelDroplets;
import org.vadere.state.attributes.models.infection.AttributesExposureModelSourceParameters;
import org.vadere.state.health.TransmissionModelHealthStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Droplets;
import org.vadere.state.scenario.Pedestrian;

/**
 * This class defines the attributes of the corresponding exposure model. All attributes are defined by the user and
 * relate to
 * <ul>
 *     <li>the TransmissionModel: {@link #transmissionModelSourceParameters}, {@link #pedestrianRespiratoryCyclePeriod}</li>
 *     <li>the {@link TransmissionModelHealthStatus} of the {@link Pedestrian}s</li>
 *     <li>the {@link AerosolCloud}s' initial attributes when they are created by the TransmissionModel</li>
 *     <li>the {@link Droplets}s' initial attributes when they are created by the TransmissionModel</li>
 * </ul>
 */
@ModelAttributeClass
public class AttributesTransmissionModel extends Attributes {

	private ArrayList<AttributesExposureModelSourceParameters> transmissionModelSourceParameters;

	/**
	 * Attribute related to the pedestrians' health state that is shared among all pedestrians. It is not defined
	 * for each instance of TransmissionModelHealthStatus separately to keep the TransmissionModelHealthStatus lean.
	 * pedestrianRespiratoryCyclePeriod equals 1/(pedestrians' average breathing rate) in seconds.
	 */
	private double pedestrianRespiratoryCyclePeriod;

	/**
	 * Defines whether aerosol clouds are considered in the exposure model (true) or not (false).
	 */
	private boolean aerosolCloudsActive;
	private AttributesTransmissionModelAerosolCloud aerosolCloudParameters;

	/**
	 * Defines whether droplets are considered in the exposure model (true) or not (false).
	 */
	private boolean dropletsActive;
	private AttributesTransmissionModelDroplets dropletParameters;


	public AttributesTransmissionModel() {
		this.transmissionModelSourceParameters = new ArrayList<>(Arrays.asList(new AttributesExposureModelSourceParameters(AttributesEmbedShape.ID_NOT_SET, false)));

		this.pedestrianRespiratoryCyclePeriod = 4;

		this.aerosolCloudsActive = false;
		this.aerosolCloudParameters = new AttributesTransmissionModelAerosolCloud();

		this.dropletsActive = false;
		this.dropletParameters = new AttributesTransmissionModelDroplets();
	}

	// Getter

	public double getPedestrianRespiratoryCyclePeriod() {
		return pedestrianRespiratoryCyclePeriod;
	}

	public ArrayList<AttributesExposureModelSourceParameters> getTransmissionModelSourceParameters() {
		return transmissionModelSourceParameters;
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

	// Setter

	public void setAerosolCloudsActive(boolean aerosolCloudsActive) {
		this.aerosolCloudsActive = aerosolCloudsActive;
	}

	public void setAerosolCloudHalfLife(double aerosolCloudHalfLife) {
		this.aerosolCloudParameters.setHalfLife(aerosolCloudHalfLife);
	}

	public void setAerosolCloudInitialRadius(double aerosolCloudInitialRadius) {
		this.aerosolCloudParameters.setInitialRadius(aerosolCloudInitialRadius);
	}

	public void setAerosolCloudInitialPathogenLoad(double aerosolCloudInitialPathogenLoad) {
		this.aerosolCloudParameters.setInitialPathogenLoad(aerosolCloudInitialPathogenLoad);
	}

	public void setAerosolCloudAirDispersionFactor(double aerosolCloudAirDispersionFactor) {
		this.aerosolCloudParameters.setAirDispersionFactor(aerosolCloudAirDispersionFactor);
	}

	public void setAerosolCloudPedestrianDispersionWeight(double aerosolCloudPedestrianDispersionWeight) {
		this.aerosolCloudParameters.setPedestrianDispersionWeight(aerosolCloudPedestrianDispersionWeight);
	}

	public void setAerosolCloudAbsorptionRate(double aerosolCloudAbsorptionRate) {
		this.aerosolCloudParameters.setAbsorptionRate(aerosolCloudAbsorptionRate);
	}

	public void setDropletsActive(boolean dropletsActive) {
		this.dropletsActive = dropletsActive;
	}
}
