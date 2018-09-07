package org.vadere.util.math.pso;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.ICircleSector;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 */
public class PSO {
	private AttributesPSO attributesPSO = new AttributesPSO();
	private Random random;
	private final List<Particle> particles;
	private final ICircleSector circle;
	private final double errorToleranz = 0.0001;
	private double gBest;
	private VPoint gBestLocation;
	private final Function<VPoint, Double> f;
	private int iterationCounter;
	private final double maxVelocity;
	private final double minAngle;
	private final double maxAngle;

	public PSO(
			@NotNull final Function<VPoint, Double> f,
			@NotNull final ICircleSector circle,
			final double minAngle,
			final double maxAngle,
			@NotNull final Random random,
			@NotNull final double maxVelocity,
			@NotNull final List<VPoint> swarmPositions) {
		this.f = f;
		this.random = random;
		this.circle = circle;
		this.gBest = Double.MAX_VALUE;
		this.gBestLocation = circle.getCenter();
		this.iterationCounter = 0;
		this.maxVelocity = maxVelocity;
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
		this.particles = initialSwarm(swarmPositions);
	}

	public VPoint getOptimumArg() {
		while (!hasFinished()) {
			update();
		}
		return getBestLocation();
	}

	public double getOptimum() {
		while (!hasFinished()) {
			update();
		}
		return getBestValue();
	}

	private VPoint getBestLocation() {
		return gBestLocation;
	}

	private double getBestValue() {
		return gBest;
	}

	public boolean hasFinished() {
		return iterationCounter >= attributesPSO.maxIteration || hasConverged();
	}

	public boolean hasConverged() {
		return false;
	}

	public void update() {
		if (iterationCounter < attributesPSO.maxIteration) {
			iterationCounter++;
			updateLocalBest();
			updateGlobalBest();

			double omega = attributesPSO.wUpperBound - (iterationCounter / attributesPSO.maxIteration) * (attributesPSO.wUpperBound - attributesPSO.wLowerBound);
			particles.forEach(particle -> updateParticle(particle, omega));
		}
	}

	private void updateParticle(@NotNull final Particle particle, final double omega) {
		double rLocal = random.nextDouble();
		double rGlobal = random.nextDouble();

		VPoint dirLocalBest = particle.getLocalBestLocation().subtract(particle.getLocation());
		VPoint dirGobalBest = particle.getGlobalBestLocation().subtract(particle.getLocation());

		VPoint velocity = particle.getVelocity().scalarMultiply(omega)
				.add(dirLocalBest.scalarMultiply(rLocal * attributesPSO.c1))
				.add(dirGobalBest.scalarMultiply(rGlobal * attributesPSO.c2))
				.limit(maxVelocity);

		VPoint currentLocation = particle.getLocation();
		particle.setVelocity(velocity);
		particle.setLocation(particle.getLocation().add(particle.getVelocity()));

		if (!circle.contains(particle.getLocation())) {
			particle.setVelocity(particle.getVelocity().scalarMultiply(-0.5));
			particle.setLocation(circle.getClosestIntersectionPoint(currentLocation, particle.getLocation(), particle.getLocation()).orElse(particle.getLocation()));
		}
		particle.setFitnessValue(f.apply(particle.getLocation()));
	}

	/**
	 * updates the best local value of each particle
	 */
	private void updateLocalBest() {
		particles.parallelStream().forEach(particle -> {

			if (particle.getFitnessValue() < particle.getLocalBestFitnessValue()) {
				particle.setLocalBestFitnessValue(particle.getFitnessValue());
				particle.setLocalBestLocation(particle.getLocation());
			}

			if (particle.getLocalBestFitnessValue() < particle.getGlobalBestFitnessValue()) {
				particle.setGlobalBestFitnessValue(particle.getLocalBestFitnessValue());
				particle.setGlobalBestLocation(particle.getLocalBestLocation());
			}

		});
	}

	/**
	 * 1) updates the best overall value of the PSO. 2) if the new best overall value is smaller
	 * than the old one, particles inform each other about their best values and locations.
	 */
	private void updateGlobalBest() {
		double lastGBest = gBest;

		for (Particle particle : particles) {
			double globalBest = particle.getGlobalBestFitnessValue();
			if (globalBest < gBest) {
				gBest = globalBest;
				gBestLocation = particle.getGlobalBestLocation();
			}
		}

		if (gBest >= lastGBest) {
			informKParticle();
		}
	}

	private void informParticles(@NotNull final Particle particle, @NotNull final Particle otherParticle) {
		double globalBest = particle.getGlobalBestFitnessValue();
		double otherGlobalBest = otherParticle.getGlobalBestFitnessValue();

		if (globalBest < otherGlobalBest) {
			otherParticle.setGlobalBestFitnessValue(globalBest);
			otherParticle.setGlobalBestLocation(particle.getGlobalBestLocation());
		}
	}

	private void informKParticle() {
		for (Particle particle : particles) {
			for (int i = 0; i < attributesPSO.numberOfInformedParticles; i++) {
				int index = (int) Math.floor(random.nextDouble() * particles.size());
				Particle otherParticle = particles.get(index);
				informParticles(particle, otherParticle);
			}
		}
	}

	private void informAllParticles() {
		for (Particle particle : particles) {
			double globalBest = particle.getGlobalBestFitnessValue();

			assert globalBest >= gBest;

			particle.setGlobalBestFitnessValue(gBest);
			particle.setGlobalBestLocation(gBestLocation);
		}
	}

	private List<Particle> initialSwarm(@NotNull List<VPoint> swarmPositions) {
		return swarmPositions.stream().map(location -> locationToParticle(location)).collect(Collectors.toList());
	}

	private Particle locationToParticle(@NotNull final VPoint location) {
		double vDelta = random.nextDouble() * (maxAngle - minAngle);
		double vMag = Math.sqrt(random.nextDouble()) * circle.getRadius();
		VPoint v = new VPoint(Math.cos(vDelta), Math.sin(vDelta)).norm(vMag);
		VPoint velocity = v.subtract(location).scalarMultiply(0.5).limit(maxVelocity);

		double fitnessValue = f.apply(location);
		return new Particle(location, velocity, fitnessValue);
	}
}
