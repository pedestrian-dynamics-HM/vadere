package org.vadere.state.attributes.processor;

import java.util.ArrayList;

public class AttributesMeshPedStimulusCountingProcessor extends AttributesMeshDensityCountingProcessor {

	private String informationFilter = "";
	private boolean isRegexFilter = false;

	public String getInformationFilter() {
		return informationFilter;
	}

	public void setInformationFilter(String informationFilter) {
		checkSealed();
		this.informationFilter = informationFilter;
	}

	public boolean isRegexFilter() {
		return isRegexFilter;
	}

	public void setRegexFilter(boolean regexFilter) {
		checkSealed();
		isRegexFilter = regexFilter;
	}
}
