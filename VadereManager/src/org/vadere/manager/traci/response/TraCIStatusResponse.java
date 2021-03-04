package org.vadere.manager.traci.response;

import org.vadere.state.traci.TraCIException;

/**
 * Status Codes for each Response send from a TraCI-Server. These codes are wrapped within a {@link
 * TraCIStatusResponse} response send back to the client. Depending on the command an additional
 * response appended.
 *
 * See {@link TraCIResponse} for more information.
 */
public enum TraCIStatusResponse {

	OK(0x00),
	ERR(0xFF),
	NOT_IMPLEMENTED(0x01),
	;

	public int id;

	TraCIStatusResponse(int id) {
		this.id = id;
	}

	public static TraCIStatusResponse fromId(int id) {
		for (TraCIStatusResponse status : values()) {
			if (status.id == id)
				return status;
		}
		throw new TraCIException(String.format("No status id found with id: %02X", id));
	}
}
