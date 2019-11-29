package org.vadere.simulator.models.seating.dataprocessing;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.seating.SeatingModel;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.Seat;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.processor.AttributesLogEventProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetListener;
import org.vadere.util.data.FindByClass;

/**
 * Produce output in same format as LOG_EVENT.csv from the SeatingDataCollection app.
 * The ID column is added through {@link IdDataKey}!
 * 
 * See *_EVENT constants for supported log event types.
 *
 */
public class LogEventProcessor extends DataProcessor<IdDataKey, LogEventEntry> {
	
	// These fields come from the enum LogEventType from the SeatingDataCollection app:
	private static final String INITIALIZATION_END_EVENT = "INITIALIZATION_END";
	private static final String SIT_DOWN_EVENT = "SIT_DOWN";
	
	private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME; // 15:20:00

	private AttributesLogEventProcessor attributes;
	private TrainModel trainModel;
	private LocalTime time;
	private int nextLogEventId = 1;
	private Set<Seat> observedSeats;

	public LogEventProcessor() {
		super(LogEventEntry.getHeaders());
	}

	@Override
	public void init(ProcessorManager manager) {
		attributes = (AttributesLogEventProcessor) getAttributes();
		if (attributes == null)
			throw new AttributesNotFoundException(AttributesLogEventProcessor.class); //, "Attributes for log event processor are undefined."

		nextLogEventId = attributes.getFirstLogEventId();

		trainModel = getTrainModelFromProcessorManager(manager);
		observedSeats = getSeatsOfCompartment();
	
		for (Seat seat : observedSeats) {
			final int seatNumber = seat.getSeatNumberWithinCompartment();
			seat.getAssociatedTarget().addListener(new LogEventTargetListener(seatNumber));
		}
	}

	@Override
	public void preLoop(SimulationState state) {
		time = LocalTime.now();

		writeInitialSitDownEvents();
		writeInitializationEndEvent();
	}

	private Set<Seat> getSeatsOfCompartment() {
		final Compartment compartment = trainModel.getCompartment(attributes.getCompartmentIndex());
		if (compartment.isHalfCompartment())
			throw new IllegalArgumentException(
					"The log event processors's attribute compartmentIndex denotes a half-compartment."
					+ "The processor cannot be used for half-compartments.");
		// Otherwise, the resulting files are not comparable with the collected data.

		final Set<Seat> result = new HashSet<>();
		for (SeatGroup sg : compartment.getSeatGroups()) {
			result.addAll(sg.getSeats());
		}
		return result;
	}

	@Override
	public void doUpdate(SimulationState state) {
		updateTime(state);
		// sit-down stimuli are logged by a target listener registered in init()
	}

	private TrainModel getTrainModelFromProcessorManager(ProcessorManager manager) {
		final MainModel mainModel = manager.getMainModel();
		final SeatingModel seatingModel = FindByClass.findFirstObjectOfClass(mainModel.getSubmodels(), SeatingModel.class);
		return seatingModel.getTrainModel();
	}

	private void writeInitialSitDownEvents() {
		for (Seat seat : observedSeats) {
			final Pedestrian person = seat.getSittingPerson();
			if (person != null) {
				writeSitDownEvent(person, seat.getSeatNumberWithinCompartment());
			}
		}
	}

	private void writeInitializationEndEvent() {
		writeEvent(INITIALIZATION_END_EVENT, null, null);
	}

	private void writeSitDownEvent(Pedestrian person, int seatNumber) {
		writeEvent(SIT_DOWN_EVENT, person.getId() + attributes.getPersonIdOffset(), seatNumber);
	}

	private void writeEvent(String eventType, Integer personId, Integer seatNumber) {
		final int logEventId = nextLogEventId++;
		final String timeString = timeFormatter.format(time);
		final int surveyId = attributes.getSurveyId();
		putValue(new IdDataKey(logEventId), new LogEventEntry(timeString, eventType, personId, seatNumber, surveyId));
	}

	private void updateTime(SimulationState state) {
		long nanos = (long) (state.getSimTimeInSec() * 1e9);
		time = time.plusNanos(nanos);
	}

	private class LogEventTargetListener implements TargetListener {
		private int seatNumber;

		public LogEventTargetListener(int seatNumberWithinCompartment) {
			seatNumber = seatNumberWithinCompartment;
		}

		@Override
		public void reachedTarget(Target target, Agent agent) {
			final Seat seat = trainModel.getSeatForTarget(target);
			// This check is necessary because a second person could reach the
			// target (and find that the seat is already taken).
			// Order of listener invocations is not predictable, therefore both options are checked.
			if (seat.isAvailable() || seat.getSittingPerson() == agent)
				writeSitDownEvent((Pedestrian) agent, seatNumber);
		}
	}

}
