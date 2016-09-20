package org.vadere.simulator.models.seating;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.seating.trainmodel.Compartment;
import org.vadere.simulator.models.seating.trainmodel.Seat;
import org.vadere.simulator.models.seating.trainmodel.SeatGroup;
import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.processor.AttributesLogEventProcessor;
import org.vadere.state.scenario.Pedestrian;
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
	private Set<Seat> emptySeats;

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
	}

	@Override
	public void preLoop(SimulationState state) {
		emptySeats = getSeatsOfCompartment(); // will be filtered in writeNewSitDownEvents()
		time = LocalTime.now();

		writeInitialSitDownEvents();
		writeInitializationEndEvent();
	}

	private Set<Seat> getSeatsOfCompartment() {
		final Compartment compartment = trainModel.getCompartment(attributes.getCompartmentIndex());
		final Set<Seat> result = new HashSet<>();
		for (SeatGroup sg : compartment.getSeatGroups()) {
			for (int i = 0; i < 4; i++) {
				result.add(sg.getSeat(i));
			}
		}
		return result;
	}

	@Override
	public void doUpdate(SimulationState state) {
		updateTime(state);
		writeNewSitDownEvents();
	}

	private TrainModel getTrainModelFromProcessorManager(ProcessorManager manager) {
		final MainModel mainModel = manager.getMainModel();
		final SeatingModel seatingModel = FindByClass.findFirstObjectOfClass(mainModel.getActiveCallbacks(), SeatingModel.class);
		return seatingModel.getTrainModel();
	}

	private void writeInitialSitDownEvents() {
		writeNewSitDownEvents();
	}

	private void writeNewSitDownEvents() {
		Iterator<Seat> it = emptySeats.iterator();
		while (it.hasNext()) {
			final Seat seat = it.next();
			final Pedestrian person = seat.getSittingPerson();
			if (person != null) {
				writeSitDownEvent(person, getSeatNumber(seat));
				it.remove();
			}
		}
	}

	private int getSeatNumber(Seat seat) {
		// TODO this does not work yet: must be the seat number from 1 to 16 within one compartment!
		return 0;
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
		addValue(new IdDataKey(logEventId), new LogEventEntry(timeString, eventType, personId, seatNumber, surveyId));
	}

	private void updateTime(SimulationState state) {
		long nanos = (long) (state.getSimTimeInSec() * 1e9);
		time = time.plusNanos(nanos);
	}

}
