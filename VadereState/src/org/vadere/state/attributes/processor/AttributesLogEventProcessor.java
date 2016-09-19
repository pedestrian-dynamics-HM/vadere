package org.vadere.state.attributes.processor;

public class AttributesLogEventProcessor extends AttributesProcessor {
	
	private int compartmentIndex = 0;
	
	private int surveyId = 1;
	/**
	 * Offset to add to the pedestrian ID in order to prevent duplicate person
	 * IDs from different log event processors.
	 */
	private int personIdOffset = 0;
	/**
	 * Log event ID for initializing the ID counter to prevent duplicate log
	 * event IDs from different log event processors.
	 */
	private int firstLogEventId = 1;

	public int getCompartmentIndex() {
		return compartmentIndex;
	}

	public int getSurveyId() {
		return surveyId;
	}

	public int getPersonIdOffset() {
		return personIdOffset;
	}

	public int getFirstLogEventId() {
		return firstLogEventId;
	}

}
