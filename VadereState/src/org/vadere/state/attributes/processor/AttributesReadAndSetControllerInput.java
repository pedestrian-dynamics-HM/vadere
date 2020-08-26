package org.vadere.state.attributes.processor;

public class AttributesReadAndSetControllerInput extends AttributesProcessor {
	private String controllerInputFile = null;
	private boolean useFile = false;


	public String getControllerInputFile() {
		return controllerInputFile;
	}

	public void setControllerInputFile(String controllerInputFile) {
		this.controllerInputFile = controllerInputFile;
	}

	public boolean isUseFile() {
		return useFile;
	}

	public void setUseFile(boolean useFile) {
		this.useFile = useFile;
	}
}
