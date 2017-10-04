package org.vadere.util.math;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.MemoryStack;
import org.vadere.util.opencl.IOUtil;
import org.vadere.util.opencl.InfoUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 */
public class CLConvolution {
    private static Logger log = LogManager.getLogger(CLConvolution.class);

    private Convolution gaussianFilter;

    // CL ids
    private MemoryStack stack;
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

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;

    // CL kernel
    private long clKernelConvolve;
    private long clKernelConvolveRow;
    private long clKernelConvolveCol;

    public CLConvolution() {
        this.stack = MemoryStack.stackPush();
    }

    public void init() {
        initCallbacks();
        initCL();
        buildProgram();
    }

    public float[] convolve(final float[] input,
                            final int matrixWidth,
                            final int matrixHeight,
                            final float[] kernel,
                            final int kernelWidth) {
        init();
        float[] result = convolve(input, matrixWidth, matrixHeight, kernel, kernelWidth, clKernelConvolve);
        clearCL();
        clReleaseKernel(clKernelConvolve);
        return result;
    }

    public float[] convolveRow(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel,
                               final int kernelWidth) {
        init();
        float[] result = convolve(input, matrixWidth, matrixHeight, kernel, kernelWidth, clKernelConvolveRow);
        clearCL();
        clReleaseKernel(clKernelConvolveRow);
        return result;
    }

    public float[] convolveCol(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel,
                               final int kernelWidth) {
        init();
        float[] result = convolve(input, matrixWidth, matrixHeight, kernel, kernelWidth, clKernelConvolveCol);
        clearCL();
        clReleaseKernel(clKernelConvolveCol);
        return result;
    }

    public float[] convolveSeparate(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel,
                                    final int kernelWidth) {
        assert matrixWidth * matrixHeight == input.length;
        init();
        hostScenario = CLUtils.toFloatBuffer(input);
        output = CLUtils.toFloatBuffer(input);
        hostGaussKernel = CLUtils.toFloatBuffer(kernel);

        // host memory to gpu memory
        clInput = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, hostScenario, errcode_ret);
        clTmp = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * input.length, errcode_ret);
        clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, 4 * input.length, errcode_ret);
        clGaussianKernel = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, hostGaussKernel, errcode_ret);

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

        PointerBuffer clGlobalWorkSizeEdges = BufferUtils.createPointerBuffer(2);
        clGlobalWorkSizeEdges.put(0, matrixWidth);
        clGlobalWorkSizeEdges.put(1, matrixHeight);

        // run the kernel and read the result
        clEnqueueNDRangeKernel(clQueue, clKernelConvolveCol, 2, null, clGlobalWorkSizeEdges, null, null, null);
        clEnqueueNDRangeKernel(clQueue, clKernelConvolveRow, 2, null, clGlobalWorkSizeEdges, null, null, null);
        clFinish(clQueue);
        clEnqueueReadBuffer(clQueue, clOutput, true, 0, output, null, null);

        float[] foutput = CLUtils.toFloatArray(output, input.length);
        clearCL();
        clReleaseKernel(clTmp);
        clReleaseKernel(clKernelConvolve);
        clReleaseKernel(clKernelConvolveRow);
        clReleaseKernel(clKernelConvolveCol);
        return foutput;
    }

    private float[] convolve(final float[] input,
                             final int matrixWidth,
                             final int matrixHeight,
                             final float[] kernel,
                             final int kernelWidth, final long clKernel) {
        assert matrixWidth * matrixHeight == input.length;

        setArguments(input, matrixWidth, matrixHeight, kernel, kernelWidth, clKernel);

        PointerBuffer clGlobalWorkSizeEdges = BufferUtils.createPointerBuffer(2);
        clGlobalWorkSizeEdges.put(0, matrixWidth);
        clGlobalWorkSizeEdges.put(1, matrixHeight);

        // run the kernel and read the result
        clEnqueueNDRangeKernel(clQueue, clKernel, 2, null, clGlobalWorkSizeEdges, null, null, null);
        clFinish(clQueue);
        clEnqueueReadBuffer(clQueue, clOutput, true, 0, output, null, null);

        float[] foutput = CLUtils.toFloatArray(output, input.length);
        return foutput;
    }

    private void setArguments(final float[] input, final int matrixWidth, final int matrixHeight, final float[] kernel, final int kernelWidth, final long clKernel) {
        hostScenario = CLUtils.toFloatBuffer(input);
        output = CLUtils.toFloatBuffer(input);
        hostGaussKernel = CLUtils.toFloatBuffer(kernel);

        // host memory to gpu memory
        clInput = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, hostScenario, errcode_ret);
        clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, 4 * input.length, errcode_ret);
        clGaussianKernel = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, hostGaussKernel, errcode_ret);

        clSetKernelArg1p(clKernel, 0, clInput);
        clSetKernelArg1p(clKernel, 1, clGaussianKernel);
        clSetKernelArg1p(clKernel, 2, clOutput);
        clSetKernelArg1i(clKernel, 3, matrixWidth);
        clSetKernelArg1i(clKernel, 4, matrixHeight);
        clSetKernelArg1i(clKernel, 5, kernelWidth);
    }

    private void clearCL() {
        // release memory and devices
        contextCB.free();
        programCB.free();
        clReleaseMemObject(clInput);
        clReleaseMemObject(clOutput);
        clReleaseCommandQueue(clQueue);
        clReleaseProgram(clProgram);
        clReleaseContext(clContext);
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
            log.info("The cl_program [0x"+program+"] was built " + (InfoUtils.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
        });
    }

    private void initCL() {
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
        InfoUtils.checkCLError(errcode_ret);

        clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);
    }

    private void buildProgram() {
        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        PointerBuffer lengths = BufferUtils.createPointerBuffer(1);

        ByteBuffer source;
        try {
            source = IOUtil.ioResourceToByteBuffer("Convolve.cl", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        strings.put(0, source);
        lengths.put(0, source.remaining());

        clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);

        int errcode = clBuildProgram(clProgram, clDevice, "", programCB, NULL);
        InfoUtils.checkCLError(errcode);
        clKernelConvolve = clCreateKernel(clProgram, "convolve", errcode_ret);
        clKernelConvolveRow = clCreateKernel(clProgram, "convolveRow", errcode_ret);
        clKernelConvolveCol = clCreateKernel(clProgram, "convolveCol", errcode_ret);
    }

    private static void printPlatformInfo(long platform, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + InfoUtils.getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) {
        System.out.println("\t" + param_name + " = " + InfoUtils.getDeviceInfoStringUTF8(device, param));
    }
}
