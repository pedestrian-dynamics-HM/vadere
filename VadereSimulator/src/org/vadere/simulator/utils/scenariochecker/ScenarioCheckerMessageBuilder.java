package org.vadere.simulator.utils.scenariochecker;

import org.vadere.state.scenario.ScenarioElement;

/**
 * Builder Pattern to create TopographyCheckerMessages. each call to {@link #build()} will produce a
 * new base message which can be configured with method caning
 *
 * i.E. builder.topographyWarning().element(e1).reason(r1).build();
 *
 * will create a topographyWarning for element e1 with reason r1. The call to build() will return the message
 * and creates a new internal message object for later use.
 */
public class ScenarioCheckerMessageBuilder {

	private ScenarioCheckerMessage msg;

	public ScenarioCheckerMessageBuilder() {
	}


	public ScenarioCheckerMessageBuilder topographyWarning() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.TOPOGRAPHY_WARN);
		return this;
	}

	public ScenarioCheckerMessageBuilder topographyError() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.TOPOGRAPHY_ERROR);
		return this;
	}

	public ScenarioCheckerMessageBuilder simulationAttrWarning() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.SIMULATION_ATTR_WARN);
		return this;
	}

	public ScenarioCheckerMessageBuilder simulationAttrError() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.SIMULATION_ATTR_ERROR);
		return this;
	}

	public ScenarioCheckerMessageBuilder modelAttrWarning() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.MODEL_ATTR_WARN);
		return this;
	}

	public ScenarioCheckerMessageBuilder modelAttrAttrError() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.MODEL_ATTR_ERROR);
		return this;
	}

	public ScenarioCheckerMessageBuilder dataProcessorWarning() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.DATA_PROCESSOR_WARN);
		return this;
	}

	public ScenarioCheckerMessageBuilder dataProcessorAttrError() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.DATA_PROCESSOR_ERROR);
		return this;
	}

	public ScenarioCheckerMessageBuilder perceptionAttrError() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.PERCEPTION_ATTR_ERROR);
		return this;
	}

	public ScenarioCheckerMessageBuilder perceptionAttrWarning() {
		msg = new ScenarioCheckerMessage(ScenarioCheckerMessageType.PERCEPTION_ATTR_WARN);
		return this;
	}

	public ScenarioCheckerMessageBuilder target(ScenarioElement... targets) {
		msg.setMsgTarget(new ScenarioCheckerMessageTarget(targets));
		return this;
	}


	public ScenarioCheckerMessageBuilder reason(ScenarioCheckerReason reason) {
		msg.setReason(reason);
		return this;
	}

	public ScenarioCheckerMessageBuilder reason(ScenarioCheckerReason reason, String modifier) {
		msg.setReason(reason);
		msg.setReasonModifier(modifier);
		return this;
	}

	public ScenarioCheckerMessage build() {
		ScenarioCheckerMessage ret = msg;
		msg = null;
		return ret;
	}
}
