package org.vadere.gui.postvisualization.control;


import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.util.logging.Logger;

public class Player implements Runnable {
	private static Logger logger = Logger.getLogger(Player.class);
	private static volatile Player instance;
	private Thread currentThread;

	enum State {
		STOPPED, PAUSED, RUNNING
	}

	private int sleepTimeMS;
	private State state;
	private PostvisualizationModel model;
	private boolean running = false;

	public static Player getInstance(final PostvisualizationModel model) {
		if (instance == null) {
			instance = new Player(model);
		}
		instance.model = model;
		return instance;
	}

	private Player(final PostvisualizationModel model) {
		this.model = model;
		this.currentThread = null;
		state = State.STOPPED;
	}

	public void pause() {
		state = State.PAUSED;
	}

	public void stop() {
		state = State.STOPPED;
		running = false;

		if (currentThread != null) {
			currentThread.interrupt();
		}

		synchronized (model) {
			if (model.getStepCount() >= 1) {
				model.setStep(0);
				model.notifyObservers();
			}
		}
	}

	public void start() {
		synchronized (model) {
			if (!running) {
				running = true;
				currentThread = new Thread(this);
				currentThread.start();
			}
			state = State.RUNNING;
			synchronized (model) {
				model.notifyAll();
			}
		}
	}

	public static void kill() {
		if (instance != null) {
			instance.stop();
			instance.running = false;
			logger.info("Player killed!");
		}
	}

	private boolean isRunnable() {
		return !model.isEmpty();
	}

	@Override
	public void run() {
		long diffMs = 0;
		while (running) {
			long ms = System.currentTimeMillis();
			// synchronized (model) {
			switch (state) {
				case RUNNING: {
					if (isRunnable()) {
						double newSimeTimeInSec = model.getSimTimeInSec() + model.getTimeResolution();
						if(model.getSimTimeInSec() >= model.getMaxSimTimeInSec()) {
							newSimeTimeInSec = 0;
						}
						model.setVisTime(newSimeTimeInSec);
					}
				}
				model.notifyObservers();
				break;

				case PAUSED: {
					synchronized (model) {
						try {
							model.wait();
						} catch (InterruptedException e) {
							logger.info("Player interrupted while waiting (paused)");
						}
					}
				}
				break;
				default: break;
			}
			// }
			diffMs = System.currentTimeMillis() - ms;
			sleepTimeMS = (int) Math.round((1000.0 / model.config.getFps() - diffMs));
			if(sleepTimeMS > 0) {
				try {
					Thread.sleep(Math.max(0, sleepTimeMS));
				} catch (InterruptedException e) {
					logger.info("Player interrupted while sleeping");
				}
			}
		}
	}

}
