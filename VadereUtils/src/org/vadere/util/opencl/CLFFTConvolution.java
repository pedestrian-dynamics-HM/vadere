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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;


public class CLFFTConvolution {

    private static final int OFFSET_ZERO = 0;
    private static final int SOURCE_BUFFER_SIZE = 4096;
    private static final int BUFFER_SIZE_FACTOR = 4;
    private static final int MALLOC_SIZE = 1;
    private static final int DIMENSION_ZERO = 0;
    private static final int WORK_DIM = 1;
    private static final int FLOAT_PRECITION = 8;
    private static final int MIN_POINTS_PER_WORKITEM = 8;

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
    private FloatBuffer hostInput;
    private FloatBuffer hostOutput;
    private FloatBuffer hostComplexGaussKernel;
    private FloatBuffer hostRealGaussKernel;
    private FloatBuffer hostGaussKernelTransformed;

    // kernel code
    private ByteBuffer kernelSourceCode;

    private int matrixHeight;
    private int matrixWidth;
    private int kernelSize;
    private float[] gaussKernel;
    private float[] gaussKernelTransformed;

    private int paddHeight;
    private int paddWidth;

    // for global and local size
    private int max_workitems_per_workgroup;
    private int points_per_workitem;
    private int workitems_total_dim1;
    private int workitems_total_dim2;
    private int workitems_per_workgroup_dim1;
    private int workitems_per_workgroup_dim2;

    // Kernel Memory
    private long clMatrixInput;
    private long clMatrixOutput;
    private long clGaussKernelTransformed;
    private long clRealGaussKernel;
    private long clComplexGaussKernel;

    private long clFFTKernel;
    private long clFFTMatrix;
    private long clMultiplyKernel;
    private long clTransposeKernel;
    private long clComputeG;

    // logger for callbacks
    private static Logger log = Logger.getLogger(CLFFTConvolution.class);
    private boolean debug = false;
    private boolean profiling = false;
    private boolean padd;

    public enum Direction {
        SPACE2FREQUENCY(1), FREQUENCY2SPACE(-1);

        private final int val;

        Direction(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }
    }

    /**
     * FFT Convolution takes the 1 dimensional Argument gaussKernel and directly applys a FFT on it.
     * matrixHeight, matrixWidth and kernelSize are the dimensions for the real Data.
     *
     * @param matrixHeight height
     * @param matrixWidth  width
     * @param kernelSize   kernel size
     * @param gaussKernel  gaussian kernel vector 1 dim
     * @throws OpenCLException openCLException
     */


    public CLFFTConvolution(int matrixHeight, int matrixWidth, int kernelSize, @NotNull final float[] gaussKernel) throws OpenCLException {

        this(matrixHeight, matrixWidth, kernelSize, gaussKernel, true);

    }

    public int getPaddHeight() {
        return paddHeight;
    }

    public int getPaddWidth() {
        return paddWidth;
    }

    public CLFFTConvolution(int matrixHeight, int matrixWidth, int kernelSize, @NotNull final float[] gaussKernel, boolean padd) throws OpenCLException {
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
            this.gaussKernel = zeroPaddKernel(gaussKernel);
        } else {
            this.paddWidth = matrixWidth;
            this.paddHeight = matrixHeight;
            this.gaussKernel = gaussKernel;
        }

