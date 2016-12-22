package org.vadere.simulator.models.seating.dataprocessing;

public class LogEventEntry {

	private static final String NA_STRING = "NA";

	private final String eventType;
	private final Integer extraInt;
	private final String extraString;
	private final Integer personId;
	private final Integer seatNumber;
	private final int surveyId;
	private final String time;

	public LogEventEntry(String time, String eventType, Integer personId, Integer seatNumber, int surveyId) {
		this(eventType, null, null, personId, seatNumber, surveyId, time);
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
		String[] arr = { eventType, stringOrNA(extraInt), stringOrNA(extraString), stringOrNA(personId),
				stringOrNA(seatNumber), stringOrNA(surveyId), stringOrNA(time) };
		return arr;
	}
	
	public static String[] getHeaders() {
		// $ head -n1 seating-data/data/LOG_EVENT.csv
		// (without "ID" column because it comes from IdDataKey)
		String[] headers = { "EVENT_TYPE", "EXTRA_INT", "EXTRA_STRING", "PERSON", "SEAT", "SURVEY", "TIME" };
		return headers;
	}
	
	@Override
	public String toString() {
		return String.join(" ", toStrings());
	}
	
	private String stringOrNA(Object o) {
		return o == null ? NA_STRING : o.toString();
	}

}
