package org.vadere.state.attributes.processor;

public class AttributesLogEventProcessor extends AttributesProcessor {
	
	private int compartmentIndex = 0;
	
	private int surveyId = 1;
	private int personIdOffset = 0;
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
