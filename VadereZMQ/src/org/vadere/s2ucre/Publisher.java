package org.vadere.s2ucre;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.zeromq.ZMQ;

public final class Publisher {
	private final static Logger logger = LogManager.getLogger(Publisher.class);
	private ZMQ.Socket publisher;
	private ZMQ.Context context;
	private final String address;

	public Publisher(@NotNull final String address) {
		this.address = address;
	}

	public void send(byte[] data) {
		publisher.send(data);
	}

	public String getAddress() {
		return address;
	}

	public void open() {
		context = ZMQ.context(1);
		publisher = context.socket(ZMQ.PUB);
		publisher.bind(address);
	}

	public void close() {
		publisher.close();
		context.term();
	}
}