package org.vadere.util.math;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FourierTransformation {

	public Complex[] transform(double[] sample) {
		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] Xc = fft.transform(sample, TransformType.FORWARD);
		return Xc;
	}

	public Complex[] transform(Complex[] sample) {
		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] Xc = fft.transform(sample, TransformType.FORWARD);
		return Xc;
	}
}
