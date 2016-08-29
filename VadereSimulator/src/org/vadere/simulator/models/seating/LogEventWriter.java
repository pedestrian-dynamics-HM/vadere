package org.vadere.simulator.models.seating;

import java.io.OutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.processors.Processor;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
import org.vadere.state.attributes.processors.AttributesWriter;
import org.vadere.state.scenario.Pedestrian;

public class LogEventWriter extends ProcessorWriter {
	
	private static final String INITIALIZATION_END_EVENT = "INITIALIZATION_END";
	private static final String SIT_DOWN_EVENT = "SIT_DOWN";
	
	private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME; // 15:20:00

	private TrainModel trainModel;
	private LocalTime time;
	private int nextLogEventId = 1;

	public LogEventWriter(OutputStream out, Processor processor, AttributesWriter attributes) {
		super(out, processor, attributes);
		// TODO must be parameterized with the compartment to observe
	}

	@Override
	public void preLoop(SimulationState state) {
		super.preLoop(state);

		// TODO implement
//		final SeatingModel seatingModel = (SeatingModel) state.getMainModel();
//		trainModel = seatingModel.getTrainModel();

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

	private void writeInitialSitDownEvents() {
		for (Seat seat : trainModel.getSeats()) {
			Pedestrian person = seat.getSittingPerson();
			if (person != null) {
				// TODO this does not work yet: 2nd param must be the seat number from 1 to 16 within one compartment!
				writeSitDownEvent(person, seat.getAssociatedTarget().getId());
			}
		}
	}

	private void writeNewSitDownEvents() {
		// TODO find persons that just sat down
		//for each seat which was empty before:
		//writeSitDownEvent(seat.getSittingPerson(), seatNumber);
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
