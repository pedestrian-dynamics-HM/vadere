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

	public synchronized void send(byte[] data) {
		publisher.send(data);
	}

	public synchronized String getAddress() {
		return address;
	}

	public synchronized void open() {
		logger.debug("try to open publisher " + address);
		context = ZMQ.context(1);
		publisher = context.socket(ZMQ.PUB);
		publisher.bind(address);
		logger.debug("publisher opened " + address);
	}

	public synchronized void close() {
		logger.debug("try to close publisher " + address);
//		publisher.unbind(address);
		publisher.close();
		context.term();
		logger.debug("publisher closed " + address);
	}
}