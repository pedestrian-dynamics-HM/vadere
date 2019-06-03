package org.vadere.manager.stsc;

public enum TraCIStatusResponse {

	OK(0x00),
	ERR(0xFF),
	NOT_IMPLEMENTED(0x01);


	int code;

	TraCIStatusResponse(int code) {
		this.code = code;
	}
}
