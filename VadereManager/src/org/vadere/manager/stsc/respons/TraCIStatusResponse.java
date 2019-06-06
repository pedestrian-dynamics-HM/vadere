package org.vadere.manager.stsc.respons;

public enum TraCIStatusResponse {

	OK(0x00),
	ERR(0xFF),
	NOT_IMPLEMENTED(0x01),
	UNKNOWN(0xaa);


	public int code;


	public static TraCIStatusResponse fromId(int code){
		for(TraCIStatusResponse status : values()){
			if (status.code == code)
				return status;
		}
		return UNKNOWN;
	}

	TraCIStatusResponse(int code) {
		this.code = code;
	}
}
