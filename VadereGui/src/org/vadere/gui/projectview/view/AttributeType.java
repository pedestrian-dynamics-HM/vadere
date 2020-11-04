package org.vadere.gui.projectview.view;

public enum AttributeType {
	SIMULATION, MODEL, PSYCHOLOGY, PEDESTRIAN, CAR, TOPOGRAPHY, OUTPUTPROCESSOR, PERCEPTION, STRATEGY;

	public final static String simulationAttributes = "simulation attributes";
	public final static String modelAttributes = "panelModel attributes";
	public final static String psychologyAttributes = "psychology attributes";
	public final static String pedestrianAttributes = "pedestrian attributes";
	public final static String scenarioAttributes = "scenario attributes";
	public final static String carAttributes = "car attributes";
	public final static String stimulusAttributes = "stimulus attributes";


	public static AttributeType fromName(String name) {
		switch (name) {
			case simulationAttributes:
				return SIMULATION;
			case modelAttributes:
				return MODEL;
			case psychologyAttributes:
				return PSYCHOLOGY;
			case pedestrianAttributes:
				return PEDESTRIAN;
			case scenarioAttributes:
				return TOPOGRAPHY;
			case carAttributes:
				return CAR;
			case stimulusAttributes:
				return PERCEPTION;
			default:
				throw new IllegalArgumentException("name " + name + " does not match any attribute type.");
		}
	}
}
