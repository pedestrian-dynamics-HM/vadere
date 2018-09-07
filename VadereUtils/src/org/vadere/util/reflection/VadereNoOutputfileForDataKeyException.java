package org.vadere.util.reflection;

public class VadereNoOutputfileForDataKeyException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public VadereNoOutputfileForDataKeyException(Throwable cause){
		super(cause);
	}

	public VadereNoOutputfileForDataKeyException(String message){
		super(message);
	}

	public VadereNoOutputfileForDataKeyException(){

	}
}
