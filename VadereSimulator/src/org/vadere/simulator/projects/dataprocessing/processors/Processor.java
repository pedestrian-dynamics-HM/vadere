package org.vadere.simulator.projects.dataprocessing.processors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.util.data.Table;

public interface Processor {

	/**
	 * Returns a list of all columns that can be in the time step Table. This includes all columns
	 * from all Processors of this processor.
	 * 
	 * @return a list of all columns that can be in the time step Table.
	 */
	String[] getAllColumnNames();

	/**
	 * Sets the columns that will be part of the Table. The names are given by convention of the
	 * concrete Processor-class. You have to know which names are available, otherwise the processor
	 * will not work correctly.
	 * 
	 * Note: If a processor uses another processor to calculate values than he has to tell his
	 * processor
	 * to calculate the required values. So the processor has to call this method.
	 * 
	 * @param columnNames the column names
	 */
	void addColumnNames(final String... columnNames) throws IllegalArgumentException;

	/**
	 * Tells the processor to calculate the new values based on the simulation state.
	 * 
	 * @param state the simulation state
	 * @return a table with the new values
	 */
	Table postUpdate(final SimulationState state);

	/**
	 * Tells the processor that the simulation loop is over. Some processors return a filled table
	 * only after the
	 * simulation is over (such as the {@link MeanEvacuationTimeProcessor}).
	 * 
	 * @return a table with the new values calculated throw the whole simulation loop.
	 */
	Table postLoop(final SimulationState state);

	/**
	 * Tells the processor to prepare himself before the loop starts. Here one can put in some reset
	 * code.
	 */
	Table preLoop(final SimulationState state);

	/**
	 * Returns the attributes of this processor or null if the processor has no attributes.
	 * 
	 * @return the attributes of this processor or null if the processor has no attributes.
	 */
	Attributes getAttributes();

	/**
	 * Returns a simple unique name that acts as representation (for the gui and for the factory
	 * which is also for the gui).
	 * 
	 * @return a simple name that acts as representation
	 */
	String getName();
	// //TODO: [priority=medium] [task=refactoring] add some methods for gui (description and so on...)

	/**
	 * Returns the file extension that should be used if the generated output will be written to a
	 * file.
	 * 
	 * @return the file extension that should be used if the generated output will be written to a
	 *         file
	 */
	String getFileExtension();

	boolean isRequiredForVisualisation();
}
