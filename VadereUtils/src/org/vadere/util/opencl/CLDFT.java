package org.vadere.util.opencl;


import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.memUTF8;


public class CLDFT {

    private static final int OFFSET_ZERO = 0;
    private static final int SOURCE_BUFFER_SIZE = 4096; // TODO
    private static final int INPUT_BUFFER_SIZE_FACTOR = 4; // TODO
    private static final int OUTPUT_BUFFER_SIZE_FACTOR = 4; // TODO
    private static final int MALLOC_SIZE = 1;
    private static final int DIMENSION_ZERO = 0;
    private static final int DIMENSION_ONE = 1;
    private static final int WORK_DIM = 2;
    private static final int FORWARDS = 1;
    private static final int BACKWARDS = -1;
    private static int MAX_FFT_PER_WORK_ITEM;
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
    private FloatBuffer hostInput;
    private FloatBuffer hostOutput;
    private ByteBuffer kernelSourceCode;

    private int matrixWidth;
    private int matrixHeight;
    private int direction;

    // Kernel Memory
    private long clInput;
    private long clOutput;
    private long clKernel;

    public enum TransformationType {
        SPACE2FREQUENCY, FREQUENCY2SPACE
    }

    // logger for callbacks
    private static Logger log = log = Logger.getLogger(CLDFT.class);
    private boolean debug = false;

    public CLDFT(int matrixWidth, int matrixHeight, TransformationType type) throws OpenCLException {
        this.matrixWidth = matrixWidth;
        this.matrixHeight = matrixHeight;
        this.direction = type == TransformationType.SPACE2FREQUENCY ? FORWARDS : BACKWARDS;
        // enables debug memory operations
        if (debug) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        init();

        // max points_per_workitem
        long local_mem_size = CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
        long max_work_group_size = CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
        this.MAX_FFT_PER_WORK_ITEM = (int) (local_mem_size / (max_work_group_size * 8));
    }

    public void init() throws OpenCLException {
        try {
            initCallbacks();
            initCL();
            buildProgram();

            hostInput = MemoryUtil.memAllocFloat(matrixWidth * matrixHeight);
            hostOutput = MemoryUtil.memAllocFloat(matrixWidth * matrixHeight);

            setArguments();

        } catch (OpenCLException ex) {
            throw ex;
        }
    }

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

    private void setArguments() throws OpenCLException {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            clInput = clCreateBuffer(clContext, CL_MEM_READ_ONLY, INPUT_BUFFER_SIZE_FACTOR * matrixWidth * matrixHeight, errcode_ret);
            clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, OUTPUT_BUFFER_SIZE_FACTOR * matrixWidth * matrixHeight, errcode_ret);

            CLInfo.checkCLError(clSetKernelArg1p(clKernel, 0, clInput));
            CLInfo.checkCLError(clSetKernelArg1p(clKernel, 1, clOutput));
            CLInfo.checkCLError(clSetKernelArg1i(clKernel, 2, matrixWidth/2));
            CLInfo.checkCLError(clSetKernelArg1i(clKernel, 3, direction));
        }
    }


    public float[] dft2Dim(final float[] input) throws OpenCLException {
        // TODO
        return null;
    }

    public float[] dft1Dim(final float[] input) throws OpenCLException {

        // convert to kernel memory
        CLUtils.toFloatBuffer(input, hostInput);
        // add to Queue
        clEnqueueWriteBuffer(clQueue, clInput, true, OFFSET_ZERO, hostInput, null, null);
        // compute DFT
        dft1Dim(clKernel);
        // read output back to host memory
        clEnqueueReadBuffer(clQueue, clOutput, true, OFFSET_ZERO, hostOutput, null, null);

        // convert output to float array
        return CLUtils.toFloatArray(hostOutput, matrixWidth);
    }

    private void dft1Dim(final long clKernel) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(1);
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, matrixWidth / 2);

            int items_per_workgroup = matrixWidth / 2;
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO, items_per_workgroup); // items_per_workgroup

            //PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(WORK_DIM); // local_work_size
            //int local_size = computeLocalSize(items_per_workgroup);
            //clLocalWorkSizeEdges.put(DIMENSION_ZERO, 8);

            CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, 1, null, clGlobalWorkSizeEdges, null, null, null));
            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    private int computeLocalSize(int items_per_workgroup) {
        int points_per_item = MAX_FFT_PER_WORK_ITEM;
        int local_size = items_per_workgroup / points_per_item;

        while (!(points_per_item >= MIN_ELEMENTS_PER_WORKITEM && items_per_workgroup > points_per_item)) {
            points_per_item = points_per_item / 2;
            local_size = items_per_workgroup / points_per_item;
        }

        return local_size;
    }

    private void buildProgram() throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer errcode_ret = stack.callocInt(MALLOC_SIZE);

            PointerBuffer strings = stack.mallocPointer(MALLOC_SIZE);
            PointerBuffer lengths = stack.mallocPointer(MALLOC_SIZE);

            try {
                kernelSourceCode = CLUtils.ioResourceToByteBuffer("DFT1Dim.cl", SOURCE_BUFFER_SIZE);
            } catch (IOException e) {
                throw new OpenCLException(e.getMessage());
            }

            strings.put(0, kernelSourceCode);
            lengths.put(0, kernelSourceCode.remaining());

            clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
            CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));
            System.out.println(CLInfo.getProgramBuildInfoStringASCII(clProgram, clDevice, CL_PROGRAM_BUILD_LOG));
            clKernel = clCreateKernel(clProgram, "dft1Dim", errcode_ret);
            CLInfo.checkCLError(errcode_ret);
        }

    }

    public void clearCL() throws OpenCLException {
        clearMemory();
        contextCB.free();
        programCB.free();
        CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
        CLInfo.checkCLError(clReleaseProgram(clProgram));
        CLInfo.checkCLError(clReleaseContext(clContext));
    }

    private void clearMemory() throws OpenCLException {
        try {
            CLInfo.checkCLError(clReleaseMemObject(clInput));
            CLInfo.checkCLError(clReleaseMemObject(clOutput));

            CLInfo.checkCLError(clReleaseKernel(clKernel));
        } catch (OpenCLException e) {
            throw e;
        } finally {
            MemoryUtil.memFree(hostInput);
            MemoryUtil.memFree(hostOutput);
            MemoryUtil.memFree(kernelSourceCode);
        }
    }


}