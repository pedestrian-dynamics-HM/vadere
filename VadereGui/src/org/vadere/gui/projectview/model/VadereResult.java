package org.vadere.gui.projectview.model;

import org.vadere.gui.components.utils.Messages;

public enum VadereResult {
	SUCCESSFUL, FAILED, UNAVAILABLE;

	@Override
	public String toString() {
		switch (this) {
			case SUCCESSFUL:
				return Messages.getString("Successful.text");
			case FAILED:
				return Messages.getString("Failed.text");
			case UNAVAILABLE:
				return Messages.getString("Unavailable.text");
			default:
				return this.toString();
		}
	}
}
