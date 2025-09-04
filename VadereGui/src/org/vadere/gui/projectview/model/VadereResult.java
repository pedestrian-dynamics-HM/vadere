package org.vadere.gui.projectview.model;

import org.vadere.gui.components.utils.Localization;

public enum VadereResult {
	SUCCESSFUL, FAILED, UNAVAILABLE;

	@Override
	public String toString() {
		switch (this) {
			case SUCCESSFUL:
				return Localization.getString("Successful.text");
			case FAILED:
				return Localization.getString("Failed.text");
			case UNAVAILABLE:
				return Localization.getString("Unavailable.text");
			default:
				return this.toString();
		}
	}
}
