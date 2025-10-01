package org.vadere.gui.projectview.model;

import org.vadere.gui.components.utils.Localization;

public enum VadereState {
	RUNNING, PAUSED, INTERRUPTED, INITIALIZED, STEP;

	@Override
	public String toString() {
		switch (this) {
			case RUNNING:
				return Localization.getString("Running.text");
			case PAUSED:
				return Localization.getString("Paused.text");
			case INTERRUPTED:
				return Localization.getString("Interrupted.text");
			case INITIALIZED:
				return Localization.getString("Initialized.text");
			case STEP:
				return Localization.getString("Step.text");
			default:
				throw new IllegalStateException("VadereState. Should not be reached. All enums already tested.");
		}
	}
}
