package org.vadere.util.math.optimization.pso;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 *
 * A Particle of the PSO (particle swarm optimization) is a JavaBean (no logic) and one member of
 * the swarm. The logic is part of the {@link PSO} class. At each iteration of the PSO the particles
 * velocity is updated based on the globalBestLocation, the localBestLocation and the current
 * velocity. The new location is determined by the velocity and the old location.
 */
public class Particle {

	/**
	 * The current velocity which determines the next location.
	 */
	private VPoint velocity;

	/**
	 * Fitness value of the current location.
	 */
	private double fitnessValue;

	/**
	 * The best fitness value found by the particle itself.
	 */
	private double localBestFitnessValue;

	/**
	 * The best fitness value of a group of particles which informed this particle.
	 */
	private double globalBestFitnessValue;

	/**
	 * The current location of the particle i.e. where the fitness value is equal to fitnessValue.
	 */
	private VPoint location;

	/**
	 * The location at which the fitness value is equal to localBestFitnessValue.
	 */
	private VPoint localBestLocation;

	/**
	 * The location at which the fitness value is equal to globalBestFitnessValue.
	 */
	private VPoint globalBestLocation;


	public Particle(@NotNull final VPoint location, @NotNull final VPoint velocity, double fitnessValue) {
		this.velocity = velocity;

		this.location = location;
		this.localBestLocation = location;
		this.globalBestLocation = location;

		this.fitnessValue = fitnessValue;
		this.localBestFitnessValue = fitnessValue;
		this.globalBestFitnessValue = fitnessValue;
	}

	public VPoint getVelocity() {
		return velocity;
	}

	public void setVelocity(@NotNull final VPoint velocity) {
		this.velocity = velocity;
	}

	public double getFitnessValue() {
		return fitnessValue;
	}

	public void setFitnessValue(final double fitnessValue) {
		this.fitnessValue = fitnessValue;
	}

	public double getLocalBestFitnessValue() {
		return localBestFitnessValue;
	}

	public void setLocalBestFitnessValue(final double localBestFitnessValue) {
		this.localBestFitnessValue = localBestFitnessValue;
	}

	public double getGlobalBestFitnessValue() {
		return globalBestFitnessValue;
	}

	public void setGlobalBestFitnessValue(final double globalBestFitnessValue) {
		this.globalBestFitnessValue = globalBestFitnessValue;
	}

	public VPoint getLocation() {
		return location;
	}

	public void setLocation(@NotNull final VPoint location) {
		this.location = location;
	}

	public VPoint getLocalBestLocation() {
		return localBestLocation;
	}

	public void setLocalBestLocation(@NotNull final VPoint localBestLocation) {
		this.localBestLocation = localBestLocation;
	}

	public VPoint getGlobalBestLocation() {
		return globalBestLocation;
	}

	public void setGlobalBestLocation(@NotNull final VPoint globalBestLocation) {
		this.globalBestLocation = globalBestLocation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Particle particle = (Particle) o;

		if (Double.compare(particle.fitnessValue, fitnessValue) != 0) return false;
		if (Double.compare(particle.localBestFitnessValue, localBestFitnessValue) != 0)
			return false;
		if (Double.compare(particle.globalBestFitnessValue, globalBestFitnessValue) != 0)
			return false;
		if (!velocity.equals(particle.velocity)) return false;
		if (!location.equals(particle.location)) return false;
		if (!localBestLocation.equals(particle.localBestLocation)) return false;
		return globalBestLocation.equals(particle.globalBestLocation);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = velocity.hashCode();
		temp = Double.doubleToLongBits(fitnessValue);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(localBestFitnessValue);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(globalBestFitnessValue);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + location.hashCode();
		result = 31 * result + localBestLocation.hashCode();
		result = 31 * result + globalBestLocation.hashCode();
		return result;
	}
}
