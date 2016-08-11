package org.vadere.gui.projectview.model;

import org.vadere.gui.components.utils.Messages;

public enum VadereState {
	RUNNING, PAUSED, INTERRUPTED, INITIALIZED;

	@Override
	public String toString() {
		switch (this) {
			case RUNNING:
				return Messages.getString("Running.text");
			case PAUSED:
				return Messages.getString("Paused.text");
			case INTERRUPTED:
				return Messages.getString("Interrupted.text");
			case INITIALIZED:
				return Messages.getString("Initialized.text");
			default:
				return this.toString();
		}
	}
}
