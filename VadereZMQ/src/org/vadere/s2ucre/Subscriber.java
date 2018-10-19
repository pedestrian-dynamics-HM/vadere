package org.vadere.s2ucre;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.zeromq.ZMQ;

import static zmq.ZMQ.ZMQ_DONTWAIT;

public final class Subscriber {
	private final static Logger logger = LogManager.getLogger(Subscriber.class);

	private ZMQ.Socket subscriber;
	private ZMQ.Context context;
	private final String address;
	private boolean closed;

	public Subscriber(@NotNull final String address) {
		this.address = address;
		this.closed = true;
	}

	public String getAddress() {
		return address;
	}

	public synchronized void close() {
		closed = true;
		logger.debug("try to close subscriber " + address);
		subscriber.disconnect(address);
		logger.debug("subscriber is unsubscribed + " + address);
		subscriber.close();
		context.term();
		logger.debug("subscriber closed " + address);
	}

	public synchronized void open() {
		logger.debug("try to open subscriber " + address);
		context = ZMQ.context(1);
		subscriber = context.socket(ZMQ.SUB);
		subscriber.connect(address);
		subscriber.subscribe("".getBytes());
		logger.debug("subscriber opened " + address);
		closed = false;
	}

	/**
	 * We use a non-block strategy to cleanly interrupt the receive.
	 *
	 * @return
	 */
	public synchronized byte[] receive() throws InterruptedException {
		byte[] data = null;
		while (data == null && !closed) {
			data = subscriber.recv(ZMQ_DONTWAIT);
			Thread.sleep(10);
		}
		return data;
	}
}
