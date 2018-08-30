package org.vadere.util.math;

import org.junit.Test;
import org.vadere.util.opencl.CLConvolution;
import org.vadere.util.opencl.CLFFTConvolution;
import org.vadere.util.opencl.CLFFTConvolutionDP;
import org.vadere.util.opencl.OpenCLException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestCLFFTConvolutionDP {

    private static final double EPS = Math.pow(10.0, -5.0);

    @Test
    public void testFFTSingleWorkgoup() throws OpenCLException {

        // This test only uses the FFT methods ans does not padd the matrix

        int N = 64; // single workgroup only for N = 64
        double[] kernel = new double[]{1, 1, 1, 1, 1, 1, 1};
        double[] input = new double[2 * N * N];
        // all complex values are 0!
        for (int i = 0; i < input.length; ++i) {
            input[i] = i % 2 == 0 ? 1 : 1;
        }
        System.out.println("Input: " + Arrays.toString(input));

        CLFFTConvolutionDP fftTest = new CLFFTConvolutionDP(N, N, kernel.length, kernel, false);
        double[] forwardsFFT = fftTest.fft2Dim(input, CLFFTConvolutionDP.Direction.SPACE2FREQUENCY);

        System.out.println("\nForward: " + Arrays.toString(forwardsFFT));

        assertTrue("First element should be N*N!", forwardsFFT[0] == N * N);
        assertTrue("Second element should be N*N!", forwardsFFT[1] == N * N);

        for (int i = 2; i < forwardsFFT.length; ++i) {
            assertTrue("All other elements should be 0!", forwardsFFT[i] == 0);
        }

        double[] outputS = fftTest.fft2Dim(forwardsFFT, CLFFTConvolutionDP.Direction.FREQUENCY2SPACE);
        fftTest.clearCL();

        System.out.println("Space: " + Arrays.toString(outputS));
        assertArrayEquals("Back transformation must be equal to original input", input, outputS, (float) EPS);
    }

    @Test
    public void testFFTMultipleWorkgroups() throws OpenCLException {

        // This test only uses the FFT methods ans does not padd the matrix

        int N = 64; // 2 workgroups for N = 128
        double[] kernel = new double[]{1, 1, 1, 1, 1, 1, 1};
        double[] input = new double[2 * N * N];
        for (int i = 0; i < 2 * N * N; ++i) {
            input[i] = i % 2 == 0 ? 1 : 1;
        }
        System.out.println("Input: " + Arrays.toString(input));

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(N, N, kernel.length, kernel, false);
        double[] forwardsFFT = fftConvolution.fft2Dim(input, CLFFTConvolutionDP.Direction.SPACE2FREQUENCY);

        System.out.println("\n Forward FFT: " + Arrays.toString(forwardsFFT));

        assertTrue("First element should be N*N!", forwardsFFT[0] == N * N);
        assertTrue("Second element should be N*N!", forwardsFFT[0] == N * N);
        for (int i = 2; i < forwardsFFT.length; ++i) {
            assertTrue("All other elements should be 0!", forwardsFFT[i] == 0);
        }

        double[] backwardsFFT = fftConvolution.fft2Dim(forwardsFFT, CLFFTConvolutionDP.Direction.FREQUENCY2SPACE);

        System.out.println("Space: " + Arrays.toString(backwardsFFT));
        assertArrayEquals("Back transformation must be equal to original input", input, backwardsFFT, (float) EPS);

        fftConvolution.clearCL();
    }

    private double[] generateMockInputMatrix(int height, int width, int seed) {
        Random random = new Random(seed);
        // TODO use vadere data
        double[] matrix = new double[height*width];
        for (int i = 0; i < height*width; i++) {
            matrix[i] = (float) random.nextDouble();
        }
        return matrix;
    }

    @Test
    public void testFFTConvolutionMatrix() throws OpenCLException {

        // This test only uses the FFT methods ans does not padd the matrix

        int M = 64;  // currently working for 32x32
        int N = 64; // next up 64
        int kernelSize = 3; // kernel wir in diesem test nicht verwendet

        double[] matrix = generateMockInputMatrix(M,N,1);
        double[] kernel = Convolution.generateDoubleGaussianKernel(kernelSize, (double) Math.sqrt(0.7));

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(M, N, kernelSize, kernel, false); // es soll nicht gepadded werden
        matrix = fftConvolution.zeroPaddMatrix(matrix);
        System.out.println("padded Matrix " + Arrays.toString(matrix));

        double[] frequency = fftConvolution.fft2Dim(matrix, CLFFTConvolutionDP.Direction.SPACE2FREQUENCY);
        System.out.println("frequency " + Arrays.toString(frequency));
        double[] space = fftConvolution.fft2Dim(frequency, CLFFTConvolutionDP.Direction.FREQUENCY2SPACE);
        System.out.println("space " + Arrays.toString(space));

        assertArrayEquals("Back transformation should match original input!", matrix, space, (float) EPS);
        fftConvolution.clearCL();

    }

    @Test
    public void testKernelFFT() throws OpenCLException {

        int matrixHeight = 20; // currently the matrix will be padded to 10 + 21 - 1 = 30 with the next power of 2 being 32x32,
        int matrixWidth = 20; // for height, width = 40 the matrix will be padded to 64x64
        int kernelSize = 21;
        double[] kernel = Convolution.generateDoubleGaussianKernel(kernelSize, Math.sqrt(0.7));
        System.out.println("Kernel " + Arrays.toString(kernel));

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(matrixHeight, matrixWidth, kernelSize, kernel);

        double[] fftKernel = fftConvolution.getGaussKernelTransformed();
        System.out.println("fftKernel " + Arrays.toString(fftKernel));

        double[] backTransformation = fftConvolution.fft1Dim(fftKernel, CLFFTConvolutionDP.Direction.FREQUENCY2SPACE);

        String back = "";
        for (int i = 0; i < 2*kernelSize; ++i) {
            if (i%2 == 0)
                back += backTransformation[i]+", ";
        }
        System.out.println("back " + back);

        fftConvolution.clearCL();

       for (int i = 0; i < backTransformation.length; ++i) {
            if (i % 2 == 0 && i < 2 * kernelSize)
                assertEquals("Back transformation should give original kernel", kernel[i / 2], backTransformation[i], (float) EPS);
            else
                assertEquals("Imag part should be zero and padded positions should be zero again!", 0, backTransformation[i], (float) EPS);
        }

    }

    @Test
    public void test1DimFFT() throws OpenCLException {

        int size = 64;
        double[] input = new double[2 * size];
        for (int i = 0; i < 2 * size; ++i) {
            input[i] = i % 2 == 0 ? 1 : 1;
        }
        System.out.println(Arrays.toString(input));

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(size, size, size, input, false); // matrix size not used
        double[] output = fftConvolution.fft1Dim(input, CLFFTConvolutionDP.Direction.SPACE2FREQUENCY);
        System.out.println(Arrays.toString(output));

        assertEquals(2 * size, output.length);
        assertEquals("The first element should be N", size, output[0], EPS);
        assertEquals("The second element should be N", size, output[1], EPS);
        for (int i = 2; i < output.length; ++i) {
            assertEquals("All other elements should be 0", 0, output[i], EPS);
        }

    }

    @Test
    public void testZeroPadding() throws OpenCLException {
        int width = 10;
        int height = 14;
        int kernelSize = 3;

        double[] input = new double[width * height];
        double[] kernel = new double[kernelSize];
        for (int i = 0; i < kernelSize; ++i) {
            kernel[i] = 1;
        }
        for (int i = 0; i < width * height; ++i) {
            input[i] = 1;
        }

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(height, width, kernelSize, kernel);

        double[] kernelPadded = fftConvolution.zeroPaddKernel(kernel);
        double[] matrixPadded = fftConvolution.zeroPaddMatrix(input);

        double[] matrixExpected = new double[]{
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

        double[] expectedKernel = new double[]{1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        assertEquals("padded kernel should have the size ", 32, kernelPadded.length);
        assertEquals("padded matrix should have the size ", 16 * 32, matrixPadded.length);

        assertArrayEquals("padded matrix should match expected matrix", matrixExpected, matrixPadded,  EPS);
        assertArrayEquals("padded kernel should match expected kernel", expectedKernel, kernelPadded,  EPS);
    }

    @Test
    public void extractOriginalMatrix() throws OpenCLException {
        int width = 10; // 32
        int height = 12; // 16
        int kernelSize = 3;

        double[] input = new double[2 * 16 * 16];
        double[] kernel = new double[2 * kernelSize];
        for (int i = 0; i < kernel.length; ++i) {
            kernel[i] = i; // mock
        }
        for (int i = 0; i < input.length; ++i) {
            input[i] = i; // mock data to check if correct area is extracted
        }

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(height, width, kernelSize, kernel);

        //System.out.println(Arrays.toString(input));

        double[] orginalSizeMatrix = fftConvolution.extractOriginalArea(input);
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
        double[] matrix = new double[2 * M * N];

        for (int i = 0; i < matrix.length; ++i) {
            matrix[i] = i % 2 == 0 ? i / 2 : 0;
        }
        double[] kernel = new double[N];
        for (int i = 0; i < kernel.length; ++i) {
            kernel[i] = 1;
        }

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(M, N, N, kernel, false);
        double[] matrixOutput = fftConvolution.multiplyRows(matrix);
        matrixOutput = fftConvolution.multiplyCols(matrixOutput);
        fftConvolution.clearCL();

        assertArrayEquals("twice transposed matrix should return original matrix!", matrix, matrixOutput, (float) EPS);
    }

    @Test
    public void testMultiply() throws OpenCLException {

        int M = 64;
        int N = 128;
        double[] matrix = new double[M * 2 * N];
        double[] expected = new double[M * 2 * N];
        for (int i = 0; i < matrix.length; ++i) {
            matrix[i] = i % 2 == 0 ? i : 0;
            expected[i] = i % 2 == 0 ? 4 * i : 0;
        }

        double[] kernel = new double[2 * N];
        for (int i = 0; i < kernel.length; ++i) {
            kernel[i] = 2;
        }

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(M, N, N, kernel, false);

        double[] matrixOutput = fftConvolution.multiplyRows(matrix);

        matrixOutput = fftConvolution.multiplyCols(matrixOutput);
        fftConvolution.clearCL();

        assertArrayEquals("Muliplying rows*2 and cols*2 should give original matrix*4", expected, matrixOutput, EPS);

    }


    @Test
    public void testSmallFFTConvolution() throws OpenCLException {

        int matrixHeight = 20; // currently padded to 32, for 40 padded to 64
        int matrixWidth = 20; // currently padded to 32, for 40 padded to 64

        double[] matrix = generateMockInputMatrix(matrixHeight,matrixWidth,1); // TODO use random seed
        //System.out.println("matrix " + Arrays.toString(matrix));

        int kernelSize = 11;
        double[] kernel = Convolution.generateDoubleGaussianKernel(kernelSize,Math.sqrt(0.7));

        //System.out.println("kernel " + Arrays.toString(kernel));

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(matrixHeight, matrixWidth, kernelSize, kernel);
        double[] densityMatrix = fftConvolution.convolve(matrix);
        fftConvolution.clearCL();

        System.out.println("density " + Arrays.toString(densityMatrix));

        // compare results to CLConvolution

//        CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
//        float[] output = clConvolution.convolve(matrix);
//        clConvolution.clearCL();
//        //System.out.println("clConvolution " + Arrays.toString(output));
//
//        assertArrayEquals("CLFFTConvolution does not match clConvolution!", densityMatrix, output, EPS);

    }

    @Test
    public void testFFTConvolution() throws OpenCLException {
        int matrixHeight = 110; // currently padded to 32, for 40 padded to 64
        int matrixWidth = 110; // currently padded to 32, for 40 padded to 64

        //float[] matrix = Convolution.generdateInputMatrix(matrixHeight * matrixWidth);
        double[] matrix = generateMockInputMatrix(matrixHeight,matrixWidth,0);
        System.out.println("matrix " + Arrays.toString(matrix));

        int kernelSize = 21;
        double[] kernel = Convolution.generateDoubleGaussianKernel(kernelSize,Math.sqrt(0.7));
        //System.out.println("kernel " + Arrays.toString(kernel));

        CLFFTConvolutionDP fftConvolution = new CLFFTConvolutionDP(matrixHeight, matrixWidth, kernelSize, kernel);
        double[] densityMatrix = fftConvolution.convolve(matrix);
        fftConvolution.clearCL();

        System.out.println("density " + Arrays.toString(densityMatrix));

        // compare results to CLConvolution

//        CLConvolution clConvolution = new CLConvolution(CLConvolution.KernelType.Separate, matrixHeight, matrixWidth, kernelSize, kernel);
//        float[] output = clConvolution.convolve(matrix);
//        clConvolution.clearCL();
//        System.out.println("clConvolution " + Arrays.toString(output));
//
//        assertArrayEquals("CLFFTConvolution does not match clConvolution!", densityMatrix, output, (float) EPS);

    }

    @Test
    public void testRuntimeFFTConvolution() {

    }

    @Test
    public void testSimpleConvolution() {

        float[] matrix = new float[]{2, 1, 1, 1, 1, 1, 1, -1, 1, 1, 1, 1, 1, 1, 1, 0};
        float[] kernel = new float[]{0.06118587f, 0.12498604f, 0.06118587f, 0.12498604f, 0.25531236f, 0.12498604f, 0.06118587f, 0.12498604f, 0.06118587f};

        System.out.println("matrix " + Arrays.toString(matrix));
        System.out.println("kernel " + Arrays.toString(kernel));

        float[] output = Convolution.convolve(matrix, kernel, 4, 4, 3);

        System.out.println(Arrays.toString(output));

    }

}
