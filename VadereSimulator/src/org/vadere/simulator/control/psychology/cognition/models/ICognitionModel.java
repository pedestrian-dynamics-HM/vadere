package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.simulator.models.Model;
import org.vadere.state.attributes.models.psychology.cognition.AttributesCognitionModel;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.Random;

/**
 * Interface for a cognition model.
 *
 * A cognition model decides to which {@link SelfCategory} a {@link Pedestrian}
 * identifies to. From this {@link SelfCategory} a specific behavior derives.
 * E.g. if {@Link SelfCategory} = {@link SelfCategory#COOPERATIVE}, pedestrians
 * can swap places.
 *
 * It is designed as an interface so that different models can be used for
 * different scenarios (by specifying in the JSON file).
 *
 * The <code>initialize</code> method must be called before usage!
 * This interface defines callbacks for the simulation loop.
 * It's implementations define the major part of the simulation model's logic.
 *
 * This approach is similar to the {@link Model} interface for locomotion models.
 *
 * Watch out: The perception phase should be finished before using
 * methods in this class because, usually, first a stimulus is processed and
 * then pedestrians decide which behavior to follow. E.g., first a {@link Threat}
 * is recognized and then a pedestrian decides to follow a
 * {@link SelfCategory#THREATENED} behavior.
 */
public interface ICognitionModel {

	/**
	 * This method initializes the model. It gets the {@link Topography} so that
	 * a model can acquire additional information about a pedestrian's surrounding
	 * when evaluating pedestrian's cognition.
	 */
	void initialize(Topography topography, Random random);

	/**
	 * Usually, this method iterates over the pededestrians and calls
	 * {@link Pedestrian#setSelfCategory(SelfCategory)}.
	 *
	 * The current simulation time (in seconds) can be extracted from
	 * pedestrian's {@link Pedestrian#getMostImportantStimulus()}.
	 *
	 * @param pedestrians The pedestrians to update
	 */
	void update(Collection<Pedestrian> pedestrians);

	void setAttributes(AttributesCognitionModel attributes);

	AttributesCognitionModel getAttributes();

}
