package org.vadere.simulator.projects.migration;

import org.vadere.simulator.projects.migration.incident.incidents.Incident;

public class MigrationException extends Exception {

	private static final long serialVersionUID = 1L;

	public MigrationException() {
	}

	public MigrationException(String message) {
		super(message);
	}

	public MigrationException(Throwable throwable) {
		super(throwable);
	}

	public MigrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public MigrationException(Incident incident, String message) {
		super(incident.getClass().getSimpleName() + ": " + message);
	}

}
