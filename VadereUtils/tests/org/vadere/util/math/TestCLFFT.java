package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLFFT;
import org.vadere.util.opencl.OpenCLException;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestCLFFT {

    private static final double EPS = Math.pow(10.0,-5.0);

    @Test
    public void testForwardAndBackwardFFT() throws OpenCLException {

        // mock data
        int N = 64;
        float[] input = new float[2*N];
        for (int i = 0; i < input.length; ++i) {
            input[i] = i%2 == 0 ? 64 : 0;
        }

        // forward FFT
        CLFFT clfftForward = new CLFFT(input.length,N,CLFFT.TransformationType.SPACE2FREQUENCY);
        float[] outputF = clfftForward.fft1Dim(input);
        clfftForward.clearCL();

        assertEquals("The first element should be N*N",N*N,outputF[0],EPS);
        for (int i = 1; i < outputF.length; ++i) {
            assertEquals("All other elements should be 0",0,outputF[i],EPS);
        }


        // backward FFT
        CLFFT clfftBackward = new CLFFT(input.length,N, CLFFT.TransformationType.FREQUENCY2SPACE);
        float[] outputS = clfftBackward.fft1Dim(outputF);
        clfftBackward.clearCL();

        assertArrayEquals("Back transformation must be equal to original input",input,outputS,(float)EPS);

    }


}
