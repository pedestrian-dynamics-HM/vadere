package org.vadere.util.parallel;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * States methods for a parallel worker and defines the internal class Work.
 * 
 */
public abstract class AParallelWorker<ResultType> {
	/**
	 * Internal class representing the work the {@link AParallelWorker} should
	 * do. Implementation is done by the classes that use the
	 * {@link AParallelWorker}.
	 * 
	 * 
	 */
	public interface Work<T> extends Callable<T> {
		public void setID(int ID);

		public int getWorkerID();
	}

	protected final Work<ResultType> work;
	protected Future<ResultType> result;

	public AParallelWorker(Work<ResultType> work) {
		this.work = work;
	}

	/**
	 * Starts the work asynchronously.
	 */
	public abstract void start();

	/**
	 * Finishes the given parallel operation and returns the result. This pauses
	 * the calling thread.
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public abstract ResultType finish() throws InterruptedException,
			ExecutionException;
}
