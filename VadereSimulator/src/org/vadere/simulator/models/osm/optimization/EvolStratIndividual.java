package org.vadere.simulator.models.osm.optimization;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * The Class EvolStratIndividual.
 * 
 */
public class EvolStratIndividual implements Comparable<EvolStratIndividual> {

	private VPoint position;

	private VPoint sigma;

	private double fitness;

	/**
	 * Instantiates a new individual for an evolution strategy.
	 * 
	 * @param position
	 *        the position of the individual
	 */
	public EvolStratIndividual(VPoint position) {
		this.position = position;
		this.sigma = new VPoint(0.1, 0.1);
		this.fitness = -1;
	}

	/**
	 * Instantiates a new individual for an evolution strategy.
	 * 
	 * @param indiv
	 *        an other individual
	 */
	public EvolStratIndividual(EvolStratIndividual indiv) {
		this.position = indiv.getPosition();
		this.sigma = indiv.getSigma();
		this.fitness = indiv.getFitness();
	}

	/**
	 * Compares two individuals by fitness.
	 * 
	 * @param indiv
	 *        an other individual
	 */
	@Override
	public int compareTo(EvolStratIndividual indiv) {
		return new java.lang.Double(this.fitness).compareTo(new java.lang.Double(indiv.getFitness()));
	}

	/**
	 * Sets the fitness of the individual.
	 * 
	 * @param fitness
	 *        the new fitness
	 */
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	/**
	 * Gets the fitness of the individual.
	 * 
	 * @return the fitness
	 */
	public double getFitness() {
		return this.fitness;
	}

	/**
	 * Sets the position of the individual.
	 * 
	 * @param position
	 *        the new position
	 */
	public void setPosition(VPoint position) {
		this.position = position;
	}

	/**
	 * Gets the position of the individual.
	 * 
	 * @return the position
	 */
	public VPoint getPosition() {
		return position;
	}

	/**
	 * Sets the standard derivation for the individual.
	 * 
	 * @param sigma
	 *        the new standard derivation
	 */
	public void setSigma(VPoint sigma) {
		this.sigma = sigma;
	}

	/**
	 * Gets the standard derivation for the individual.
	 * 
	 * @return the sigma
	 */
	public VPoint getSigma() {
		return sigma;
	}

	@Override
	public String toString() {
		return this.position.toString() + " " + this.fitness;
	}

}
