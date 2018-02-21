package org.vadere.state.attributes.processor;

public class AttributesLogEventProcessor extends AttributesProcessor {
	
	/**
	 * The compartment to observe. Index 0 and the last index denote
	 * half-compartments which should not be observed.
	 */
	private int compartmentIndex = 1;
	
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

	public void setCompartmentIndex(int compartmentIndex) {
		checkSealed();
		this.compartmentIndex = compartmentIndex;
	}

	public void setSurveyId(int surveyId) {
		checkSealed();
		this.surveyId = surveyId;
	}

	public void setPersonIdOffset(int personIdOffset) {
		checkSealed();
		this.personIdOffset = personIdOffset;
	}

	public void setFirstLogEventId(int firstLogEventId) {
		checkSealed();
		this.firstLogEventId = firstLogEventId;
	}
}
