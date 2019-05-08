package org.vadere.util.opencl.examples;


import org.vadere.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;
import org.vadere.util.opencl.CLInfo;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * Created by bzoennchen on 08.09.17.
 */
public class CLVectorSum {

    // CL ids
    private MemoryStack stack;
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clKernel;
    private long clQueue;
    private long clProgram;

    // error code buffer
    private IntBuffer errcode_ret;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;
    private static Logger log = Logger.getLogger(CLVectorSum.class);

    // data on the host
    private FloatBuffer a;
    private FloatBuffer b;
    private FloatBuffer c;

    // addresses to memory on the GPU
    private long aVector;
    private long bVector;
    private long cVector;

    // size
    private int n;

    public CLVectorSum(final int size) {
        this.n = size;
    }

    private void initCallbacks() {
        contextCB = CLContextCallback.create((errinfo, private_info, cb, user_data) ->
        {
            log.warn("[LWJGL] cl_context_callback");
            log.warn("\tInfo: " + memUTF8(errinfo));
        });

        programCB = CLProgramCallback.create((program, user_data) ->
        {
	        try {
		        log.info("The cl_program [0x"+program+"] was built " + (CLInfo.getProgramBuildInfoInt(program, clDevice, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"));
	        } catch (OpenCLException e) {
		        e.printStackTrace();
	        }
        });
    }

    private void initCL() throws OpenCLException {
        // helper for the memory allocation in java
        stack = MemoryStack.stackPush();
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
    }

    private void buildProgram() throws OpenCLException {
        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        PointerBuffer lengths = BufferUtils.createPointerBuffer(1);

        ByteBuffer source;
        try {
            source = CLUtils.ioResourceToByteBuffer("demo/VectorSum.cl", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        strings.put(0, source);
        lengths.put(0, source.remaining());

        clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);

        int errcode = clBuildProgram(clProgram, clDevice, "", programCB, NULL);
        CLInfo.checkCLError(errcode);

        clKernel = clCreateKernel(clProgram, "add", errcode_ret);
    }

    private void createMemory() {
        c = stack.mallocFloat(n*2);
        a = stack.mallocFloat(n*2);
        b = stack.mallocFloat(n*2);
        Random random = new Random();

        for(int i = 0; i < n*2; i++) {
            a.put(i, random.nextFloat());
            b.put(i, random.nextFloat());
        }

        aVector = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, a, errcode_ret);
        bVector = clCreateBuffer(clContext, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, b, errcode_ret);
        cVector = clCreateBuffer(clContext, CL_MEM_READ_ONLY, 4*n*2, errcode_ret);
    }

    private void runKernel() {
        clSetKernelArg1p(clKernel, 0, aVector);
        clSetKernelArg1p(clKernel, 1, bVector);
        clSetKernelArg1p(clKernel, 2, cVector);

        PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
        kernel1DGlobalWorkSize.put(0, n);

        clEnqueueNDRangeKernel(clQueue, clKernel, 1, null, kernel1DGlobalWorkSize, null, null, null);

        clFinish(clQueue);
        clEnqueueReadBuffer(clQueue, cVector, true, 0, c, null, null);
    }

    private void printResult() {
        log.info("print CPU result");
        for(int i = 0; i < n*2; i++) {
            log.info(a.get(i) * b.get(i));
        }

        log.info("print GPU result");
        for(int i = 0; i < n*2; i++) {
            log.info(c.get(i));
        }
    }

    private void clearCL() {
        contextCB.free();
        programCB.free();
        clReleaseMemObject(aVector);
        clReleaseMemObject(bVector);
        clReleaseMemObject(cVector);
        clReleaseKernel(clKernel);
        clReleaseCommandQueue(clQueue);
        clReleaseProgram(clProgram);
        clReleaseContext(clContext);
    }

    /*
     *
     * Assumption: There is only one Platform with a GPU.
     */
    public static void main(String... args) throws OpenCLException {
        CLVectorSum sum = new CLVectorSum(4);
        sum.initCallbacks();
        sum.initCL();
        sum.buildProgram();
        sum.createMemory();
        sum.runKernel();
        sum.printResult();
        sum.clearCL();
    }

    private static void printPlatformInfo(long platform, String param_name, int param) throws OpenCLException {
        System.out.println("\t" + param_name + " = " + CLInfo.getPlatformInfoStringUTF8(platform, param));
    }

    private static void printDeviceInfo(long device, String param_name, int param) throws OpenCLException {
        System.out.println("\t" + param_name + " = " + CLInfo.getDeviceInfoStringUTF8(device, param));
    }


}
