package org.vadere.util.parallel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for parallelization of tasks. Defines functions to setup an
 * ExecutorService (thread pool).
 * 
 * 
 */
public class ParallelWorkerUtil {
	private static volatile ExecutorService executerService = null;

	public static void setup(int workers) {
		if (executerService == null) {
			executerService = Executors.newFixedThreadPool(workers);
		} else {
			// throw new
			// IllegalAccessError("A previous setup of the parallel workers was performed.");
		}
	}

	public static ExecutorService getThreadPool() {
		if (executerService == null) {
			throw new IllegalAccessError("No thread pool was setup.");
		}

		return executerService;
	}

	public static void shutdown() {
		if (executerService != null) {
			executerService.shutdownNow();
		}
		executerService = null;
	}
}
