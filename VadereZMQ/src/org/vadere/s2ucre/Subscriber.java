package org.vadere.s2ucre;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.zeromq.ZMQ;

public final class Subscriber {
	private final static Logger logger = LogManager.getLogger(Subscriber.class);

	private ZMQ.Socket subscriber;
	private ZMQ.Context context;
	private final String address;

	public Subscriber(@NotNull final String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public void close() {
		subscriber.close();
		context.term();
		logger.info("subscriber is closed");
	}

	public void open() {
		context = ZMQ.context(1);
		subscriber = context.socket(ZMQ.SUB);
		subscriber.connect(address);
		subscriber.subscribe("".getBytes());
	}

	public byte[] receive() {
		return subscriber.recv();
	}
}
