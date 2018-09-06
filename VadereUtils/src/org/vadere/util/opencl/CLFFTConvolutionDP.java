package org.vadere.util.opencl;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;


public class CLFFTConvolutionDP {

    private static final int OFFSET_ZERO = 0;
    private static final int SOURCE_BUFFER_SIZE = 4096;
    private static final int BUFFER_SIZE_FACTOR = 8;
    private static final int MALLOC_SIZE = 1;
    private static final int DIMENSION_ZERO = 0;
    private static final int WORK_DIM = 1;
    private static final int FLOAT_PRECITION = 16;
    private static final int MIN_ELEMENTS_PER_WORKITEM = 8;

    // CL ids
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clQueue;
    private long clProgram;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;

    // Host Memory
    private DoubleBuffer hostInput;
    private DoubleBuffer hostOutput;
    private DoubleBuffer hostGaussKernelInput;
    private DoubleBuffer hostGaussKernelTransformed;
    private ByteBuffer kernelSourceCode;

    private int matrixHeight;
    private int matrixWidth;
    private int kernelSize;
    private double[] gaussKernel;
    private double[] gaussKernelTransformed;

    private int paddHeight;
    private int paddWidth;

    // for global and local size
    private int max_fft_per_work_item;
    private int max_workitems_per_workgroup;

    // Kernel Memory
    private long clMatrixInput;
    private long clMatrixOutput;
    private long clGaussKernelInput;
    private long clGaussKernelTransformed;

    private long clFFTKernel;
    private long clFFTMatrix;
    private long clMultiplyKernel;


    // logger for callbacks
    private static Logger log = Logger.getLogger(CLFFTConvolutionDP.class);
    private boolean debug = false;
    private boolean profiling = false;
    private boolean padd;

    public enum Direction {
        SPACE2FREQUENCY(1), FREQUENCY2SPACE(-1);

        private final int val;
        Direction(int val) { this.val = val;}
        public int getValue() {return val;}
    }

    /**
     * FFT Convolution takes the 1 dimensional Argument gaussKernel and directly applys a FFT on it.
     * matrixHeight, matrixWidth and kernelSize are the dimensions for the real Data.
     *
     * @param matrixHeight
     * @param matrixWidth
     * @param kernelSize
     * @param gaussKernel
     * @throws OpenCLException
     */


    public CLFFTConvolutionDP(int matrixHeight, int matrixWidth, int kernelSize, @NotNull final double[] gaussKernel) throws OpenCLException {

        this(matrixHeight,matrixWidth,kernelSize,gaussKernel,true);

    }

    public CLFFTConvolutionDP(int matrixHeight, int matrixWidth, int kernelSize, @NotNull final double[] gaussKernel, boolean padd) throws OpenCLException {
        this.matrixHeight = matrixHeight;
        this.matrixWidth = matrixWidth;
        this.kernelSize = kernelSize;
        this.padd = padd;
        // enables debug memory operations
        if (debug) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        if (padd) {
            computePaddHeight();
            computePaddWidth();
        } else {
            this.paddWidth = matrixWidth;
            this.paddHeight = matrixHeight;
        }
        this.gaussKernel = zeroPaddKernel(gaussKernel);

        init();
    }


