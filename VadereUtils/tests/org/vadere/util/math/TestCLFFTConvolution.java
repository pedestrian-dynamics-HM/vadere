package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLConvolution;
import org.vadere.util.opencl.CLFFT;
import org.vadere.util.opencl.CLFFTConvolution;
import org.vadere.util.opencl.OpenCLException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCLFFTConvolution {

    private static final double EPS = Math.pow(10.0, -5.0);

    @Test
    public void testFFTSingleWorkgoup() throws OpenCLException {

        // This test only uses the FFT methods ans does not padd the matrix

        int M = 2048;
        int N = 2048; // single workgroup only for N = 64

        float[] kernel = new float[]{1, 1, 1, 1, 1, 1, 1};
        float[] input = new float[2 * M * N];
        // all complex values are 0!
        for (int i = 0; i < input.length; ++i) {
            input[i] = i % 2 == 0 ? 1 : 1;
        }
        //System.out.println("Input: " + Arrays.toString(input));

        CLFFTConvolution fftTest = new CLFFTConvolution(M, N, kernel.length, kernel, false);
        float[] forwardsFFT = fftTest.fft2Dim(input, CLFFTConvolution.Direction.SPACE2FREQUENCY);

        //System.out.println("\nForward: " + Arrays.toString(forwardsFFT));

        assertTrue("First element should be M*N!", forwardsFFT[0] == M * N);
        assertTrue("Second element should be M*N!", forwardsFFT[1] == M * N);

        for (int i = 2; i < forwardsFFT.length; ++i) {
            assertTrue("All other elements should be 0!", forwardsFFT[i] == 0);
        }

        float[] outputS = fftTest.fft2Dim(forwardsFFT, CLFFTConvolution.Direction.FREQUENCY2SPACE);
        fftTest.clearCL();

        //System.out.println("Space: " + Arrays.toString(outputS));
        assertArrayEquals("Back transformation must be equal to original input", input, outputS, (float) EPS);
    }

    @Test
    public void testFFTMultipleWorkgroups() throws OpenCLException {

        // This test only uses the FFT methods ans does not padd the matrix

        int N = 64; // 2 workgroups for N = 128
        float[] kernel = new float[]{1, 1, 1, 1, 1, 1, 1};
        float[] input = new float[2 * N * N];
        for (int i = 0; i < 2 * N * N; ++i) {
            input[i] = i % 2 == 0 ? 1 : 1;
        }
        //System.out.println("Input: " + Arrays.toString(input));

        CLFFTConvolution fftConvolution = new CLFFTConvolution(N, N, kernel.length, kernel, false);
        float[] forwardsFFT = fftConvolution.fft2Dim(input, CLFFTConvolution.Direction.SPACE2FREQUENCY);

        //System.out.println("\n Forward FFT: " + Arrays.toString(forwardsFFT));

        assertTrue("First element should be N*N!", forwardsFFT[0] == N * N);
        assertTrue("Second element should be N*N!", forwardsFFT[0] == N * N);
        for (int i = 2; i < forwardsFFT.length; ++i) {
            assertTrue("All other elements should be 0!", forwardsFFT[i] == 0);
        }

        float[] backwardsFFT = fftConvolution.fft2Dim(forwardsFFT, CLFFTConvolution.Direction.FREQUENCY2SPACE);

        //System.out.println("Space: " + Arrays.toString(backwardsFFT));
        assertArrayEquals("Back transformation must be equal to original input", input, backwardsFFT, (float) EPS);

        fftConvolution.clearCL();
    }

    @Test
    public void testFFTConvolutionMatrix() throws OpenCLException {

        // This test only uses the FFT methods ans does not padd the matrix

        int M = 256;  // currently working for 32x32
        int N = 256; // next up 64
        int kernelSize = 3; // kernel wir in diesem test nicht verwendet

        float[] matrix = Convolution.generdateInputMatrix(M * N);

        float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));

        CLFFTConvolution fftConvolution = new CLFFTConvolution(M, N, kernelSize, kernel, false); // es soll nicht gepadded werden
        matrix = fftConvolution.zeroPaddMatrix(matrix);
        //System.out.println("padded Matrix " + Arrays.toString(matrix));

        float[] frequency = fftConvolution.fft2Dim(matrix, CLFFTConvolution.Direction.SPACE2FREQUENCY);

        float[] space = fftConvolution.fft2Dim(frequency, CLFFTConvolution.Direction.FREQUENCY2SPACE);
        //System.out.println("space " + Arrays.toString(space));

        assertArrayEquals("Back transformation should match original input!", matrix, space, (float) EPS);
        fftConvolution.clearCL();

    }

    @Test
    public void testKernelFFT() throws OpenCLException {

        int matrixHeight = 20; // currently the matrix will be padded to 10 + 21 - 1 = 30 with the next power of 2 being 32x32,
        int matrixWidth = 20; // for height, width = 40 the matrix will be padded to 64x64
        int kernelSize = 21;
        float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));
        System.out.println("Kernel " + Arrays.toString(kernel));

        CLFFTConvolution fftConvolution = new CLFFTConvolution(matrixHeight, matrixWidth, kernelSize, kernel);

        float[] fftKernel = fftConvolution.getGaussKernelTransformed();
        System.out.println("fftKernel " + Arrays.toString(fftKernel) + "\n size " + fftKernel.length);

