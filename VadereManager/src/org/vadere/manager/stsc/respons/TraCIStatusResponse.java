package org.vadere.manager.stsc.respons;

import org.vadere.manager.TraCIException;

public enum TraCIStatusResponse {

	OK(0x00),
	ERR(0xFF),
	NOT_IMPLEMENTED(0x01),
	;

	public int id;

	public static TraCIStatusResponse fromId(int id){
		for(TraCIStatusResponse status : values()){
			if (status.id == id)
				return status;
		}
		throw new TraCIException(String.format("No status id found with id: %02X", id));
	}

	TraCIStatusResponse(int id) {
		this.id = id;
	}
}
