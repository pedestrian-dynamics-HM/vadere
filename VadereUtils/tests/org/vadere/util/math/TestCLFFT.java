package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLFFT;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCLFFT {

    float[] inMatrix = new float[] {
            2, 1, 1, 1,
            1, 1, 1,-1,
            1, 1, 1, 1,
            1, 1, 1, 0
    };

    private static final double EPS = Math.pow(10.0,-5.0);

    @Test
    public void testForwardAndBackwardFFT() throws OpenCLException {

        int N = 256;
        float[] input = new float[2*N];
        for (int i = 0; i < input.length; ++i) {
            input[i] = i%2 == 0 ? 1 : 0;
        }
        System.out.println("Input: "+Arrays.toString(input));

        CLFFT clfftForward = new CLFFT(input.length,CLFFT.TransformationType.SPACE2FREQUENCY);
        float[] outputF = clfftForward.fft1Dim(input);
        clfftForward.clearCL();

        System.out.println("\n Frequency: " + Arrays.toString(outputF));

        assertEquals("The first element should be N",input.length/2,outputF[0],EPS);

        for (int i = 1; i < outputF.length; ++i) {
            assertEquals("All other elements should be 0",0,outputF[i],EPS);
        }

        CLFFT clfftBackward = new CLFFT(input.length, CLFFT.TransformationType.FREQUENCY2SPACE);
        float[] outputS = clfftBackward.fft1Dim(outputF);
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
