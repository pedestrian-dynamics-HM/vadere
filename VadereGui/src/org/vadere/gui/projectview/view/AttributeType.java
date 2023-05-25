package org.vadere.gui.projectview.view;

public enum AttributeType {
  SIMULATION,
  MODEL,
  PSYCHOLOGY,
  PEDESTRIAN,
  CAR,
  TOPOGRAPHY,
  OUTPUTPROCESSOR,
  PERCEPTION,
  STRATEGY;

  public static final String simulationAttributes = "simulation attributes";
  public static final String modelAttributes = "panelModel attributes";
  public static final String psychologyAttributes = "psychology attributes";
  public static final String pedestrianAttributes = "pedestrian attributes";
  public static final String scenarioAttributes = "scenario attributes";
  public static final String carAttributes = "car attributes";
  public static final String stimulusAttributes = "stimulus attributes";

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