//        float[] backTransformation = fftConvolution.fft1Dim(fftKernel, CLFFTConvolution.Direction.FREQUENCY2SPACE);
//        System.out.println("kernel " + Arrays.toString(backTransformation));
//        fftConvolution.clearCL();
//
//        for (int i = 0; i < backTransformation.length; ++i) {
//            if (i % 2 == 0 && i < 2 * kernelSize)
//                assertEquals("Back transformation should give original kernel", kernel[i / 2], backTransformation[i], (float) EPS);
//            else
//                assertEquals("Imag part should be zero and padded positions should be zero again!", 0, backTransformation[i], (float) EPS);
//        }

    }

    @Test
    public void testComputeG() throws OpenCLException {
        int matrixHeight = 20; // currently the matrix will be padded to 10 + 21 - 1 = 30 with the next power of 2 being 32x32,
        int matrixWidth = 20; // for height, width = 40 the matrix will be padded to 64x64
        int kernelSize = 21;
        float[] kernel = new float[]{28f, 0f, -4f, 9.6569f, -4f, 4f, -4f, 1.6569f, -4f, 0f, -4f, -1.6569f, -4f, -4f, -4f, -9.6569f};

        CLFFTConvolution clfftConvolution = new CLFFTConvolution(matrixHeight,matrixWidth,kernelSize,kernel,false);
        float[] G = clfftConvolution.computeG(kernel,8);
        //System.out.println(Arrays.toString(G));
    }

    @Test
    public void test1DimFFT() throws OpenCLException {

        int size = 64;
        float[] input = new float[size];
        for (int i = 0; i < size; ++i) {
            input[i] = 1;
        }
        System.out.println(Arrays.toString(input));

        CLFFTConvolution fftConvolution = new CLFFTConvolution(size, size, size, input, false); // matrix size not used
        float[] output = fftConvolution.fft1Dim(input, CLFFTConvolution.Direction.SPACE2FREQUENCY);
        System.out.println(Arrays.toString(output));
        float[] g = fftConvolution.computeG(output, size / 2);
        System.out.println(Arrays.toString(g));

        assertEquals(size, g.length);
        assertEquals("The first element should be N", size, g[0], EPS);
        assertEquals("The second element should be N", size, g[1], EPS);
        for (int i = 2; i < output.length; ++i) {
            assertEquals("All other elements should be 0", 0, g[i], EPS);
        }

        float[] space = fftConvolution.fft1Dim(output, CLFFTConvolution.Direction.FREQUENCY2SPACE);

        assertArrayEquals("should mach input ", input, space, (float) EPS);

    }

    @Test
    public void testZeroPadding() throws OpenCLException {
        int width = 10;
        int height = 14;
        int kernelSize = 3;

        float[] input = new float[width * height];
        float[] kernel = new float[kernelSize];
        for (int i = 0; i < kernelSize; ++i) {
            kernel[i] = 1;
        }
        for (int i = 0; i < width * height; ++i) {
            input[i] = 1;
        }

        CLFFTConvolution fftConvolution = new CLFFTConvolution(height, width, kernelSize, kernel);

//        float[] kernelPadded = fftConvolution.zeroPaddKernel(kernel);
        float[] matrixPadded = fftConvolution.zeroPaddMatrix(input);

        float[] matrixExpected = new float[]{
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};

        float[] expectedKernel = new float[]{1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

//        assertEquals("padded kernel should have the size ", 32, kernelPadded.length);
        assertEquals("padded matrix should have the size ", 16 * 32, matrixPadded.length);

        assertArrayEquals("padded matrix should match expected matrix", matrixExpected, matrixPadded, (float) EPS);
//        assertArrayEquals("padded kernel should match expected kernel", expectedKernel, kernelPadded, (float) EPS);

        fftConvolution.clearCL();
    }

    @Test
    public void extractOriginalMatrix() throws OpenCLException {
        int width = 10; // 32
        int height = 12; // 16
        int kernelSize = 3;

        float[] input = new float[2 * 16 * 16];
        float[] kernel = new float[2 * kernelSize];
        for (int i = 0; i < kernel.length; ++i) {
            kernel[i] = i; // mock
        }
        for (int i = 0; i < input.length; ++i) {
            input[i] = i; // mock data to check if correct area is extracted
        }

        CLFFTConvolution fftConvolution = new CLFFTConvolution(height, width, kernelSize, kernel);

        //System.out.println(Arrays.toString(input));

        float[] orginalSizeMatrix = fftConvolution.extractOriginalArea(input);
        fftConvolution.clearCL();

        //System.out.println(Arrays.toString(orginalSizeMatrix));

        assertEquals("upper left corner point should be ", 34, orginalSizeMatrix[0], (float) EPS);
        assertEquals("upper right corner point should be ", 52, orginalSizeMatrix[9], (float) EPS);
        assertEquals("lower left corner point should be ", 386, orginalSizeMatrix[110], (float) EPS);
        assertEquals("lower right corner point should be ", 404, orginalSizeMatrix[119], (float) EPS);
    }

    @Test
    public void testNextPowerOf2() {
        assertEquals("Next power of 2 near 5 should be 8 !", 8, CLFFTConvolution.nextPowerOf2(5));
        assertEquals("Next power of 2 near 55 should be 64 !", 64, CLFFTConvolution.nextPowerOf2(55));
    }

    @Test
    public void testTranspose() throws OpenCLException {

        int M = 64;
        int N = 128;
        float[] matrix = new float[2 * M * N];

        for (int i = 0; i < matrix.length; ++i) {
            matrix[i] = i % 2 == 0 ? i / 2 : 0;
        }
        float[] kernel = new float[N];
        for (int i = 0; i < kernel.length; ++i) {
            kernel[i] = 1;
        }

        CLFFTConvolution fftConvolution = new CLFFTConvolution(M, N, N, kernel, false);
        float[] matrixOutput = fftConvolution.multiplyRows(matrix);
        matrixOutput = fftConvolution.multiplyCols(matrixOutput);
        fftConvolution.clearCL();

        assertArrayEquals("twice transposed matrix should return original matrix!", matrix, matrixOutput, (float) EPS);
    }

    @Test
    public void testMultiply() throws OpenCLException {

        int M = 32;
        int N = 32;
        float[] matrix = new float[M * 2 * N];
        float[] expected = new float[M * 2 * N];
        for (int i = 0; i < matrix.length; ++i) {
            matrix[i] = i % 2 == 0 ? 1 : 0;
            expected[i] = i % 2 == 0 ? 4 : 0;
        }

        float[] kernel = new float[N];
        for (int i = 0; i < kernel.length; ++i) {
            kernel[i] = i%2 == 0 ? 2 : 0;
        }
        System.out.println(Arrays.toString(matrix));
        System.out.println(Arrays.toString(expected));
        CLFFTConvolution fftConvolution = new CLFFTConvolution(M, N, N, kernel, false);

        float[] matrixOutput = fftConvolution.multiplyRows(matrix);
        System.out.println(Arrays.toString(matrixOutput));
        matrixOutput = fftConvolution.multiplyCols(matrixOutput);
        System.out.println(Arrays.toString(matrixOutput));

        fftConvolution.clearCL();
        assertArrayEquals("Muliplying rows*2 and cols*2 should give original matrix*4", expected, matrixOutput, (float) EPS);

    }

    @Test
    public void testSmallFFTConvolution() throws OpenCLException {

        int matrixHeight = 200; // currently padded to 32, for 40 padded to 64
        int matrixWidth = 200; // currently padded to 32, for 40 padded to 64
        int kernelSize = 99;

        float[] matrix = Convolution.generdateInputMatrix(matrixHeight * matrixWidth);
        float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));
        //System.out.println("kernel " + Arrays.toString(kernel));
        CLFFTConvolution fftConvolution = new CLFFTConvolution(matrixHeight, matrixWidth, kernelSize, kernel);
        float[] densityMatrix = fftConvolution.convolve(matrix);
        fftConvolution.clearCL();

        // compare results to CLConvolution
        CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
        float[] output = clConvolution.convolve(matrix);
        clConvolution.clearCL();

        assertArrayEquals("CLFFTConvolution does not match clConvolution!", densityMatrix, output, (float) EPS);
    }

    @Test
    public void testFFTConvolution() throws OpenCLException {
        int matrixHeight = 1000; // currently padded to 32, for 40 padded to 64
        int matrixWidth = 1000; // currently padded to 32, for 40 padded to 64

        float[] matrix = Convolution.generdateInputMatrix(matrixHeight * matrixWidth);

        int kernelSize = matrixWidth + 1;
        System.out.println("INFO - " + matrixHeight + " " + matrixWidth + " " + kernelSize);

        float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));

        CLFFTConvolution fftConvolution = new CLFFTConvolution(matrixHeight, matrixWidth, kernelSize, kernel);
        float[] densityMatrix = fftConvolution.convolve(matrix);
        fftConvolution.clearCL();

        // compare results to CLConvolution
        CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
        float[] output = clConvolution.convolve(matrix);
        clConvolution.clearCL();

        assertArrayEquals("CLFFTConvolution does not match clConvolution!", densityMatrix, output, (float) EPS);
    }

    @Test
    public void testRuntimeDifferentMatrixSize() throws OpenCLException {

        int min_size = 256;
        int max_size = (int) Math.pow(2, 12);
        int seed = 1;

        for (int size = min_size; size <= max_size; size *= 2) {

            int kernelSize = 91;
            int matrixHeight = size-kernelSize+1;
            int matrixWidth = size-kernelSize+1;


            float[] matrix = Convolution.generdateInputMatrix(matrixHeight * matrixWidth,seed);
            float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));

            System.out.println("INFO - " + matrixHeight + " " + matrixWidth + " " + kernelSize);
            CLFFTConvolution fftConvolution = new CLFFTConvolution(matrixHeight, matrixWidth, kernelSize, kernel);

            int paddHeight = fftConvolution.getPaddHeight();
            int paddWidth = fftConvolution.getPaddWidth();

            //System.out.println(" padd " + paddWidth);

            float[] densityMatrix = fftConvolution.convolve(matrix);
            fftConvolution.clearCL();

            // compare results to CLConvolution
            CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
            float[] output = clConvolution.convolve(matrix);
            clConvolution.clearCL();
            assertArrayEquals("CLFFTConvolution does not match clConvolution!", densityMatrix, output, (float) EPS);
        }


    }

    @Test
    public void testRuntimeDifferentKernelSize() throws OpenCLException {

        int min_size = 32;
        int max_size = (int) Math.pow(2, 10);
        int seed = 1;

        for (int paddSize = min_size; paddSize <= 32; paddSize *= 2) {

            int matrixHeight = (int) Math.pow(2, 10);
            int matrixWidth = (int) Math.pow(2, 10);
            int kernelSize = paddSize - 1;

            System.out.println("INFO - " + matrixHeight + " " + matrixWidth + " " + kernelSize);
            float[] matrix = Convolution.generdateInputMatrix(matrixHeight * matrixWidth,seed);
            float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));

            CLFFTConvolution fftConvolution = new CLFFTConvolution(matrixHeight, matrixWidth, kernelSize, kernel);
            float[] densityMatrix = fftConvolution.convolve(matrix);
            fftConvolution.clearCL();

            // compare results to CLConvolution
            CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
            float[] output = clConvolution.convolve(matrix);
            clConvolution.clearCL();
            assertArrayEquals("CLFFTConvolution does not match clConvolution!", densityMatrix, output, (float) EPS);
        }


    }

    @Test
    public void testCLConvolution() throws OpenCLException {

        int matrixHeight = (int) Math.pow(2, 5);
        int matrixWidth = (int) Math.pow(2, 5);
        int kernelSize = 11;

        float[] matrix = Convolution.generdateInputMatrix(matrixHeight * matrixWidth);
        float[] kernel = Convolution.floatGaussian1DKernel(kernelSize, (float) Math.sqrt(0.7));

        CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
        float[] output = clConvolution.convolve(matrix);
        clConvolution.clearCL();
    }

    @Test
    public void testRandomMatrix() throws OpenCLException {

        float[] mat1 = Convolution.generdateInputMatrix(100,1);
        float[] mat2 = Convolution.generdateInputMatrix(100,1);

        assertArrayEquals("should give same matrix for same seed", mat1,mat2,(float)EPS);
    }

    @Test
    public void testSimpleConvolution() {

        float[] matrix = new float[]{2, 1, 1, 1, 1, 1, 1, -1, 1, 1, 1, 1, 1, 1, 1, 0};
        float[] kernel = new float[]{0.06118587f, 0.12498604f, 0.06118587f, 0.12498604f, 0.25531236f, 0.12498604f, 0.06118587f, 0.12498604f, 0.06118587f};

//        System.out.println("matrix " + Arrays.toString(matrix));
//        System.out.println("kernel " + Arrays.toString(kernel));

        float[] output = Convolution.convolve(matrix, kernel, 4, 4, 3);

//        System.out.println(Arrays.toString(output));

    }

}
