package org.vadere.simulator.util;

import org.vadere.state.scenario.ScenarioElement;

/**
 * Builder Pattern to create TopographyCheckerMessages. each call to {@link #build()} will produce a
 * new base message which can be configured with method caning
 *
 * i.E. builder.warning().element(e1).reason(r1).build();
 *
 * will create a warning for element e1 with reason r1. The call to build() will return the message
 * and creates a new internal message object for later use.
 */
public class TopographyCheckerMessageBuilder {

	private TopographyCheckerMessage msg;

	public TopographyCheckerMessageBuilder() {
	}


	public TopographyCheckerMessageBuilder warning() {
		msg = new TopographyCheckerMessage(TopographyCheckerMessageType.WARN);
		return this;
	}

	public TopographyCheckerMessageBuilder error() {
		msg = new TopographyCheckerMessage(TopographyCheckerMessageType.ERROR);
		return this;
	}

	public TopographyCheckerMessageBuilder element(ScenarioElement element) {
		msg.setElement(element);
		return this;
	}

	public TopographyCheckerMessageBuilder reason(TopographyCheckerReason reason) {
		msg.setReason(reason);
		return this;
	}

	public TopographyCheckerMessageBuilder reason(TopographyCheckerReason reason, String modifier) {
		msg.setReason(reason);
		msg.setReasonModifier(modifier);
		return this;
	}

	public TopographyCheckerMessage build() {
		TopographyCheckerMessage ret = msg;
		msg = null;
		return ret;
	}
}
