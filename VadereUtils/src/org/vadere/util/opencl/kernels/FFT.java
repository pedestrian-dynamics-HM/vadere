package org.vadere.util.opencl.kernels;

import com.nativelibs4java.opencl.CLAbstractUserProgram;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.LocalSize;

import java.io.IOException;

/** Wrapper around the OpenCL program FFT */
public class FFT extends CLAbstractUserProgram {
	public FFT(CLContext context) throws IOException {
		super(context, readRawSourceForClass(FFT.class));
	}
	public FFT(CLProgram program) throws IOException {
		super(program, readRawSourceForClass(FFT.class));
	}
	CLKernel fft_kernel;
	public synchronized CLEvent fft(CLQueue commandQueue, CLBuffer<Float > g_data, LocalSize l_dataLocalByteSize, int points_per_group, int size, int dir, int globalWorkSizes[], int localWorkSizes[], CLEvent... eventsToWaitFor) throws CLBuildException {
		if ((fft_kernel == null)) 
			fft_kernel = createKernel("fft");
		fft_kernel.setArgs(g_data, l_dataLocalByteSize, points_per_group, size, dir);
		return fft_kernel.enqueueNDRange(commandQueue, globalWorkSizes, localWorkSizes, eventsToWaitFor);
	}
}
