package org.vadere.simulator.util;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.ScenarioElement;

import java.util.Comparator;

public class TopographyCheckerMessage implements Comparable<TopographyCheckerMessage> {


	// label of enum is the MessageId for locale de/en. Multiple instances of the same error will
	// have the same reason.
	private TopographyCheckerReason reason;
	// variable part of the reason. This will be concatenated to the reason on display time.
	private String reasonModifier;
	// error or warning. A scenario with an error cannot be simulated.
	private TopographyCheckerMessageType msgType;
	// element producing the error / warning
	private TopographyCheckerMessageTarget msgTarget;


	public TopographyCheckerMessage(TopographyCheckerMessageType type) {
		msgType = type;
		msgTarget = null;
		reasonModifier = "";
	}

	public TopographyCheckerReason getReason() {
		return reason;
	}

	public void setReason(TopographyCheckerReason reason) {
		this.reason = reason;
	}

	public TopographyCheckerMessageType getMsgType() {
		return msgType;
	}

	public void setMsgType(TopographyCheckerMessageType msgType) {
		this.msgType = msgType;
	}

	public TopographyCheckerMessageTarget getMsgTarget() {
		return msgTarget;
	}

	public void setMsgTarget(TopographyCheckerMessageTarget msgTarget) {
		this.msgTarget = msgTarget;
	}

	public String getReasonModifier() {
		return reasonModifier;
	}

	public void setReasonModifier(String reasonModifier) {
		this.reasonModifier = reasonModifier;
	}

	private Comparator<TopographyCheckerMessage> sortByType() {
		return (o1, o2) -> {
			if (o1.equals(o2))
				return 0;
			if (o1.getMsgType().ordinal() > o2.getMsgType().ordinal()) {
				return 1;
			} else if (o1.getMsgType().ordinal() < o2.getMsgType().ordinal()) {
				return -1;
			} else {
				return 0;
			}
		};
	}
//
//	private Comparator<TopographyCheckerMessage> sortByElement() {
//		return (o1, o2) -> {
//			if (o1.element.equals(o2.element))
//				return 0;
//
//			if (o1.element.getType().ordinal() > o2.element.getType().ordinal()) {
//				return 1;
//			} else if (o1.element.getType().ordinal() < o2.element.getType().ordinal()) {
//				return -1;
//			} else {
//				return 0;
//			}
//		};
//	}

	private Comparator<TopographyCheckerMessage> defaultSort() {
		return sortByType();
	}

	@Override
	public int compareTo(@NotNull TopographyCheckerMessage o) {
		return defaultSort().compare(this, o);
	}
}
