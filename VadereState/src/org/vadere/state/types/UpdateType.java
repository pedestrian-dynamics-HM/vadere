package org.vadere.state.types;

public enum UpdateType {
  SEQUENTIAL,
  EVENT_DRIVEN,
  PARALLEL,
  SHUFFLE,
  PARALLEL_OPEN_CL,
  EVENT_DRIVEN_CL,
  EVENT_DRIVEN_PARALLEL;
}
