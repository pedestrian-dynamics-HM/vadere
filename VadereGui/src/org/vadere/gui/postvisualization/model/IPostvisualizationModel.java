package org.vadere.gui.postvisualization.model;


import org.vadere.gui.postvisualization.utils.PotentialFieldContainer;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TopographyIterator;
import org.vadere.state.simulation.Step;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

public interface IPostvisualizationModel {

	Logger logger = Logger.getLogger(IPostvisualizationModel.class);

	Scenario getScenario();

	/**
	 * Returns the simulation time which for which the postVis displays all objects.
	 *
	 * @return the simulation time which for which the postVis displays all objects
	 */
	double getSimTimeInSec();

	default double getMaxSimTimeInSec() {
		return Step.toSimTimeInSec(getLastStep(), getSimTimeStepLength());
	}

	/**
	 * Sets the simulation time for which the postVis displays all objects. This can be
	 * any value. If the value is smaller 0 or greater than the overall simulation time
	 * it will be adjusted accordingly.
	 *
	 * @param visTimeInSec  the simulation time for which the postVis displays all objects
	 */
	void setVisTime(final double visTimeInSec);

	/**
	 * Returns a dt in seconds by which the postVis will be stepped forward and backwards.
	 * This can be different from the dt which is defined by the actual output i.e. the <tt>simTimeStepLength</tt>.
	 *
	 * @return a dt by which the postVis will be stepped forward and backwards
	 */
	double getVisTimeStepLength();

	/**
	 * Sets a dt in seconds by which the postVis will be stepped forward and backwards.
	 *
	 * @param visTimeStepLength the dt
	 */
	void setVisTimeStepLength(final double visTimeStepLength);

	/**
	 * Returns a dt in seconds by which the simulation updated its dynamic elements such as
	 * its dynamic floor fields, its sources and targets and so on.
	 *
	 * @return a dt in seconds by which the simulation updated its dynamic elements
	 */
	double getSimTimeStepLength();

	/**
	 * Returns the last simulation step for which an agent is present. A simulation step is defined by
	 * <tt>simTimeStepLength</tt> that is if t is the simulation time than ceil(t / <tt>simTimeStepLength</tt>)
	 * is the simulation {@link Step}.
	 *
	 * @return the last simulation step for which an agent is present
	 */
	int getLastStep();

	/**
	 * Returns the first simulation step for which an agent is present. A simulation step is defined by
	 * <tt>simTimeStepLength</tt> that is if t is the simulation time than ceil(t / <tt>simTimeStepLength</tt>)
	 * is the simulation {@link Step}.
	 *
	 * @return the first simulation step for which an agent is present
	 */
	int getFirstStep();

	default Function<IPoint, Double> getPotentialField() {
		Function<IPoint, Double> f = p -> 0.0;
		Optional<PotentialFieldContainer> optionalPotentialFieldContainer = getPotentialFieldContainer();
		if(optionalPotentialFieldContainer.isPresent()) {
			PotentialFieldContainer container = optionalPotentialFieldContainer.get();
			try {
				final CellGrid potentialField = container.getPotentialField(Step.toFloorStep(getVisTimeStepLength(), getSimTimeStepLength()));
				f = potentialField.getInterpolationFunction();
			} catch (IOException e) {
				logger.warn("could not load potential field from file.");
			}
		}
		return f;
	}

	Optional<PotentialFieldContainer> getPotentialFieldContainer();

	void setPotentialFieldContainer(final PotentialFieldContainer container);

	default boolean isFloorFieldAvailable() {
		return getPotentialFieldContainer().isPresent();
	}

	/**
	 * Returns the number of simulation steps for which agents are present.
	 *
	 * @return the number of simulation steps for which agents are present
	 */
	int getStepCount();

	default void setStep(final int step) {
		synchronized (this) {
			setVisTime(Step.toSimTimeInSec(step, getSimTimeStepLength()));
		}
	}

	default int getStep() {
		synchronized (this) {
			return Step.toFloorStep(getSimTimeInSec(), getSimTimeStepLength());
		}
	}

	/**
	 * Returns all agents which are alive at the current visualization time.
	 *
	 * @return all agents which are alive at the current visualization time.
	 */
	Collection<Agent> getAgents();

	default Topography getTopography() {
		return getScenario().getTopography();
	}

	default Iterator<ScenarioElement> iterator() {
		synchronized (this) {
			return new TopographyIterator(getScenario().getTopography(), getAgents());
		}
	}

	double getGridResolution();
}
