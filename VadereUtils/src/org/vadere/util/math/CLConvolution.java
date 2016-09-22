package org.vadere.util.math;


import com.nativelibs4java.opencl.*;

import org.bridj.Pointer;
import org.vadere.util.opencl.kernels.Convolve;

import java.io.IOException;

public class CLConvolution {

	private CLContext context;
	private Convolve gaussianFilter;

	public CLConvolution() throws IOException {
		context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU, CLPlatform.DeviceFeature.MaxComputeUnits);
		gaussianFilter = new Convolve(context);
	}

	public CLConvolution(CLPlatform.DeviceFeature... deviceFeature) throws IOException {
		context = JavaCL.createBestContext(deviceFeature);
		gaussianFilter = new Convolve(context);
	}

	public float[] convolve(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel,
			final int kernelWidth) throws IOException {
		float[] output = new float[input.length];
		CLQueue queue = context.createDefaultQueue();

		CLBuffer<Float> clInput = doubleArrayToCLBuffer(input);
		CLBuffer<Float> clKernel = doubleArrayToCLBuffer(kernel);
		CLBuffer<Float> clOutput = doubleArrayToCLBuffer(output);

		// timer.start();
		CLEvent convolveEvt = gaussianFilter.convolve(queue, clInput, clKernel, clOutput, matrixWidth, matrixHeight,
				kernelWidth, new int[] {matrixWidth, input.length / matrixWidth}, null);

		queue.finish();
		convolveEvt.waitFor();
		Pointer<Float> outPtr = clOutput.read(queue, convolveEvt);

		for (int i = 0; i < output.length; i++) {
			output[i] = outPtr.get(i);
		}
		outPtr.release();

		return output;
	}

	public float[] convolveSperate(final float[] input, final int matrixWidth, final int matrixHeight,
			final float[] kernel, final int kernelWidth) {
		float[] output = new float[input.length];
		float[] tmp = new float[input.length];
		CLQueue queue = context.createDefaultQueue();

		CLBuffer<Float> clInput = doubleArrayToCLBuffer(input);
		CLBuffer<Float> clKernel = doubleArrayToCLBuffer(kernel);
		CLBuffer<Float> clOutput = doubleArrayToCLBuffer(output);
		CLBuffer<Float> clTmp = doubleArrayToCLBuffer(tmp);

		CLEvent convolveEvtCol = gaussianFilter.convolveCol(queue, clInput, clKernel, clTmp, matrixWidth, matrixHeight,
				kernelWidth, new int[] {matrixWidth, input.length / matrixWidth}, null);
		convolveEvtCol.waitFor();
		CLEvent convolveEvtRow = gaussianFilter.convolveRow(queue, clTmp, clKernel, clOutput, matrixWidth, matrixHeight,
				kernelWidth, new int[] {matrixWidth, input.length / matrixWidth}, null);
		queue.finish();
		convolveEvtRow.waitFor();

		Pointer<Float> outPtr = clOutput.read(queue, convolveEvtRow);
		for (int i = 0; i < output.length; i++) {
			output[i] = outPtr.get(i);
		}
		outPtr.release();
		return output;
	}

	public float[] convolveCol(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel,
			final int kernelWidth) {
		float[] output = new float[input.length];
		CLQueue queue = context.createDefaultQueue();

		CLBuffer<Float> clInput = doubleArrayToCLBuffer(input);
		CLBuffer<Float> clKernel = doubleArrayToCLBuffer(kernel);
		CLBuffer<Float> clOutput = doubleArrayToCLBuffer(output);

		CLEvent convolveEvtCol = gaussianFilter.convolveCol(queue, clInput, clKernel, clOutput, matrixWidth,
				matrixHeight, kernelWidth, new int[] {matrixWidth, input.length / matrixWidth}, null);
		convolveEvtCol.waitFor();
		queue.finish();

		Pointer<Float> outPtr = clOutput.read(queue, convolveEvtCol);
		for (int i = 0; i < output.length; i++) {
			output[i] = outPtr.get(i);
		}
		outPtr.release();
		return output;
	}

	public float[] convolveRow(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel,
			final int kernelWidth) {
		float[] output = new float[input.length];
		float[] tmp = new float[input.length];
		CLQueue queue = context.createDefaultQueue();

		CLBuffer<Float> clInput = doubleArrayToCLBuffer(input);
		CLBuffer<Float> clKernel = doubleArrayToCLBuffer(kernel);
		CLBuffer<Float> clOutput = doubleArrayToCLBuffer(output);

		CLEvent convolveEvtRow = gaussianFilter.convolveRow(queue, clInput, clKernel, clOutput, matrixWidth,
				matrixHeight, kernelWidth, new int[] {matrixWidth, input.length / matrixWidth}, null);
		queue.finish();
		convolveEvtRow.waitFor();

		Pointer<Float> outPtr = clOutput.read(queue, convolveEvtRow);
		for (int i = 0; i < output.length; i++) {
			output[i] = outPtr.get(i);
		}
		outPtr.release();
		return output;
	}

	public CLContext getContext() {
		return context;
	}

	private CLBuffer<Float> doubleArrayToCLBuffer(float[] matrix) {
		return CLUtils.doubleArrayToCLBuffer(matrix, context);
	}

}
