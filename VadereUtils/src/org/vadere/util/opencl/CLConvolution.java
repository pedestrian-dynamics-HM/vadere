package org.vadere.util.opencl;

import org.apache.log4j.LogManager;
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

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 */
public class CLConvolution {
    private static Logger log = LogManager.getLogger(CLConvolution.class);

    // CL ids
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clQueue;
    private long clProgram;

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

    public CLConvolution(
            @NotNull final KernelType type,
            final int matrixWidth,
            final int matrixHeight,
            final int kernelWidth, @NotNull final float[] kernel) throws OpenCLException {
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

    public void init() throws OpenCLException {
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
            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(2);
            clGlobalWorkSizeEdges.put(0, matrixWidth);
	        clGlobalWorkSizeEdges.put(1, matrixHeight);

            PointerBuffer ev = stack.callocPointer(1);
            // run the kernel and read the result
	        CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernelConvolveCol, 2, null, clGlobalWorkSizeEdges, null, null, null));
	        CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernelConvolveRow, 2, null, clGlobalWorkSizeEdges, null, null, null));
            clFinish(clQueue);
        }
    }

    private void convolve(final long clKernel) throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(2);
		    clGlobalWorkSizeEdges.put(0, matrixWidth);
		    clGlobalWorkSizeEdges.put(1, matrixHeight);

		    // run the kernel and read the result
		    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, 2, null, clGlobalWorkSizeEdges, null, null, null));
		    CLInfo.checkCLError(clFinish(clQueue));
	    }
    }

    private void setArguments(final long clKernel) throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.callocInt(1);

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
		    IntBuffer errcode_ret = stack.callocInt(1);

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

    public void clearCL() throws OpenCLException {
    	clearMemory();
	    CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
	    CLInfo.checkCLError(clReleaseProgram(clProgram));
	    CLInfo.checkCLError(clReleaseContext(clContext));
	    contextCB.free();
	    programCB.free();
    }

    // private helpers
    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.debug("[LWJGL] cl_context_callback" + "\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
	        try {
		        log.debug("The cl_program [0x"+program+"] was built " + (CLInfo.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
	        } catch (OpenCLException e) {
		        e.printStackTrace();
	        }
        });
    }

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

    private void buildProgram() throws OpenCLException {
	    try (MemoryStack stack = stackPush()) {
		    IntBuffer errcode_ret = stack.callocInt(1);

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
