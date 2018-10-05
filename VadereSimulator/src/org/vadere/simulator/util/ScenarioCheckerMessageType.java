package org.vadere.simulator.util;

import java.util.Comparator;

/**
 * Types of {@link ScenarioChecker} messages. The {@link #msgId} is used as messageId for locale
 * de/en
 */
public enum ScenarioCheckerMessageType {

	TOPOGRAPHY_ERROR("Error",100, "ScenarioChecker.type.error"),
	TOPOGRAPHY_WARN("Warning",500, "ScenarioChecker.type.warning"),
	SIMULATION_ATTR_ERROR("Error",101, "ScenarioChecker.type.error"),
	SIMULATION_ATTR_WARN("Warning",501, "ScenarioChecker.type.warning"),
	MODEL_ATTR_ERROR("Error",102, "ScenarioChecker.type.error"),
	MODEL_ATTR_WARN("Warning",502, "ScenarioChecker.type.warning"),
	DATA_PROCESSOR_ERROR("Error",103, "ScenarioChecker.type.error"),
	DATA_PROCESSOR_WARN("Warning",503, "ScenarioChecker.type.warning");

	private String type;
	private String msgId;
	private int id;

	ScenarioCheckerMessageType(String type,int id, String msgId) {
		this.type = type;
		this.id = id;
		this.msgId = msgId;
	}

	public String getType() {
		return type;
	}

	public String getLocalTypeId() {
		return msgId;
	}

	public int getId(){ return  id;}

	@Override
	public String toString() {
		return "ScenarioCheckerMessageType{" +
				"type='" + type + '\'' +
				'}';
	}
}