        /**
         * init initializes all OpenCL fields, the openCL Callbacks for error messages. Also builds the Kernels for the
         * 2 - dimensional FFT, 1 - dimensional FFT and the multiplication
         * Both for the matrix and the gauss Kernel FFT the Memory is allocated,
         * hostGaussKernelTransformed holds the transformed gaussKernel
         * @throws OpenCLException
         */
    public void init() throws OpenCLException {
        try {
            initCallbacks();
            initCL();
            buildKernels();

            // get divice specific constants
            long local_mem_size = CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
            max_workitems_per_workgroup = (int) CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);

            // determin biggest fft that can be computed with one workitem
            this.max_fft_per_work_item = (int) (local_mem_size / (max_workitems_per_workgroup * FLOAT_PRECITION));

            // reserve memory for input matrix and gauss kernel
            hostInput = MemoryUtil.memAllocDouble(2 * paddHeight * paddWidth);
            hostOutput = MemoryUtil.memAllocDouble(2 * paddHeight * paddWidth);
            hostGaussKernelInput = MemoryUtil.memAllocDouble(2*paddWidth);

            setArgumentsFFT();

            if (padd) {
                gaussKernelTransformed = fftGaussKernel();
            } else {
                gaussKernelTransformed = gaussKernel; // for testing purposses skipp kernel fft
            }

            hostGaussKernelTransformed = CLUtils.toDoubleBuffer(gaussKernelTransformed);


            // clear hostGaussKernel and clGaussKernel as kernel fft is only done once
            // CLInfo.checkCLError(clReleaseMemObject(clGaussKernelInput));

            setArgumentsMultiply();

        } catch (OpenCLException ex) {
            throw ex;
        } finally {

        }
    }

    /**
     * initializes Callbacks for error messages
     */
    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.debug("[LWJGL] cl_context_callback" + "\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
            try {
                log.debug("The cl_program [0x" + program + "] was built " + (CLInfo.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
            } catch (OpenCLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Inizializes the clPlatform, clDevice, clContext and the cl Queue
     * @throws OpenCLException
     */
    private void initCL() throws OpenCLException {
        try (MemoryStack stack = stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(1);
            IntBuffer numberOfPlatforms = stack.mallocInt(1);

            CLInfo.checkCLError(clGetPlatformIDs(null, numberOfPlatforms));
            PointerBuffer platformIDs = stack.mallocPointer(numberOfPlatforms.get(0));
            CLInfo.checkCLError(clGetPlatformIDs(platformIDs, numberOfPlatforms));

            clPlatform = platformIDs.get(0);

            IntBuffer numberOfDevices = stack.mallocInt(1);
            CLInfo.checkCLError(clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, null, numberOfDevices));
            PointerBuffer deviceIDs = stack.mallocPointer(numberOfDevices.get(0));
            CLInfo.checkCLError(clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, deviceIDs, numberOfDevices));

            clDevice = deviceIDs.get(0);

            log.debug("CL_DEVICE_NAME = " + CLInfo.getDeviceInfoStringUTF8(clDevice, CL_DEVICE_NAME));

            PointerBuffer ctxProps = stack.mallocPointer(3);
            ctxProps.put(CL_CONTEXT_PLATFORM)
                    .put(clPlatform)
                    .put(NULL)
                    .flip();

            clContext = clCreateContext(ctxProps, clDevice, contextCB, NULL, errcode_ret);
            CLInfo.checkCLError(errcode_ret);

            clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);
            CLInfo.checkCLError(errcode_ret);
        }
    }

    public double[] getGaussKernelTransformed() {
        return gaussKernelTransformed;
    }

    public double[] getGaussKernel() {
        return gaussKernel;
    }

    /**
     * Sets the kernel arguments for the FFT for the matrix and the gauss kernel
     * @throws OpenCLException
     */
    private void setArgumentsFFT() throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            clMatrixInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, BUFFER_SIZE_FACTOR * 2 * paddHeight * paddWidth, errcode_ret);
            clMatrixOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, BUFFER_SIZE_FACTOR * 2 * paddHeight * paddWidth, errcode_ret);

            CLInfo.checkCLError(clSetKernelArg1p(clFFTMatrix, 0, clMatrixInput)); // input
            CLInfo.checkCLError(clSetKernelArg(clFFTMatrix, 1, paddWidth * paddHeight)); // local memory size
            CLInfo.checkCLError(clSetKernelArg1p(clFFTMatrix, 2, clMatrixOutput)); // output

            clGaussKernelInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, BUFFER_SIZE_FACTOR * 2 * paddWidth, errcode_ret);

            CLInfo.checkCLError(clSetKernelArg1p(clFFTKernel, 0, clGaussKernelInput));
            CLInfo.checkCLError(clSetKernelArg(clFFTKernel, 1, (long) paddWidth)); // local memory size
            CLInfo.checkCLError(clSetKernelArg1i(clFFTKernel, 2, paddWidth));
        }
    }

    /**
     * Sets the kernel arguments for the multiplication
     * @throws OpenCLException
     */
    private void setArgumentsMultiply() throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            clGaussKernelTransformed = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, hostGaussKernelTransformed, errcode_ret);

            CLInfo.checkCLError(clSetKernelArg1p(clMultiplyKernel, 0, clMatrixInput));
            CLInfo.checkCLError(clSetKernelArg1p(clMultiplyKernel, 1, clGaussKernelTransformed));
            CLInfo.checkCLError(clSetKernelArg1p(clMultiplyKernel, 2, clMatrixOutput));
        }
    }

    /**
     * Computes the linear convolution of the given matrix with the set Gaussian kernel
     * @param matrix position matrix
     * @return densityMatrix with the size of the original matrix
     * @throws OpenCLException
     */
    public double[] convolve(final double[] matrix) throws OpenCLException {

        // padd Matrix
        double[] tmpMatrix = zeroPaddMatrix(matrix);
        // FFT on Matrix
        tmpMatrix = fft2Dim(tmpMatrix, Direction.SPACE2FREQUENCY);

        //multiply matrix rows with kernel
        tmpMatrix = multiplyRows(tmpMatrix);
        //multiply matrix cols with kernel
        tmpMatrix = multiplyCols(tmpMatrix);

        // IFFT
        tmpMatrix = fft2Dim(tmpMatrix, Direction.FREQUENCY2SPACE);
        return extractOriginalArea(tmpMatrix);
    }

    /**
     * multiply computes the point wise multiplication of the matrix with the kernel row and column wise
     * @param matrix
     * @return outputMatrix
     * @throws OpenCLException
     */
    public double[] multiply(final double[] matrix, int height, int width) throws OpenCLException {

        CLUtils.toDoubleBuffer(matrix,hostInput);
        clEnqueueWriteBuffer(clQueue,clMatrixInput,true,OFFSET_ZERO,hostInput,null,null);

        multiply(clMultiplyKernel, height,width);

        clEnqueueReadBuffer(clQueue,clMatrixOutput,true,OFFSET_ZERO,hostOutput,null,null);

        return CLUtils.toDoubleArray(hostOutput,matrix.length);
    }

    /**
     * multiply sets the local and global worksize and enqueues the multiply kernel
     * @param clKernel
     * @throws OpenCLException
     */
    private void multiply(final long clKernel, int height, int width) throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(2);

            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, height);
            clGlobalWorkSizeEdges.put(1, width);

            CLInfo.checkCLError(clSetKernelArg1i(clMultiplyKernel, 3, height));
            CLInfo.checkCLError(clSetKernelArg1i(clMultiplyKernel, 4, width));

            CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue,clKernel,2,null,clGlobalWorkSizeEdges,null,null, null));
            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    public double[] multiplyRows(final double[] matrix) throws OpenCLException {
        return multiply(matrix,paddHeight,paddWidth);
    }

    public double[] multiplyCols(final double[] matrix) throws OpenCLException {
        return multiply(matrix,paddWidth,paddHeight); // matrix is transposed
    }

    /**
     * fft1Dim computes the 1 dimensional FFT of the input in the given direction
     * @param input
     * @param direction
     * @return transformed input
     * @throws OpenCLException
     */
    public double[] fft1Dim(final double[] input, Direction direction) throws OpenCLException {
        // inner FFT
        CLUtils.toDoubleBuffer(input, hostGaussKernelInput);
        clEnqueueWriteBuffer(clQueue, clGaussKernelInput, true, OFFSET_ZERO, hostGaussKernelInput, null, null);
        fft1Dim(clFFTKernel, direction);
        clEnqueueReadBuffer(clQueue, clGaussKernelInput, true, OFFSET_ZERO, hostGaussKernelInput, null, null);

        return CLUtils.toDoubleArray(hostGaussKernelInput, input.length);
    }

    /**
     * fft1Dim sets the local and global worksize and enqueues the 1 dim FFT Kernel
     * @param clKernel
     * @param direction
     * @throws OpenCLException
     */
    private void fft1Dim(final long clKernel, Direction direction) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int workitems_total = paddWidth/max_fft_per_work_item;
            int workitems_per_workgroup = Math.min(workitems_total,max_workitems_per_workgroup); //paddWidth/max_fft_per_work_item;

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            clLocalWorkSizeEdges.put(DIMENSION_ZERO, workitems_per_workgroup); // number of workitems per workgroup
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, workitems_total); // number of workitems global

            // set direction for FFT
            CLInfo.checkCLError(clSetKernelArg1i(clFFTKernel, 3, direction.getValue()));

            CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null));
            CLInfo.checkCLError(clFinish(clQueue));

        }
    }

    /**
     * fftGaussKernel computes the FFT transformation into frequency space of the set gauss kernel
     * @return transformedGaussKernel in the frequency space
     * @throws OpenCLException
     */
    private double[] fftGaussKernel() throws OpenCLException {
        return fft1Dim(gaussKernel, Direction.SPACE2FREQUENCY);
    }

    /**
     * fft2Dim compute the 2 dimensional FFT in the given direction. The input matrix is given in vector.
     * @param input
     * @param direction
     * @return output transformed input matrix
     * @throws OpenCLException
     */
    public double[] fft2Dim(final double[] input, Direction direction) throws OpenCLException {

        // inner FFT
        CLUtils.toDoubleBuffer(input, hostInput);
        clEnqueueWriteBuffer(clQueue, clMatrixInput, true, OFFSET_ZERO, hostInput, null, null);

        fft2Dim(clFFTMatrix, paddHeight, paddWidth, direction);
        clEnqueueReadBuffer(clQueue, clMatrixOutput, true, OFFSET_ZERO, hostOutput, null, null);

        // outer FFT -> matrix is transposed
        double[] innerFFT = CLUtils.toDoubleArray(hostOutput, 2 * paddHeight * paddWidth); // TODO avoid having to read to array and then write to buffer again
        CLUtils.toDoubleBuffer(innerFFT, hostInput);

        clEnqueueWriteBuffer(clQueue, clMatrixInput, true, OFFSET_ZERO, hostInput, null, null);
        fft2Dim(clFFTMatrix, paddWidth, paddHeight, direction); // swapp height and width as matrix is transpose
        clEnqueueReadBuffer(clQueue, clMatrixOutput, true, OFFSET_ZERO, hostOutput, null, null);

        return CLUtils.toDoubleArray(hostOutput, 2 * paddHeight * paddWidth);
    }

    /**
     * fft2Dim enqueues the 2-dim FFT Kernel and sets the direction argument of the kernel
     * @param clKernel
     * @param direction
     * @throws OpenCLException
     */
    private void fft2Dim(final long clKernel, int height, int width, Direction direction) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int workitems_total = paddWidth/max_fft_per_work_item;
            int workitems_per_workgroup = Math.min(workitems_total,max_workitems_per_workgroup); //paddWidth/max_fft_per_work_item;

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            clLocalWorkSizeEdges.put(DIMENSION_ZERO, workitems_per_workgroup); // number of workitems per workgroup
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, workitems_total); // number of workitems global

            // set height and width
            CLInfo.checkCLError(clSetKernelArg1i(clFFTMatrix, 3, height)); // matrix height
            CLInfo.checkCLError(clSetKernelArg1i(clFFTMatrix, 4, width)); // matrix width

            // set Direction for FFT
            CLInfo.checkCLError(clSetKernelArg1i(clFFTMatrix, 5, direction.getValue())); // FFT: 1 or IFFT: -1

            CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null));
            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    /**
     * computePaddWidth calculates the paddWidth from the matrixWidth and KernelSize and rounds up to the next power of two
     */
    public void computePaddWidth() {
        int N = matrixWidth + kernelSize - 1; // matrixWidth * 2 -> real and complex values
        int N2 = nextPowerOf2(N);
        this.paddWidth = N2; // times two for complex values
    }

    /**
     * computePaddHeight calculates the paddHeight from the matrixHeight and KernelSize and rounds up to the next power of two
     */
    public void computePaddHeight() {
        int M = matrixHeight + kernelSize - 1;
        int M2 = nextPowerOf2(M);
        this.paddHeight = M2;
    }

    /**
     * zeroPaddKernel padds the Kernel with zeros and adds the complex values which are zero
     * @param orginalKernel
     * @return paddedKernel
     */
    public double[] zeroPaddKernel(double[] orginalKernel) {

        double[] paddedKernel = new double[2*paddWidth];
        for (int i = 0; i < paddedKernel.length; ++i) {

            if (i%2 != 0 || i >= kernelSize*2) {
                paddedKernel[i] = 0;
            } else {
                paddedKernel[i] = orginalKernel[i/2];
            }
        }

        return paddedKernel;
    }

    /**
     * zeroPaddMatrix padds the matrix with zeros and adds the complex values which are zero
     * @param orginalMatrix
     * @return paddedMatrix
     */
    public double[] zeroPaddMatrix(double[] orginalMatrix) {
        double[] paddedMatrix = new double[paddHeight * 2 * paddWidth];

        for (int i = 0; i < paddedMatrix.length; ++i) {
            int col = i % (paddWidth*2);
            int row = i / (paddWidth*2);

            if (i%2 != 0 || !(col < matrixWidth*2 && row < matrixHeight)) {
                paddedMatrix[i] = 0;
            } else {
                paddedMatrix[i] = orginalMatrix[row * matrixWidth + col/2];
            }
        }
        return paddedMatrix;
    }

    /**
     * extractOriginalArea returns the convolution result with the same size as the original matrix
     * @param outputMatrix
     * @return originalMatrix
     */
    public double[] extractOriginalArea(double[] outputMatrix) { // TODO extract only real numbers

        double[] originalSizeMatrix = new double[matrixHeight * matrixWidth];
        int M = (matrixHeight + kernelSize - 1);
        int N = (matrixWidth + kernelSize - 1)*2;
        int offsetTop = (M - matrixHeight) / 2;
        int offsetLeft = (N - matrixWidth*2) / 2;

        for (int row = offsetTop; row < matrixHeight + offsetTop; ++row) {
            for (int col = offsetLeft; col < 2*matrixWidth + offsetLeft; ++col) {
                if (col%2 == 0) {
                    int indexPadded = row * paddWidth*2 + col;
                    int indexOriginal = (row - offsetTop) * matrixWidth + (col - offsetLeft)/2;
                    originalSizeMatrix[indexOriginal] = outputMatrix[indexPadded];
                }
            }
        }

        return originalSizeMatrix;
    }

    /**
     * nextPowerOf2 rounds n up to the next power of two
     * @param n
     * @return power of two neares to n
     */
    public static int nextPowerOf2(int n) {
        int exponent = (int) Math.ceil(Math.log((double) n) / Math.log(2.0));
        return (int) Math.pow(2, exponent);
    }


    /**
     * buildKernels compiles the kernels for the 2-dim FFT the 1-dim FFT and the multiplication
     * @throws OpenCLException
     */
    private void buildKernels() throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            PointerBuffer strings = stack.mallocPointer(MALLOC_SIZE);
            PointerBuffer lengths = stack.mallocPointer(MALLOC_SIZE);

            try {
                kernelSourceCode = CLUtils.ioResourceToByteBuffer("FFTConvolution64.cl", SOURCE_BUFFER_SIZE);
            } catch (IOException e) {
                throw new OpenCLException(e.getMessage());
            }

            strings.put(0, kernelSourceCode);
            lengths.put(0, kernelSourceCode.remaining());

            clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
            CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));

            clFFTKernel = clCreateKernel(clProgram, "fft1Dim", errcode_ret);
            CLInfo.checkCLError(errcode_ret);

            clFFTMatrix = clCreateKernel(clProgram, "fft2Dim", errcode_ret);
            CLInfo.checkCLError(errcode_ret);

            clMultiplyKernel = clCreateKernel(clProgram, "multiply", errcode_ret);
            CLInfo.checkCLError(errcode_ret);

        }

    }

    /*
    private PointerBuffer clEvent;
    clEvent = MemoryUtil.memAllocPointer(1);
    LongBuffer startTime;
    LongBuffer endTime;
    PointerBuffer retSize;


    private long enqueueNDRangeKernel(final String name, long command_queue, long kernel, int work_dim, PointerBuffer global_work_offset, PointerBuffer global_work_size, PointerBuffer local_work_size, PointerBuffer event_wait_list, PointerBuffer event) {
        if(profiling) {
            long result = clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, clEvent);
            clWaitForEvents(clEvent);
            long eventAddr = clEvent.get();
            clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_START, startTime, retSize);
            clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_END, endTime, retSize);
            clEvent.clear();
            // in nanaSec
            log.info(name + " event time " + "0x"+eventAddr + ": " + (endTime.get() - startTime.get()) + " ns");
            endTime.clear();
            startTime.clear();
            return result;
        }
        else {
            return clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, event);
        }
    }
    */

    /**
     * clearMemory releases the allocated memory objects
     * @throws OpenCLException
     */
    private void clearMemory() throws OpenCLException {
        try {
            CLInfo.checkCLError(clReleaseMemObject(clMatrixInput));
            CLInfo.checkCLError(clReleaseMemObject(clMatrixOutput));
            CLInfo.checkCLError(clReleaseMemObject(clGaussKernelInput));
            CLInfo.checkCLError(clReleaseMemObject(clGaussKernelTransformed));

            CLInfo.checkCLError(clReleaseKernel(clFFTMatrix));
            CLInfo.checkCLError(clReleaseKernel(clFFTKernel));
            CLInfo.checkCLError(clReleaseKernel(clMultiplyKernel));
        } catch (OpenCLException e) {
            throw e;
        } finally {
            MemoryUtil.memFree(hostInput);
            MemoryUtil.memFree(hostOutput);
            MemoryUtil.memFree(hostGaussKernelTransformed);
            MemoryUtil.memFree(kernelSourceCode);
            MemoryUtil.memFree(hostGaussKernelInput);
        }
    }

    /**
     * clearCL releses the OpenCL fields
     * @throws OpenCLException
     */
    public void clearCL() throws OpenCLException {
        clearMemory();
        CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
        CLInfo.checkCLError(clReleaseProgram(clProgram));
        CLInfo.checkCLError(clReleaseContext(clContext));
        contextCB.free();
        programCB.free();
    }


}
