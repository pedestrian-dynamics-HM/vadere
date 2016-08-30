package org.vadere.simulator.models.seating;

import java.io.OutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.processors.Processor;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
import org.vadere.state.attributes.processors.AttributesWriter;
import org.vadere.state.scenario.Pedestrian;

/**
 * Produce output in same format as LOG_EVENT.csv from the SeatingDataCollection app.
 * 
 * See *_EVENT constants for supported log event types.
 *
 */
public class LogEventWriter extends ProcessorWriter {
	
	// These fields come from the enum LogEventType from the SeatingDataCollection app:
	private static final String INITIALIZATION_END_EVENT = "INITIALIZATION_END";
	private static final String SIT_DOWN_EVENT = "SIT_DOWN";
	
	private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME; // 15:20:00

	private TrainModel trainModel;
	private LocalTime time;
	private int nextLogEventId = 1;
	private List<Seat> emptySeats;

	public LogEventWriter(OutputStream out, Processor processor, AttributesWriter attributes) {
		super(out, processor, attributes);
		// TODO must be parameterized with the compartment to observe
	}

	@Override
	public void preLoop(SimulationState state) {
		super.preLoop(state);

		trainModel = getTrainModelFromSimulationState(state);
		emptySeats = new LinkedList<>(trainModel.getSeats()); // will be filtered in writeNewSitDownEvents()
		time = LocalTime.now();

		writeInitialSitDownEvents();
		writeInitializationEndEvent();
	}

	@Override
	public void update(SimulationState state) {
		super.update(state);
		updateTime(state);
		writeNewSitDownEvents();
	}

	private TrainModel getTrainModelFromSimulationState(SimulationState state) {
		// TODO implement
//		final SeatingModel seatingModel = (SeatingModel) state.getMainModel();
		final SeatingModel seatingModel = null;
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
				// TODO this does not work yet: 2nd param must be the seat number from 1 to 16 within one compartment!
				writeSitDownEvent(person, seat.getAssociatedTarget().getId());
				it.remove();
			}
		}
	}

	private void writeInitializationEndEvent() {
		writeEvent(INITIALIZATION_END_EVENT, null, null);
	}

	private void writeSitDownEvent(Pedestrian person, int seatNumber) {
		writeEvent(SIT_DOWN_EVENT, person.getId(), seatNumber);
	}

	private void writeEvent(String eventType, Integer personId, Integer seatNumber) {
		final int logEventId = nextLogEventId++;
		final String timeString = timeFormatter.format(time);
		// TODO implement:
		//add(logEventId, timeString, eventType, personId, seatNumber)
	}

	private void updateTime(SimulationState state) {
		long nanos = (long) (state.getSimTimeInSec() * 1e9);
		time = time.plusNanos(nanos);
	}

}
