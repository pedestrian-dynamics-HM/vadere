package org.vadere.util.logging;

public class Timer {

	private long runtime;
	private long startTime;

	/**
	 * Starts the timer.
	 */
	public void start() {
		runtime = 0;
		startTime = System.nanoTime();
	}

	/**
	 * Pause the timer, so the timer stop adding run time.
	 */
	public void pause() {
		runtime += System.nanoTime() - startTime;
	}

	/**
	 * Unpause the timer, so the timer continue adding run time.
	 */
	public void unpause() {
		startTime = System.nanoTime();
	}

	/**
	 * Stops the timer, so the timer stop adding run time and reset the calculated runtime to 0.
	 *
	 * @return returns the calculated run time
	 */
	public double end() {
		pause();
		runtime += System.nanoTime() - startTime;
		long result = runtime;
		return result;
	}

	@Override
	public String toString() {
		return "runtime: " + runtime / 1000000.0 + "[msec]";
	}
}
