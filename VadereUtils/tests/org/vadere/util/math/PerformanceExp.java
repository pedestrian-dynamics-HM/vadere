package org.vadere.util.math;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * @author Benedikt Zoennchen
 *
 * A simple example how to write a performance test. We compare two different
 * implementations of solutions of the approximation of the exponential function.
 */
public class PerformanceExp {

	@State(Scope.Thread)
	public static class RandomDouble {
		public double value;

		@Setup(Level.Invocation)
		public void doSetup() {
			value = Math.random();
			//System.out.println("Do Setup");
		}
	}

	@Benchmark @BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void testExp(RandomDouble randomDouble) {
		double exp = Math.exp(randomDouble.value);
	}

	@Benchmark @BenchmarkMode(Mode.Throughput) @OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void testExpAp(RandomDouble randomDouble) {
		double exp = MathUtil.expAp(randomDouble.value);
	}
}
