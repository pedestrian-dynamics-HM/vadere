package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLFFT;
import org.vadere.util.opencl.CLFFT2Dim;
import org.vadere.util.opencl.OpenCLException;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestCLFFT2Dim {

    float[] inMatrix = new float[] {
            2, 1, 1, 1,
            1, 1, 1,-1,
            1, 1, 1, 1,
            1, 1, 1, 0
    };

    private static final double EPS = Math.pow(10.0,-5.0);

    @Test
    public void testForwardAndBackwardFFT() throws OpenCLException {

        int N = 64;
        float[] input = new float[2*N*N];
        for (int i = 0; i < input.length; ++i) {
            input[i] = i%2 == 0 ? 1 : 0;
        }
        System.out.println("Input: "+Arrays.toString(input));

        CLFFT2Dim clfftForward = new CLFFT2Dim(input.length,N,CLFFT2Dim.TransformationType.SPACE2FREQUENCY);
        float[] outputF = clfftForward.fft2Dim(input);
        clfftForward.clearCL();

        System.out.println("\n Frequency: " + Arrays.toString(outputF));

        assertEquals("The first element should be N",N,outputF[0],EPS);

        for (int i = 1; i < outputF.length; ++i) {
            assertEquals("All other elements should be 0",0,outputF[i],EPS);
        }

        CLFFT2Dim clfftBackward = new CLFFT2Dim(N,N,CLFFT2Dim.TransformationType.FREQUENCY2SPACE);
        float[] outputS = clfftBackward.fft2Dim(outputF);
        clfftBackward.clearCL();

        System.out.println("Space: " + Arrays.toString(outputS));
        assertArrayEquals("Back transformation must be equal to original input",input,outputS,(float)EPS);
    }

    public void testSimpleConvolutionWithFFT() {

        float[] kernel = new float[] {
                1, 2, 3,
                4, 5, 6,
                7, 8, 9
        };

        float[] inMatrix = new float[] {
                2, 1, 1, 1,
                1, 1, 1,-1,
                1, 1, 1, 1,
                1, 1, 1, 0
        };

    }


}
