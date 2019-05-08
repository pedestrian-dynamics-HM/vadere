package org.vadere.simulator.control;




import org.vadere.util.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ScenarioExecutorService extends ThreadPoolExecutor {

	private static Logger log = Logger.getLogger(ScenarioExecutorService.class);
//	private HashMap<String, Throwable> output

	public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
		return new ScenarioExecutorService(nThreads, nThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(),
				threadFactory);
	}

	public ScenarioExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public ScenarioExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public ScenarioExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public ScenarioExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}


	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if (t != null) {
			// todo Throwable von scenario run abolen
			log.info("Error in execution found...");
			t.printStackTrace();
		} else {
			log.info("Execution worked");
		}
		super.afterExecute(r, t);
	}
}