        init();
    }



    /**
     * init initializes all OpenCL fields, the openCL Callbacks for error messages. Also builds the Kernels for the
     * 2 - dimensional FFT, 1 - dimensional FFT and the multiplication
     * Both for the matrix and the gauss Kernel FFT the Memory is allocated,
     * hostGaussKernelTransformed holds the transformed gaussKernel
     *
     * @throws OpenCLException openCLException
     */
    public void init() throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            initCallbacks();
            initCL();
            buildKernels();

            computeLocalGlobalWorksize();

            // reserve memory for input matrix and gauss kernel
            hostInput = MemoryUtil.memAllocFloat(2 * paddHeight * paddWidth);
            hostOutput = MemoryUtil.memAllocFloat(2 * paddHeight * paddWidth);
            hostComplexGaussKernel = MemoryUtil.memAllocFloat(2 * paddWidth);
            hostRealGaussKernel = MemoryUtil.memAllocFloat(paddWidth);

            // create buffers
            // FFT 2dim
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);
            clMatrixInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, BUFFER_SIZE_FACTOR * 2 * paddHeight * paddWidth, errcode_ret);
            clMatrixOutput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, BUFFER_SIZE_FACTOR * 2 * paddHeight * paddWidth, errcode_ret);
            // FFT 1dim
            clRealGaussKernel = clCreateBuffer(clContext, CL_MEM_READ_WRITE, BUFFER_SIZE_FACTOR * 2 * paddWidth, errcode_ret);
            clComplexGaussKernel = clCreateBuffer(clContext, CL_MEM_READ_WRITE, BUFFER_SIZE_FACTOR * 2 * paddWidth, errcode_ret);

            setArgumentsFFT1Dim();

            if (padd) {
                gaussKernelTransformed = fftGaussKernel(); // uses the real FFT
            } else {
                gaussKernelTransformed = Arrays.copyOf(gaussKernel, gaussKernel.length); // for testing purposses skipp kernel fft
            }

            hostGaussKernelTransformed = CLUtils.toFloatBuffer(gaussKernelTransformed);
            // multiply
            clGaussKernelTransformed = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, hostGaussKernelTransformed, errcode_ret);

            setArgumentsMultiply();

        } catch (OpenCLException ex) {
            ex.printStackTrace();
        }
    }

    private void computeLocalGlobalWorksize() throws OpenCLException {
        // get divice specific constants
        long local_mem_size = CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
        max_workitems_per_workgroup = (int) CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);

        //determin biggest fft that can be computed with one workitem
        int max_points_per_workitem = (int) (local_mem_size / (max_workitems_per_workgroup * FLOAT_PRECITION));
        points_per_workitem = paddWidth < max_points_per_workitem ? MIN_POINTS_PER_WORKITEM : max_points_per_workitem;

        workitems_total_dim1 = (paddWidth/2)/(points_per_workitem/2);
        workitems_per_workgroup_dim1 = Math.min(workitems_total_dim1, max_workitems_per_workgroup);

        workitems_total_dim2 = (paddHeight * paddWidth) / points_per_workitem;
        workitems_per_workgroup_dim2 = Math.min(workitems_total_dim2, max_workitems_per_workgroup);
    }

    /**
     * initializes Callbacks for error messages
     */
    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
                log.debug("[LWJGL] cl_context_callback" + "\tInfo: " + memUTF8(errinfo)));

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
     *
     * @throws OpenCLException openCLException
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
            if (profiling) {
                clQueue = clCreateCommandQueue(clContext, clDevice, CL_QUEUE_PROFILING_ENABLE, errcode_ret);
            } else {
                clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);
            }
            CLInfo.checkCLError(errcode_ret);
        }
    }

    public float[] getGaussKernelTransformed() {
        return gaussKernelTransformed;
    }

    /**
     * Sets the kernel arguments for the 2 dimensional FFT
     *
     * @throws OpenCLException openCLException
     */
    private void setArgumentsFFT2Dim(long clInput, long clOutput, int height, int width, Direction direction) throws OpenCLException {
        CLInfo.checkCLError(clSetKernelArg1p(clFFTMatrix, 0, clInput)); // input
        CLInfo.checkCLError(clSetKernelArg(clFFTMatrix, 1, workitems_per_workgroup_dim2 * points_per_workitem * BUFFER_SIZE_FACTOR * 2)); // local memory size
        CLInfo.checkCLError(clSetKernelArg1p(clFFTMatrix, 2, clOutput)); // output
        CLInfo.checkCLError(clSetKernelArg1i(clFFTMatrix, 3, height)); // matrix height
        CLInfo.checkCLError(clSetKernelArg1i(clFFTMatrix, 4, width)); // matrix width
        CLInfo.checkCLError(clSetKernelArg1i(clFFTMatrix, 5, direction.getValue())); // FFT: 1 or IFFT: -1
    }

    /**
     * Sets the kernel arguments for the 1 dimensional FFT
     *
     * @throws OpenCLException openCLException
     */
    private void setArgumentsFFT1Dim() throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            // FFT
            CLInfo.checkCLError(clSetKernelArg1p(clFFTKernel, 0, clRealGaussKernel));
            CLInfo.checkCLError(clSetKernelArg(clFFTKernel, 1, paddWidth * BUFFER_SIZE_FACTOR)); // local memory size
            CLInfo.checkCLError(clSetKernelArg1i(clFFTKernel, 2, paddWidth / 2));

            // convert N/2 FFT to N FFT
            CLInfo.checkCLError(clSetKernelArg1p(clComputeG, 0, clRealGaussKernel));
            CLInfo.checkCLError(clSetKernelArg1p(clComputeG, 1, clComplexGaussKernel));
        }
    }

    /**
     * Sets the kernel arguments for the multiplication
     *
     * @throws OpenCLException openCLException
     */
    private void setArgumentsMultiply() throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            CLInfo.checkCLError(clSetKernelArg1p(clMultiplyKernel, 0, clMatrixInput));
            CLInfo.checkCLError(clSetKernelArg1p(clMultiplyKernel, 1, clGaussKernelTransformed));
            CLInfo.checkCLError(clSetKernelArg1p(clMultiplyKernel, 2, clMatrixOutput));

            CLInfo.checkCLError(clSetKernelArg1p(clTransposeKernel, 0, clMatrixInput));
            CLInfo.checkCLError(clSetKernelArg1p(clTransposeKernel, 1, clMatrixOutput));
        }
    }

    /**
     * Computes the linear convolution of the given matrix with the set Gaussian kernel
     *
     * @param matrix position matrix
     * @return densityMatrix with the size of the original matrix
     * @throws OpenCLException openCLException
     */
    public float[] convolve(final float[] matrix) throws OpenCLException {

        // padd Matrix
        float[] tmpMatrix = zeroPaddMatrix(matrix);
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
     *
     * @param matrix matrix in fourier space
     * @return outputMatrix
     * @throws OpenCLException openCLException
     */
    public float[] multiply(final float[] matrix, int height, int width) throws OpenCLException {

        CLUtils.toFloatBuffer(matrix, hostInput);
        clEnqueueWriteBuffer(clQueue, clMatrixInput, true, OFFSET_ZERO, hostInput, null, null);

        multiply(clMultiplyKernel, height, width);

        clEnqueueReadBuffer(clQueue, clMatrixOutput, true, OFFSET_ZERO, hostOutput, null, null);

        return CLUtils.toFloatArray(hostOutput, matrix.length);
    }

    public float[] computeG(final float[] vector, int width) throws OpenCLException {

        CLUtils.toFloatBuffer(vector, hostRealGaussKernel);
        clEnqueueWriteBuffer(clQueue, clRealGaussKernel, true, OFFSET_ZERO, hostRealGaussKernel, null, null);
        computeG(clComputeG, width);
        clEnqueueReadBuffer(clQueue, clComplexGaussKernel, true, OFFSET_ZERO, hostComplexGaussKernel, null, null);

        return CLUtils.toFloatArray(hostComplexGaussKernel, width*2);
    }

    private void computeG(final long clKernel, int width) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(1);
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, width/2); // only run to N/2
            PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(1);
            clLocalWorkSizeEdges.put(DIMENSION_ZERO, 1);

            CLInfo.checkCLError(clSetKernelArg1i(clComputeG, 2, width));

            int err_code = (int) enqueueNDRangeKernel("computeG", clQueue, clKernel, 1, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null);
            CLInfo.checkCLError(err_code);

            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    /**
     * multiply sets the local and global worksize and enqueues the multiply kernel
     *
     * @param clKernel kernel program for multiplication
     * @throws OpenCLException openCLException
     */
    private void multiply(final long clKernel, int height, int width) throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(2);

            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, height);
            clGlobalWorkSizeEdges.put(1, width);

            CLInfo.checkCLError(clSetKernelArg1i(clMultiplyKernel, 3, height));
            CLInfo.checkCLError(clSetKernelArg1i(clMultiplyKernel, 4, width));

            //CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue,clKernel,2,null,clGlobalWorkSizeEdges,null,null, null));
            int err_code = (int) enqueueNDRangeKernel("multiply", clQueue, clKernel, 2, null, clGlobalWorkSizeEdges, null, null, null);
            CLInfo.checkCLError(err_code);

            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    /**
     * multiply sets the local and global worksize and enqueues the multiply kernel
     *
     * @param clKernel kernel program for multiplication
     * @throws OpenCLException openCLException
     */
    private void transpose(final long clKernel, int height, int width) throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(2);

            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, height);
            clGlobalWorkSizeEdges.put(1, width);

            CLInfo.checkCLError(clSetKernelArg1i(clTransposeKernel, 2, height));
            CLInfo.checkCLError(clSetKernelArg1i(clTransposeKernel, 3, width));

            //CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue,clKernel,2,null,clGlobalWorkSizeEdges,null,null, null));
            int err_code = (int) enqueueNDRangeKernel("transpose", clQueue, clKernel, 2, null, clGlobalWorkSizeEdges, null, null, null);
            CLInfo.checkCLError(err_code);

            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    public float[] multiplyRows(final float[] matrix) throws OpenCLException {
        return multiply(matrix, paddHeight, paddWidth);
    }

    public float[] multiplyCols(final float[] matrix) throws OpenCLException {
        return multiply(matrix, paddWidth, paddHeight); // matrix is transposed
    }

    /**
     * fft1Dim computes the 1 dimensional FFT of the input in the given direction
     *
     * @param input     1 dim input vector
     * @param direction of fft
     * @return transformed input
     * @throws OpenCLException openCLException
     */
    public float[] fft1Dim(final float[] input, Direction direction) throws OpenCLException {
        // inner FFT
        CLUtils.toFloatBuffer(input, hostRealGaussKernel);
        clEnqueueWriteBuffer(clQueue, clRealGaussKernel, true, OFFSET_ZERO, hostRealGaussKernel, null, null);
        fft1Dim(clFFTKernel, direction);
        clEnqueueReadBuffer(clQueue, clRealGaussKernel, true, OFFSET_ZERO, hostRealGaussKernel, null, null);

        return CLUtils.toFloatArray(hostRealGaussKernel, input.length);
    }

    /**
     * fft1Dim sets the local and global worksize and enqueues the 1 dim FFT Kernel
     *
     * @param clKernel  kernel program
     * @param direction for fft
     * @throws OpenCLException openCLException
     */
    private void fft1Dim(final long clKernel, Direction direction) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            clLocalWorkSizeEdges.put(DIMENSION_ZERO, workitems_per_workgroup_dim1); // number of workitems per workgroup
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, workitems_total_dim1); // number of workitems global

            // set direction for FFT
            CLInfo.checkCLError(clSetKernelArg1i(clFFTKernel, 3, direction.getValue()));

            //CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null));
            int err_code = (int) enqueueNDRangeKernel("fft1dim", clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null);
            CLInfo.checkCLError(err_code);
            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    /**
     * fftGaussKernel computes the FFT transformation into frequency space of the set gauss kernel
     *
     * @return transformedGaussKernel in the frequency space
     * @throws OpenCLException openCLException
     */
    private float[] fftGaussKernel() throws OpenCLException {
        float[] tmp = fft1Dim(gaussKernel, Direction.SPACE2FREQUENCY);
        tmp = computeG(tmp, paddWidth);
        return tmp;
    }

    /**
     * fft2Dim compute the 2 dimensional FFT in the given direction. The input matrix is given in vector.
     *
     * @param input     2 dim input matrix as 1-dim vector
     * @param direction for fft
     * @return output transformed input matrix
     * @throws OpenCLException openCLException
     */
    public float[] fft2Dim(final float[] input, Direction direction) throws OpenCLException {

        // rows FFT
        CLUtils.toFloatBuffer(input, hostInput);
        clEnqueueWriteBuffer(clQueue, clMatrixInput, true, OFFSET_ZERO, hostInput, null, null);
        fft2Dim(clFFTMatrix, clMatrixInput, clMatrixOutput, paddHeight, paddWidth, direction);

        // columns FFT
        fft2Dim(clFFTMatrix, clMatrixOutput, clMatrixInput, paddWidth, paddHeight, direction); // swapp height and width as matrix is transpose
        clEnqueueReadBuffer(clQueue, clMatrixInput, true, OFFSET_ZERO, hostInput, null, null);

        return CLUtils.toFloatArray(hostInput, 2 * paddHeight * paddWidth);
    }

    /**
     * fft2Dim enqueues the 2-dim FFT Kernel and sets the direction argument of the kernel
     *
     * @param clKernel  kernel program
     * @param direction for fft
     * @throws OpenCLException openCLException
     */
    private void fft2Dim(final long clKernel, long clInput, long clOutput, int height, int width, Direction direction) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            setArgumentsFFT2Dim(clInput, clOutput, height, width, direction);

            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(WORK_DIM);
            
            clLocalWorkSizeEdges.put(DIMENSION_ZERO, workitems_per_workgroup_dim2); // number of workitems per workgroup
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, workitems_total_dim2); // number of workitems global

            //CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null));
            int err_code = (int) enqueueNDRangeKernel("fft2dim_" + direction.name(), clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null);
            CLInfo.checkCLError(err_code);
            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    /**
     * computePaddWidth calculates the paddWidth from the matrixWidth and KernelSize and rounds up to the next power of two
     */
    private void computePaddWidth() {
        this.paddWidth = nextPowerOf2(matrixWidth + kernelSize - 1); // times two for complex values
    }

    /**
     * computePaddHeight calculates the paddHeight from the matrixHeight and KernelSize and rounds up to the next power of two
     */
    private void computePaddHeight() {
        this.paddHeight = nextPowerOf2(matrixHeight + kernelSize - 1);
    }

    /**
     * zeroPaddKernel padds the Kernel with zeros and adds the complex values which are zero
     *
     * @param originalKernel befor zero padding
     * @return paddedKernel after zero padding
     */
    public float[] zeroPaddKernel(float[] originalKernel) {
        float[] paddedKernel = new float[paddWidth];
        System.arraycopy(originalKernel, 0, paddedKernel, 0, originalKernel.length);

        return paddedKernel;
    }

    /**
     * zeroPaddMatrix padds the matrix with zeros and adds the complex values which are zero
     *
     * @param orginalMatrix input matrix befor zero padding
     * @return paddedMatrix after zero padding
     */
    public float[] zeroPaddMatrix(float[] orginalMatrix) {
        float[] paddedMatrix = new float[paddHeight * 2 * paddWidth];

        for (int i = 0; i < paddedMatrix.length; ++i) {
            int col = i % (paddWidth * 2);
            int row = i / (paddWidth * 2);

            if (i % 2 != 0 || !(col < matrixWidth * 2 && row < matrixHeight)) {
                paddedMatrix[i] = 0;
            } else {
                paddedMatrix[i] = orginalMatrix[row * matrixWidth + col / 2];
            }
        }
        return paddedMatrix;
    }

    /**
     * extractOriginalArea returns the convolution result with the same size as the original matrix
     *
     * @param outputMatrix convolved matrix still with zero padding
     * @return originalMatrix
     */
    public float[] extractOriginalArea(float[] outputMatrix) {

        float[] originalSizeMatrix = new float[matrixHeight * matrixWidth];
        int M = (matrixHeight + kernelSize - 1);
        int N = (matrixWidth + kernelSize - 1) * 2;
        int offsetTop = (M - matrixHeight) / 2;
        int offsetLeft = (N - matrixWidth * 2) / 2;

        for (int row = offsetTop; row < matrixHeight + offsetTop; ++row) {
            for (int col = offsetLeft; col < 2 * matrixWidth + offsetLeft; ++col) {
                if (col % 2 == 0) {
                    int indexPadded = row * paddWidth * 2 + col;
                    int indexOriginal = (row - offsetTop) * matrixWidth + (col - offsetLeft) / 2;
                    originalSizeMatrix[indexOriginal] = outputMatrix[indexPadded];
                }
            }
        }

        return originalSizeMatrix;
    }

    /**
     * nextPowerOf2 rounds n up to the next power of two
     *
     * @param n number
     * @return power of two neares to n
     */
    public static int nextPowerOf2(int n) {
        int exponent = (int) Math.ceil(Math.log((double) n) / Math.log(2.0));
        return (int) Math.pow(2, exponent);
    }


    /**
     * buildKernels compiles the kernels for the 2-dim FFT the 1-dim FFT and the multiplication
     *
     * @throws OpenCLException openCLException
     */
    private void buildKernels() throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            PointerBuffer strings = stack.mallocPointer(MALLOC_SIZE);
            PointerBuffer lengths = stack.mallocPointer(MALLOC_SIZE);

            try {
                kernelSourceCode = CLUtils.ioResourceToByteBuffer("FFTConvolution.cl", SOURCE_BUFFER_SIZE);
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

            clTransposeKernel = clCreateKernel(clProgram, "transpose", errcode_ret);
            CLInfo.checkCLError(errcode_ret);

            clComputeG = clCreateKernel(clProgram, "computeG", errcode_ret);
            CLInfo.checkCLError(errcode_ret);
        }

    }


    private PointerBuffer clEvent = MemoryUtil.memAllocPointer(1);
    LongBuffer startTime;
    LongBuffer endTime;
    PointerBuffer retSize;

    private long enqueueNDRangeKernel(final String name, long command_queue, long kernel, int work_dim, PointerBuffer global_work_offset, PointerBuffer global_work_size, PointerBuffer local_work_size, PointerBuffer event_wait_list, PointerBuffer event) {
        if (profiling) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                retSize = stack.mallocPointer(1);
                startTime = stack.mallocLong(1);
                endTime = stack.mallocLong(1);

                long result = clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, clEvent);
                clWaitForEvents(clEvent);
                long eventAddr = clEvent.get();
                clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_START, startTime, retSize);
                clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_END, endTime, retSize);
                clEvent.clear();
                // in nanaSec
                log.info(name + " " + (endTime.get() - startTime.get()));
                endTime.clear();
                startTime.clear();
                return result;
            }
        } else {
            return clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, null, null);
        }
    }

    /**
     * clearMemory releases the allocated memory objects
     *
     * @throws OpenCLException openCLException
     */
    private void clearMemory() throws OpenCLException {
        try {
            CLInfo.checkCLError(clReleaseMemObject(clMatrixInput));
            CLInfo.checkCLError(clReleaseMemObject(clMatrixOutput));
            CLInfo.checkCLError(clReleaseMemObject(clGaussKernelTransformed));
            CLInfo.checkCLError(clReleaseMemObject(clComplexGaussKernel));
            CLInfo.checkCLError(clReleaseMemObject(clRealGaussKernel));

            CLInfo.checkCLError(clReleaseKernel(clFFTMatrix));
            CLInfo.checkCLError(clReleaseKernel(clFFTKernel));
            CLInfo.checkCLError(clReleaseKernel(clMultiplyKernel));
            CLInfo.checkCLError(clReleaseKernel(clTransposeKernel));
            CLInfo.checkCLError(clReleaseKernel(clComputeG));
        } catch (OpenCLException e) {
            e.printStackTrace();
        } finally {
            MemoryUtil.memFree(hostInput);
            MemoryUtil.memFree(hostOutput);
            MemoryUtil.memFree(hostComplexGaussKernel);
            MemoryUtil.memFree(hostGaussKernelTransformed);
            MemoryUtil.memFree(kernelSourceCode);


        }
    }

    /**
     * clearCL releses the OpenCL fields
     *
     * @throws OpenCLException openCLException
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
