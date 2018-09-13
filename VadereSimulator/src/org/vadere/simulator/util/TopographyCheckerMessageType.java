package org.vadere.simulator.util;

/**
 * Types of {@link TopographyChecker} messages. The {@link #msgId} is used as messageId for locale
 * de/en
 */
public enum TopographyCheckerMessageType {

	ERROR("Error", "TopographyChecker.type.error"),
	WARN("Warning", "TopographyChecker.type.warning");

	private String type;
	private String msgId;

	TopographyCheckerMessageType(String type, String msgId) {
		this.type = type;
		this.msgId = msgId;
	}

	public String getType() {
		return type;
	}

	public String getLocalTypeId() {
		return msgId;
	}
}
