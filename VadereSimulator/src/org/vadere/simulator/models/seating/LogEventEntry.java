package org.vadere.simulator.models.seating;

public class LogEventEntry {
	private static final int defaultSurveyId = 0;
	private final String eventType;
	private final Integer extraInt;
	private final String extraString;
	private final Integer personId;
	private final Integer seatNumber;
	private final int surveyId;
	private final String time;

	public LogEventEntry(String time, String eventType, Integer personId, Integer seatNumber) {
		this(eventType, null, null, personId, seatNumber, defaultSurveyId, time);
	}

	public LogEventEntry(String eventType, Integer extraInt, String extraString, Integer personId, Integer seatNumber,
			int surveyId, String time) {
		this.eventType = eventType;
		this.extraInt = extraInt;
		this.extraString = extraString;
		this.personId = personId;
		this.seatNumber = seatNumber;
		this.surveyId = surveyId;
		this.time = time;
	}
	
	public String[] toStrings() {
		// TODO implement
		String[] arr = {eventType};
		return arr;
	}

}
