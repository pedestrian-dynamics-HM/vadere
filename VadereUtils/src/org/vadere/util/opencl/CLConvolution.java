package org.vadere.util.opencl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.math.Convolution;

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

    private Convolution gaussianFilter;

    // CL ids
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clQueue;
    private long clProgram;

    // error code buffer
    private IntBuffer errcode_ret;

    // CL Memory
    private long clInput;
    private long clOutput;
    private long clGaussianKernel;
    private long clTmp;

    // Host Memory
    private FloatBuffer hostScenario;
    private FloatBuffer hostGaussKernel;
    private FloatBuffer output;

    private PointerBuffer strings;
    private PointerBuffer lengths;

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

    public enum KernelType {
        Separate,
        Col,
        Row,
        NonSeparate
    }

    public CLConvolution(
            final int matrixWidth,
            final int matrixHeight,
            final int kernelWidth, @NotNull final float[] kernel) {
        this(KernelType.Separate, matrixWidth, matrixHeight, kernelWidth, kernel);
    }

    public CLConvolution(
            @NotNull final KernelType type,
            final int matrixWidth,
            final int matrixHeight,
            final int kernelWidth, @NotNull final float[] kernel) {
        this.type = type;
        this.matrixHeight = matrixHeight;
        this.matrixWidth = matrixWidth;
        this.kernelWidth = kernelWidth;
        this.kernel = kernel;

        init();
        Configuration.DEBUG.set(true);
        Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
        Configuration.DEBUG_STACK.set(true);
    }

    public void init() {
        initCallbacks();
        initCL();
        buildProgram();

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

    public float[] convolve(final float[] input) {
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


    private void convolveSeparate() {
        //init();
        try (MemoryStack stack = stackPush()) {

            PointerBuffer clGlobalWorkSizeEdges = BufferUtils.createPointerBuffer(2);
            clGlobalWorkSizeEdges.put(0, matrixWidth);
            clGlobalWorkSizeEdges.put(1, matrixHeight);

            PointerBuffer ev = stack.callocPointer(1);
            // run the kernel and read the result
            clEnqueueNDRangeKernel(clQueue, clKernelConvolveCol, 2, null, clGlobalWorkSizeEdges, null, null, null);
            clEnqueueNDRangeKernel(clQueue, clKernelConvolveRow, 2, null, clGlobalWorkSizeEdges, null, null, null);
            clFinish(clQueue);
        }
    }

    private void convolve(final long clKernel) {
        PointerBuffer clGlobalWorkSizeEdges = BufferUtils.createPointerBuffer(2);
        clGlobalWorkSizeEdges.put(0, matrixWidth);
        clGlobalWorkSizeEdges.put(1, matrixHeight);

        // run the kernel and read the result
        clEnqueueNDRangeKernel(clQueue, clKernel, 2, null, clGlobalWorkSizeEdges, null, null, null);
        clFinish(clQueue);
    }

    private void setArguments(final long clKernel) {
        // host memory to gpu memory
        clInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * matrixWidth * matrixHeight, errcode_ret);
        clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, 4 * matrixWidth * matrixHeight, errcode_ret);
        clGaussianKernel = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, hostGaussKernel, errcode_ret);

        clSetKernelArg1p(clKernel, 0, clInput);
        clSetKernelArg1p(clKernel, 1, clGaussianKernel);
        clSetKernelArg1p(clKernel, 2, clOutput);
        clSetKernelArg1i(clKernel, 3, matrixWidth);
        clSetKernelArg1i(clKernel, 4, matrixHeight);
        clSetKernelArg1i(clKernel, 5, kernelWidth);
    }

    private void setArguments(final long clKernelConvolveCol, final long clKernelConvolveRow) {
        clTmp = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * matrixWidth * matrixHeight, errcode_ret);
        clInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * matrixWidth * matrixHeight, errcode_ret);
        clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, 4 * matrixWidth * matrixHeight, errcode_ret);
        clGaussianKernel = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, hostGaussKernel, errcode_ret);

        clSetKernelArg1p(clKernelConvolveCol, 0, clInput);
        clSetKernelArg1p(clKernelConvolveCol, 1, clGaussianKernel);
        clSetKernelArg1p(clKernelConvolveCol, 2, clTmp);
        clSetKernelArg1i(clKernelConvolveCol, 3, matrixWidth);
        clSetKernelArg1i(clKernelConvolveCol, 4, matrixHeight);
        clSetKernelArg1i(clKernelConvolveCol, 5, kernelWidth);

        clSetKernelArg1p(clKernelConvolveRow, 0, clTmp);
        clSetKernelArg1p(clKernelConvolveRow, 1, clGaussianKernel);
        clSetKernelArg1p(clKernelConvolveRow, 2, clOutput);
        clSetKernelArg1i(clKernelConvolveRow, 3, matrixWidth);
        clSetKernelArg1i(clKernelConvolveRow, 4, matrixHeight);
        clSetKernelArg1i(clKernelConvolveRow, 5, kernelWidth);
    }

    private void clearMemory() {
        // release memory and devices

        clReleaseMemObject(clInput);
        clReleaseMemObject(clOutput);
        clReleaseMemObject(clGaussianKernel);

        if(type == KernelType.Separate) {
            clReleaseMemObject(clTmp);
        }

        clReleaseKernel(clKernelConvolve);
        clReleaseKernel(clKernelConvolveRow);
        clReleaseKernel(clKernelConvolveCol);

        MemoryUtil.memFree(hostScenario);
        MemoryUtil.memFree(output);
        MemoryUtil.memFree(hostGaussKernel);
        //CL.destroy();
//        strings.free();
//        lengths.free();
    }

    public void clearCL() {
        clearMemory();
        contextCB.free();
        programCB.free();
        log.info("release command queue: " + (clReleaseCommandQueue(clQueue) == CL_SUCCESS));
        log.info("release program: " + (clReleaseProgram(clProgram) == CL_SUCCESS));
        log.info("release context: " + (clReleaseContext(clContext) == CL_SUCCESS));
    }

    // private helpers
    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.warn("[LWJGL] cl_context_callback");
            log.warn("\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
            log.info("The cl_program [0x"+program+"] was built " + (CLInfo.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
        });
    }

    private void initCL() {
        try (MemoryStack stack = stackPush()) {
            // helper for the memory allocation in java
            //stack = MemoryStack.stackPush();
            errcode_ret = stack.callocInt(1);

            IntBuffer numberOfPlatforms = stack.mallocInt(1);
            clGetPlatformIDs(null, numberOfPlatforms);
            PointerBuffer platformIDs = stack.mallocPointer(numberOfPlatforms.get(0));
            clGetPlatformIDs(platformIDs, numberOfPlatforms);

            clPlatform = platformIDs.get(0);

            IntBuffer numberOfDevices = stack.mallocInt(1);
            clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, null, numberOfDevices);
            PointerBuffer deviceIDs = stack.mallocPointer(numberOfDevices.get(0));
            clGetDeviceIDs(clPlatform, CL_DEVICE_TYPE_GPU, deviceIDs, numberOfDevices);

            clDevice = deviceIDs.get(0);

            printDeviceInfo(clDevice, "CL_DEVICE_NAME", CL_DEVICE_NAME);

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

    private void buildProgram() {
        strings = BufferUtils.createPointerBuffer(1);
        lengths = BufferUtils.createPointerBuffer(1);

        ByteBuffer source;
        try {
            source = CLUtils.ioResourceToByteBuffer("Convolve.cl", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        strings.put(0, source);
        lengths.put(0, source.remaining());

        clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);

        int errcode = clBuildProgram(clProgram, clDevice, "", programCB, NULL);
        CLInfo.checkCLError(errcode);
        clKernelConvolve = clCreateKernel(clProgram, "convolve", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelConvolveRow = clCreateKernel(clProgram, "convolveRow", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
        clKernelConvolveCol = clCreateKernel(clProgram, "convolveCol", errcode_ret);
        CLInfo.checkCLError(errcode_ret);
    }

    private static void printPlatformInfo(long platform, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + CLInfo.getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + CLInfo.getDeviceInfoStringUTF8(device, param));
    }
}
