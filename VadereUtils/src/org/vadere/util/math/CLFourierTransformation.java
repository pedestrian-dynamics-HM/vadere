package org.vadere.util.math;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

import org.apache.commons.math3.complex.Complex;
import org.bridj.Pointer;
import org.vadere.util.opencl.kernels.DFT;

import java.io.IOException;
import java.nio.FloatBuffer;

public class CLFourierTransformation {

	private CLQueue queue;
	private CLContext context;
	private DFT program;

	public CLFourierTransformation() throws IOException {
		// Create a command queue, if possible able to execute multiple jobs in parallel
		// (out-of-order queues will still respect the CLEvent chaining)
		this(JavaCL.createBestContext().createDefaultOutOfOrderQueueIfPossible());
	}

	public CLFourierTransformation(final CLQueue queue) throws IOException {
		this.queue = queue;
		this.context = queue.getContext();
		this.program = new DFT(context);
	}

	public synchronized Pointer<Float> dft(final FloatBuffer in, final boolean forward) {
		assert in.capacity() % 2 == 0;
		int length = in.capacity() / 2;

		CLBuffer<Float> inBuf = context.createFloatBuffer(CLMem.Usage.Input, in, true);
		CLBuffer<Float> outBuf = context.createFloatBuffer(CLMem.Usage.Output, length * 2);

		CLEvent dftEvent = program.dft(queue, inBuf, outBuf, length, forward ? 1 : -1, new int[]{length}, null);
		queue.finish();
		dftEvent.waitFor();
		return outBuf.read(queue, dftEvent);
	}

	public Complex[] dft(final double[] realValues, final boolean forward) {
		return dft(MathUtil.toComplex(realValues), forward);
	}

	public Complex[] dft(final Complex[] complexValues, final boolean forward) {
		Pointer<Float> outBuffer = dft(FloatBuffer.wrap(MathUtil.toFloat(complexValues)), forward);
		Complex[] transformedComplexValues = new Complex[complexValues.length];

		for(int i = 0; i < complexValues.length*2; i += 2) {
			transformedComplexValues[i/2] = Complex.valueOf(outBuffer.get(i), outBuffer.get(i+1));
		}
		outBuffer.release();
		return transformedComplexValues;
	}
}
