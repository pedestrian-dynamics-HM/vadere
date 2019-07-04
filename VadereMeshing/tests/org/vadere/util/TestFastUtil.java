package org.vadere.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;


//https://www.retit.de/continuous-benchmarking-with-jmh-and-junit-2/
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 4)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode({Mode.AverageTime})
public class TestFastUtil {

	private static Logger logger = Logger.getLogger(TestFastUtil.class);

	@State(Scope.Benchmark)
	public static class BenchmarkState {
		@Param({ "1000", "10000", "100000", "1000000", })
		public int arraySize;
	}

	@Benchmark
	public void memoryUsageDoubleArray(BenchmarkState state) {
		int size = state.arraySize;

		//long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		Random random = new Random();
		DoubleArrayList numbers = new DoubleArrayList(size);
		for(int i = 0; i < size; i++) {
			double number = random.nextDouble();
			numbers.add(number);
		}

		//long afterUseMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		//logger.info("Memory usage [FastUtil] = " + (afterUseMem - beforeUsedMem) / 1000.0 + " [KB]");
	}

	@Benchmark
	public void memoryUsageArray(BenchmarkState state) {
		int size = state.arraySize;

		//long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
			Random random = new Random();
			ArrayList<Double> numbers = new ArrayList<>(size);
			for(int i = 0; i < size; i++) {
				double number = random.nextDouble();
				numbers.add(number);
			}

		//long afterUseMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		//logger.info("Memory usage [SDK] = " + (afterUseMem - beforeUsedMem) / 1000.0 + " [KB]");
	}
}
