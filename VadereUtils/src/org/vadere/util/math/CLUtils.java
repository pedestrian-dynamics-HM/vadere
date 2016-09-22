package org.vadere.util.math;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLMem;

import org.bridj.Pointer;

import static org.bridj.Pointer.allocateFloats;

public class CLUtils {

	public static CLBuffer<Float> doubleArrayToCLBuffer(float[] matrix, CLContext context) {
		Pointer<Float> aPtr = allocateFloats(matrix.length).order(context.getByteOrder());
		for (int i = 0; i < matrix.length; i++) {
			aPtr.set(i, matrix[i]);
		}

		return context.createFloatBuffer(CLMem.Usage.Input, aPtr);
	}
}
