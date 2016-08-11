package org.vadere.gui.postvisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class Player implements Runnable {
	private static Logger logger = LogManager.getLogger(Player.class);
	private static volatile Player instance;
	private Thread currentThread;
	private int currentStep;

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
		this.currentStep = 1;
		this.currentThread = null;
		state = State.STOPPED;
	}

	public void pause() {
		state = State.PAUSED;
	}

	public void stop() {
		state = State.STOPPED;
		running = false;
		currentStep = 1;

		if (currentThread != null) {
			currentThread.interrupt();
		}

		synchronized (model) {
			if (model.getStepCount() >= 1) {
				model.setStep(1);
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

			model.getStep().ifPresent(s -> currentStep = s.getStepNumber());
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

	private boolean isRunable() {
		return model.getStep().isPresent() && model.getFirstStep().isPresent() && model.getLastStep().isPresent();
	}

	@Override
	public void run() {
		long diffMs = 0;
		while (running) {
			long ms = System.currentTimeMillis();
			// synchronized (model) {
			switch (state) {
				case RUNNING: {
					if (isRunable()) {
						if (model.getLastStep().get().getStepNumber() > model.getStep().get().getStepNumber()) {
							currentStep = model.getStep().get().getStepNumber() + 1;
							model.setStep(currentStep);
						} else {
							pause();
						}
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
				default:
					break;
			}
			// }
			diffMs = System.currentTimeMillis() - ms;
			sleepTimeMS = (int) Math.round((1000.0 - diffMs) / model.config.getFps());
			try {
				Thread.sleep(Math.max(0, sleepTimeMS));
			} catch (InterruptedException e) {
				logger.info("Player interrupted while sleeping");
			}
		}
	}

}
