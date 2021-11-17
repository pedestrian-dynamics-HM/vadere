package org.vadere.simulator.utils.scenariochecker;

/**
 * Types of {@link ScenarioChecker} messages. The {@link #msgId} is used as messageId for locale
 * de/en
 */
public enum ScenarioCheckerMessageType {

	SIMULATION_ATTR_ERROR("Error",100, "ScenarioChecker.type.simulation.error"),
	SIMULATION_ATTR_WARN("Warning",500, "ScenarioChecker.type.simulation.warning"),

	MODEL_ATTR_ERROR("Error",101, "ScenarioChecker.type.model.error"),
	MODEL_ATTR_WARN("Warning",501, "ScenarioChecker.type.model.warning"),

	DATA_PROCESSOR_ERROR("Error",102, "ScenarioChecker.type.processor.error"),
	DATA_PROCESSOR_WARN("Warning",502, "ScenarioChecker.type.processor.warning"),

	TOPOGRAPHY_ERROR("Error",103, "ScenarioChecker.type.topography.error"),
	TOPOGRAPHY_WARN("Warning",503, "ScenarioChecker.type.topography.warning"),

	PERCEPTION_ATTR_ERROR("Error", 104, "ScenarioChecker.type.perception.error"),
	PERCEPTION_ATTR_WARN("Warning", 504, "ScenarioChecker.type.perception.warning");


	private static final int ERROR_START = 100;
	private static final int WARN_START = 500;

	private String type;
	private String msgId;
	private int id;

	ScenarioCheckerMessageType(String type, int id, String msgId) {
		this.type = type;
		this.id = id;
		this.msgId = msgId;
	}

	public boolean isErrorMsg(){
		return type.equals("Error");
	}

	public boolean isWarnMsg(){
		return type.equals("Warning");
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
				", id=" + id +
				'}';
	}
}
