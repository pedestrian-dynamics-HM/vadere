package org.vadere.util.opencl.kernels;

import com.nativelibs4java.opencl.CLAbstractUserProgram;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;

import java.io.IOException;

/** Wrapper around the OpenCL program Convolve */
public class Convolve extends CLAbstractUserProgram {
	public Convolve(CLContext context) throws IOException {
		super(context, readRawSourceForClass(Convolve.class));
	}
	public Convolve(CLProgram program) throws IOException {
		super(program, readRawSourceForClass(Convolve.class));
	}
	CLKernel convolve_kernel;
	public synchronized CLEvent convolve(CLQueue commandQueue, CLBuffer<Float > pInput, CLBuffer<Float > pFilter, CLBuffer<Float > pOutput, int nInWidth, int nInHeight, int nFilterWidth, int globalWorkSizes[], int localWorkSizes[], CLEvent... eventsToWaitFor) throws CLBuildException {
		if ((convolve_kernel == null)) 
			convolve_kernel = createKernel("convolve");
		convolve_kernel.setArgs(pInput, pFilter, pOutput, nInWidth, nInHeight, nFilterWidth);
		return convolve_kernel.enqueueNDRange(commandQueue, globalWorkSizes, localWorkSizes, eventsToWaitFor);
	}
	CLKernel convolveRow_kernel;
	public synchronized CLEvent convolveRow(CLQueue commandQueue, CLBuffer<Float > pInput, CLBuffer<Float > pFilter, CLBuffer<Float > pOutput, int nInWidth, int nInHeight, int nFilterWidth, int globalWorkSizes[], int localWorkSizes[], CLEvent... eventsToWaitFor) throws CLBuildException {
		if ((convolveRow_kernel == null)) 
			convolveRow_kernel = createKernel("convolveRow");
		convolveRow_kernel.setArgs(pInput, pFilter, pOutput, nInWidth, nInHeight, nFilterWidth);
		return convolveRow_kernel.enqueueNDRange(commandQueue, globalWorkSizes, localWorkSizes, eventsToWaitFor);
	}
	CLKernel convolveCol_kernel;
	public synchronized CLEvent convolveCol(CLQueue commandQueue, CLBuffer<Float > pInput, CLBuffer<Float > pFilter, CLBuffer<Float > pOutput, int nInWidth, int nInHeight, int nFilterWidth, int globalWorkSizes[], int localWorkSizes[], CLEvent... eventsToWaitFor) throws CLBuildException {
		if ((convolveCol_kernel == null)) 
			convolveCol_kernel = createKernel("convolveCol");
		convolveCol_kernel.setArgs(pInput, pFilter, pOutput, nInWidth, nInHeight, nFilterWidth);
		return convolveCol_kernel.enqueueNDRange(commandQueue, globalWorkSizes, localWorkSizes, eventsToWaitFor);
	}
}
