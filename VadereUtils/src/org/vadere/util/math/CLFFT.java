package org.vadere.util.math;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import org.vadere.util.opencl.kernels.FFT;

import java.io.IOException;

public class CLFFT {


	private CLContext context;
	private FFT fft;

	public CLFFT() throws IOException {
		context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU, CLPlatform.DeviceFeature.MaxComputeUnits);
		fft = new FFT(context);
	}

	/*public Complex[] transform(float[] sample) {
		CLQueue queue = context.createDefaultQueue();
		float[] coeffs = new float[sample.length*2];
		Complex[] result = new Complex[sample.length];

		CLBuffer<Float> x = CLUtils.doubleArrayToCLBuffer(sample, context);
		CLBuffer<Float> clOutput = CLUtils.doubleArrayToCLBuffer(coeffs, context);
		//CLEvent transformEvt = fft.fft(queue, )
				//dft(queue, x, clOutput, new int[] {sample.length}, null);

		queue.finish();
		transformEvt.waitFor();
		Pointer<Float> outPtr = clOutput.read(queue, transformEvt);
		for (int i = 0; i < coeffs.length; i += 2) {
			result[i/2] = Complex.valueOf((double)outPtr.get(i), (double)outPtr.get(i+1));
		}
		outPtr.release();
		return result;
	}*/
}
