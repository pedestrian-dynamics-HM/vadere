package org.vadere.gui.projectview.view;

public enum AttributeType {
	SIMULATION, MODEL, PEDESTRIAN, CAR, TOPOGRAPHY, OUTPUTPROCESSOR, EVENT;

	public final static String simulationAttributes = "simulation attributes";
	public final static String modelAttributes = "panelModel attributes";
	public final static String pedestrianAttributes = "pedestrian attributes";
	public final static String scenarioAttributes = "scenario attributes";
	public final static String carAttributes = "car attributes";
	public final static String eventAttributes = "event attributes";

	public static AttributeType fromName(String name) {
		switch (name) {
			case simulationAttributes:
				return SIMULATION;
			case modelAttributes:
				return MODEL;
			case pedestrianAttributes:
				return PEDESTRIAN;
			case scenarioAttributes:
				return TOPOGRAPHY;
			case carAttributes:
				return CAR;
			case eventAttributes:
				return EVENT;
			default:
				throw new IllegalArgumentException("name " + name + " does not match any attribute type.");
		}
	}
}
