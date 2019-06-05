package org.vadere.manager.stsc;

public abstract class Packet {


	public abstract byte[] send();

	public abstract byte[] extractCommandsOnly();

}
