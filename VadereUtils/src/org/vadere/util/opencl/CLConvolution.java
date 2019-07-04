package org.vadere.util.opencl;


import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_MEM_ALLOC_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.CL_MEM_WRITE_ONLY;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_STATUS;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * This class offers the convolution operation via OpenCL i.e. the use of the GPU to
 * accelerate the computation. Successive convolutions can be computed by reusing memory if the size of
 * the involved matrices do not change, that is, {@link CLConvolution#clearCL()} can be called
 * after multiple convolutions are computed.
 *
 * @author Benedikt Zoennchen
 */
public class CLConvolution extends CLOperation {
    private static Logger log = Logger.getLogger(CLConvolution.class);

    // CL Memory
    private long clInput;
    private long clOutput;
    private long clGaussianKernel;
    private long clTmp;

    // Host Memory
    private FloatBuffer hostScenario;
    private FloatBuffer hostGaussKernel;
    private FloatBuffer output;

    private ByteBuffer source;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;

    // CL kernel
    private long clKernelConvolve;
    private long clKernelConvolveRow;
    private long clKernelConvolveCol;

    private long clKernel;

    private int matrixWidth;
    private int matrixHeight;
    private int kernelWidth;
    private float[] kernel;
    private KernelType type;
    private boolean debug = false;

    public enum KernelType {
        Separate,
        Col,
        Row,
        NonSeparate
    }

    public CLConvolution(
            final int matrixWidth,
            final int matrixHeight,
            final int kernelWidth, @NotNull final float[] kernel) throws OpenCLException {
        this(KernelType.Separate, matrixWidth, matrixHeight, kernelWidth, kernel);
    }

	/**
	 * Default constructor.
	 *
	 * @param type              kernel type e.g. separated kernel
	 * @param matrixWidth       input matrix width
	 * @param matrixHeight      input matrix height
	 * @param kernelWidth       kernel width which is also the kernel height
	 * @param kernel            a 1D error representing the kernel (this can be a 1D kernel or a 2D kernel depending on the <tt>type</tt>)
	 *
	 * @throws OpenCLException      if there is some OpenCL problem e.g. it is not supported
	 * @throws UnsatisfiedLinkError if native libraries for LWJGL are missing
	 */
    public CLConvolution(
            @NotNull final KernelType type,
            final int matrixWidth,
            final int matrixHeight,
            final int kernelWidth,
            @NotNull final float[] kernel) throws OpenCLException, UnsatisfiedLinkError {
        this.type = type;
        this.matrixHeight = matrixHeight;
        this.matrixWidth = matrixWidth;
        this.kernelWidth = kernelWidth;
        this.kernel = kernel;

        // enables debug memory operations
        if(debug) {
	        Configuration.DEBUG.set(true);
	        Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
	        Configuration.DEBUG_STACK.set(true);
        }
        init();
    }

    public void init() throws OpenCLException, BootstrapMethodError {
        initCallbacks();
        initCL();
        buildProgram();

        hostGaussKernel = CLUtils.toFloatBuffer(kernel);
        hostScenario = MemoryUtil.memAllocFloat(matrixWidth * matrixHeight);
        output = MemoryUtil.memAllocFloat(matrixWidth * matrixHeight);

        switch (type) {
            case NonSeparate: clKernel = clKernelConvolve; break;
            case Col: clKernel = clKernelConvolveCol; break;
            case Row: clKernel = clKernelConvolveRow; break;
            case Separate: clKernel = -1; break;
            default: throw new IllegalArgumentException("unsupported kernel type = " + type);
        }

        if(type != KernelType.Separate) {
            setArguments(clKernel);
        }
        else {
            setArguments(clKernelConvolveCol, clKernelConvolveRow);
        }
    }

	/**
	 * Executes the convolution operation this might be a 2D convolution realized by one 2D kernel
	 * or by two 1D kernels or just one 1D convolution which depends on the constructor arguments of this
	 * class.
	 *
	 * @param input the input matrix
	 *
	 * @return the output matrix of same dimension as the input matrix
	 *
	 * @throws OpenCLException if there is any problem with OpenCL e.g. no OpenCL support
	 */
	public float[] convolve(final float[] input) throws OpenCLException {
        // 1. write input to native-c-like-memory
        CLUtils.toFloatBuffer(input, hostScenario);

        // 2. write this memory to the GPU
        clEnqueueWriteBuffer(clQueue, clInput, true, 0, hostScenario, null, null);

        // 2. convolve
        switch (type) {
            case NonSeparate: convolve(clKernelConvolve); break;
            case Col: convolve(clKernelConvolveCol); break;
            case Row: convolve(clKernelConvolveRow); break;
            case Separate: convolveSeparate(); break;
            default: throw new IllegalArgumentException("unsupported kernel type = " + type);
        }

        // 4. read result from the GPU to a native-c-like-memory
        clEnqueueReadBuffer(clQueue, clOutput, true, 0, output, null, null);

        // 5. read this memory and transform it back into a java array.
        float[] foutput = CLUtils.toFloatArray(output, matrixWidth * matrixHeight);
        return foutput;
    }


    private void convolveSeparate() throws OpenCLException {
        //init();
        try (MemoryStack stack = stackPush()) {
            PointerBuffer clGlobalWorkSizeEdges = stack.mallocPointer(2);
            clGlobalWorkSizeEdges.put(0, matrixWidth);
	        clGlobalWorkSizeEdges.put(1, matrixHeight);

            PointerBuffer ev = stack.mallocPointer(1);
            // run the kernel and read the result
	        CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernelConvolveCol, 2, null, clGlobalWorkSizeEdges, null, null, null));
	        CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernelConvolveRow, 2, null, clGlobalWorkSizeEdges, null, null, null));
            clFinish(clQueue);
        }
    }

    private void convolve(final long clKernel) throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    PointerBuffer clGlobalWorkSizeEdges = stack.mallocPointer(2);
		    clGlobalWorkSizeEdges.put(0, matrixWidth);
		    clGlobalWorkSizeEdges.put(1, matrixHeight);

		    // run the kernel and read the result
		    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, 2, null, clGlobalWorkSizeEdges, null, null, null));
		    CLInfo.checkCLError(clFinish(clQueue));
	    }
    }

    private void setArguments(final long clKernel) throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.mallocInt(+1);

		    // host memory to gpu memory
		    clInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * matrixWidth * matrixHeight, errcode_ret);
		    clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, 4 * matrixWidth * matrixHeight, errcode_ret);
		    clGaussianKernel = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, hostGaussKernel, errcode_ret);

		    CLInfo.checkCLError(clSetKernelArg1p(clKernel, 0, clInput));
		    CLInfo.checkCLError(clSetKernelArg1p(clKernel, 1, clGaussianKernel));
		    CLInfo.checkCLError(clSetKernelArg1p(clKernel, 2, clOutput));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernel, 3, matrixWidth));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernel, 4, matrixHeight));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernel, 5, kernelWidth));
	    }
    }

    private void setArguments(final long clKernelConvolveCol, final long clKernelConvolveRow) throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.mallocInt(1);

		    clTmp = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * matrixWidth * matrixHeight, errcode_ret);
		    clInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * matrixWidth * matrixHeight, errcode_ret);
		    clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, 4 * matrixWidth * matrixHeight, errcode_ret);
		    clGaussianKernel = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, hostGaussKernel, errcode_ret);

		    CLInfo.checkCLError(clSetKernelArg1p(clKernelConvolveCol, 0, clInput));
		    CLInfo.checkCLError(clSetKernelArg1p(clKernelConvolveCol, 1, clGaussianKernel));
		    CLInfo.checkCLError(clSetKernelArg1p(clKernelConvolveCol, 2, clTmp));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernelConvolveCol, 3, matrixWidth));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernelConvolveCol, 4, matrixHeight));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernelConvolveCol, 5, kernelWidth));

		    CLInfo.checkCLError(clSetKernelArg1p(clKernelConvolveRow, 0, clTmp));
		    CLInfo.checkCLError(clSetKernelArg1p(clKernelConvolveRow, 1, clGaussianKernel));
		    CLInfo.checkCLError(clSetKernelArg1p(clKernelConvolveRow, 2, clOutput));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernelConvolveRow, 3, matrixWidth));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernelConvolveRow, 4, matrixHeight));
		    CLInfo.checkCLError(clSetKernelArg1i(clKernelConvolveRow, 5, kernelWidth));
	    }
    }

    private void clearMemory() throws OpenCLException {
        // release memory and devices

	    try {
		    CLInfo.checkCLError(clReleaseMemObject(clInput));
		    CLInfo.checkCLError(clReleaseMemObject(clOutput));
		    CLInfo.checkCLError(clReleaseMemObject(clGaussianKernel));

		    if(type == KernelType.Separate) {
			    CLInfo.checkCLError(clReleaseMemObject(clTmp));
		    }

		    CLInfo.checkCLError(clReleaseKernel(clKernelConvolve));
		    CLInfo.checkCLError(clReleaseKernel(clKernelConvolveRow));
		    CLInfo.checkCLError(clReleaseKernel(clKernelConvolveCol));
	    }
	    catch (OpenCLException ex) {
			throw ex;
	    }
		finally {
		    MemoryUtil.memFree(hostScenario);
		    MemoryUtil.memFree(output);
		    MemoryUtil.memFree(hostGaussKernel);
		    MemoryUtil.memFree(source);
	    }
    }

	/**
	 * Works like a C++ destructor, i.e. frees host and GPU / device memory.
	 * This has to be called if no more convolutions are computed. Note that
	 * successive convolutions can be computed by reusing memory if the size of
	 * the involved matrices do not change.
	 *
	 * @throws OpenCLException if there is any problem with OpenCL e.g. no OpenCL support
	 */
	public void clearCL() throws OpenCLException {
    	clearMemory();
    	super.clearCL();
    }

    private void buildProgram() throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.mallocInt(1);

		    PointerBuffer strings = stack.mallocPointer(1);
		    PointerBuffer lengths = stack.mallocPointer(1);

		    try {
			    source = CLUtils.ioResourceToByteBuffer("Convolve.cl", 4096);
		    } catch (IOException e) {
			    throw new OpenCLException(e.getMessage());
		    }


		    strings.put(0, source);
		    lengths.put(0, source.remaining());

		    clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
		    CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));
		    clKernelConvolve = clCreateKernel(clProgram, "convolve", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clKernelConvolveRow = clCreateKernel(clProgram, "convolveRow", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clKernelConvolveCol = clCreateKernel(clProgram, "convolveCol", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
	    }

    }
}
