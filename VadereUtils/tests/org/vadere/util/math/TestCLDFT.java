package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLDFT;
import org.vadere.util.opencl.OpenCLException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCLDFT {

    private float[] inMatrix = new float[] {
            2, 1, 1, 1,
            1, 1, 1, -1,
            1, 1, 1, 1,
            1, 1, 1, 0
    };

    private static final double EPS = Math.pow(10.0,-5.0);

    @Test
    public void testSimpleDFT() throws OpenCLException {

        int N = 64;
        float[] input = new float[2*N];
        for (int i = 0; i < input.length; ++i) {
            input[i] = i%2 == 0 ? 1 : 0;
        }

        System.out.println("Input: "+Arrays.toString(input));

        CLDFT clfft = new CLDFT(input.length,1, CLDFT.TransformationType.SPACE2FREQUENCY);
        float[] outputF = clfft.dft1Dim(input);
        clfft.clearCL();

        System.out.println("\n Frequency: " + Arrays.toString(outputF));

        assertEquals("The first element should be N",input.length/2,outputF[0],EPS);

        for (int i = 1; i < outputF.length; ++i) {
            assertEquals("All other elements should be 0",0,outputF[i],EPS);
        }

        CLDFT clfftBackwards = new CLDFT(input.length,1,CLDFT.TransformationType.FREQUENCY2SPACE);
        float[] outputS = clfftBackwards.dft1Dim(outputF);
        clfftBackwards.clearCL();

        System.out.println("Space: " + Arrays.toString(outputS));
        assertArrayEquals("Back transformation must be equal to original input",input,outputS,(float)EPS);
    }

}
