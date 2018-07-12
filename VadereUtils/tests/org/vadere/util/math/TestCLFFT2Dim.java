package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLFFT;
import org.vadere.util.opencl.CLFFT2Dim;
import org.vadere.util.opencl.OpenCLException;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCLFFT2Dim {

    private static final double EPS = Math.pow(10.0,-5.0);

    @Test
    public void testForwardAndBackwardFFT() throws OpenCLException {

        int N = 64;
        float[] input = new float[2*N*N];
        for (int i = 0; i < input.length; ++i) {
            input[i] = i%2 == 0 ? 1 : 0;
        }
        System.out.println("Input: "+Arrays.toString(input));

        CLFFT2Dim clffftForwards = new CLFFT2Dim(input.length,N,CLFFT2Dim.TransformationType.SPACE2FREQUENCY);
        float[] forwardsFFT = clffftForwards.fft2Dim(input);
        clffftForwards.clearCL();

        System.out.println("\n Forward FFT: " + Arrays.toString(forwardsFFT));

        assertTrue("First element should be N*N!",forwardsFFT[0] == N*N);
        for (int i = 1; i < forwardsFFT.length; ++i) {
            assertTrue("All other elements should be 0!",forwardsFFT[i] == 0);
        }

        CLFFT2Dim clfftBackward = new CLFFT2Dim(forwardsFFT.length,N,CLFFT2Dim.TransformationType.FREQUENCY2SPACE);
        float[] outputS = clfftBackward.fft2Dim(forwardsFFT);
        clfftBackward.clearCL();
        System.out.println("Space: " + Arrays.toString(outputS));
        assertArrayEquals("Back transformation must be equal to original input",input,outputS,(float)EPS);
    }


}
