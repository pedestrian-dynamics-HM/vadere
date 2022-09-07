package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesGFM extends Attributes {

	private final double gravityFactor = 1;
	private final double normalParticleStiffness = 5000;
	private final double dampingParticle = 40;
	private final double normalObstacleStiffness = 10000;
	private final double dampingObstacle = 40;
	private final double timeStepSize = 0.01;
	private final double dynamicFrictionParticle = 0.25;
	private final double staticFrictionParticle = 0.2;
	private final double dynamicFrictionObstacle = 0.25;
	private final double staticFrictionObstacle = 0.2;
	private final double addhesionCoefficient = 0.1;
	private final double tangentialStiffness = 1000;
	private final double tangentialDissipation = 10;
	private final double startAccelerationX = 0;
	private final double startAccelerationY = 0;
	private final double accelerationTime = 0;
	private final int accelerationId = 0;
	private final double massParticle = 1;
	private final double momOfInertia = 1;
	private final double startOmega = 0;

	public double getGravityFactor() {
		return gravityFactor;
	}

	public double getNormalParticleStiffness() {
		return normalParticleStiffness;
	}

	public double getDampingParticle() {
		return dampingParticle;
	}

	public double getNormalObstacleStiffness() {
		return normalObstacleStiffness;
	}

	public double getDampingObstacle() {
		return dampingObstacle;
	}

	public double getTimeStepSize() {
		return timeStepSize;
	}

	public double getDynamicFrictionParticle() {
		return dynamicFrictionParticle;
	}

	public double getStaticFrictionParticle() {
		return staticFrictionParticle;
	}

	public double getDynamicFrictionObstacle() {
		return dynamicFrictionObstacle;
	}

	public double getStaticFrictionObstacle() {
		return staticFrictionObstacle;
	}

	public double getAddhessionCoefficient() {
		return addhesionCoefficient;
	}

	public double getTangentialStiffness() {
		return tangentialStiffness;
	}

	public double getTangentialDissipation() {
		return tangentialDissipation;
	}

	public double startAccelerationX() {
		return startAccelerationX;
	}

	public double startAccelerationY() {
		return startAccelerationY;
	}

	public double getAccelerationTime() {
		return accelerationTime;
	}

	public int getAccelerationId() {
		return accelerationId;
	}

	public double getMassParticle() {
		return this.massParticle;
	}

	public double getMomentOfInertia() {
		return this.momOfInertia;
	}

	public double getStartOmega() {
		return startOmega;
	}
}
