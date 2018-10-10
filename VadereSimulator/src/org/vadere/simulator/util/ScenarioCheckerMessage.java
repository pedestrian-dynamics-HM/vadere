package org.vadere.simulator.util;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class ScenarioCheckerMessage implements Comparable<ScenarioCheckerMessage> {


	// label of enum is the MessageId for locale de/en. Multiple instances of the same topographyError will
	// have the same reason.
	private ScenarioCheckerReason reason;
	// variable part of the reason. This will be concatenated to the reason on display time.
	private String reasonModifier;
	// topographyError or topographyWarning. A scenario with an topographyError cannot be simulated.
	private ScenarioCheckerMessageType msgType;
	// element producing the topographyError / topographyWarning
	private ScenarioCheckerMessageTarget msgTarget;


	public ScenarioCheckerMessage(ScenarioCheckerMessageType type) {
		msgType = type;
		msgTarget = null;
		reasonModifier = "";
	}

	public ScenarioCheckerReason getReason() {
		return reason;
	}

	public void setReason(ScenarioCheckerReason reason) {
		this.reason = reason;
	}

	public ScenarioCheckerMessageType getMsgType() {
		return msgType;
	}

	public void setMsgType(ScenarioCheckerMessageType msgType) {
		this.msgType = msgType;
	}

	public ScenarioCheckerMessageTarget getMsgTarget() {
		return msgTarget;
	}

	public void setMsgTarget(ScenarioCheckerMessageTarget msgTarget) {
		this.msgTarget = msgTarget;
	}

	public String getReasonModifier() {
		return reasonModifier;
	}

	public void setReasonModifier(String reasonModifier) {
		this.reasonModifier = reasonModifier;
	}

	public boolean isMessageForAllElements(Integer... ids){
		if (hasTarget()){
			return msgTarget.affectsAllTargets(ids);
		} else {
			return  false;
		}
	}

	public boolean hasTarget(){
		return msgTarget != null;
	}

	private Comparator<ScenarioCheckerMessage> sortByType() {
		return (o1, o2) -> {
			if (o1.equals(o2))
				return 0;
			if (o1.getMsgType().getId() > o2.getMsgType().getId()) {
				return 1;
			} else if (o1.getMsgType().getId() < o2.getMsgType().getId()) {
				return -1;
			} else {
				return 0;
			}
		};
	}

	private Comparator<ScenarioCheckerMessage> defaultSort() {
		return sortByType();
	}

	@Override
	public int compareTo(@NotNull ScenarioCheckerMessage o) {
		return defaultSort().compare(this, o);
	}

	@Override
	public String toString() {
		return "ScenarioCheckerMessage{" +
				"reason=" + reason +
				", reasonModifier='" + reasonModifier + '\'' +
				", msgType=" + msgType +
				", msgTarget=" + msgTarget +
				'}';
	}
}
