package org.vadere.util.opencl;

import org.apache.log4j.Logger;
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


public class CLFFT2Dim {

    private static final int OFFSET_ZERO = 0;
    private static final int SOURCE_BUFFER_SIZE = 4096;
    private static final int INPUT_BUFFER_SIZE_FACTOR = 4;
    private static final int OUTPUT_BUFFER_SIZE_FACTOR = 4;
    private static final int MALLOC_SIZE = 1;
    private static final int DIMENSION_ZERO = 0;
    private static final int WORK_DIM = 1;
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
    //private IntBuffer hostOutput;
    private FloatBuffer hostOutput;
    private ByteBuffer kernelSourceCode;

    private int length;
    private int size;
    private int direction;

    // Kernel Memory
    private long clInput;
    private long clOutput;
    private long clKernel;


    // logger for callbacks
    private static Logger log = Logger.getLogger(CLFFT2Dim.class);
    private boolean debug = false;
    private boolean profiling = false;

    public enum TransformationType {
        SPACE2FREQUENCY, FREQUENCY2SPACE
    }

    public CLFFT2Dim(int length, int size, TransformationType type) throws OpenCLException {
        this.length = length;
        this.size = size;
        this.direction = type == TransformationType.SPACE2FREQUENCY ? 1 : -1;
        // enables debug memory operations
        if (debug) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        init();

        // max FFT on one workitem
        long local_mem_size = CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
        long max_work_group_size = CLInfo.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
        this.MAX_FFT_PER_WORK_ITEM = (int) (local_mem_size / (max_work_group_size * 8));
    }

    public void init() throws OpenCLException {
        try {
            initCallbacks();
            initCL();
            buildProgram();

            hostInput = MemoryUtil.memAllocFloat(length);
            hostOutput = MemoryUtil.memAllocFloat(length);
            setArguments();
        } catch (OpenCLException ex) {
            throw ex;
        }
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

            clInput = clCreateBuffer(clContext, CL_MEM_READ_WRITE, INPUT_BUFFER_SIZE_FACTOR * length, errcode_ret);
            clOutput = clCreateBuffer(clContext, CL_MEM_WRITE_ONLY, OUTPUT_BUFFER_SIZE_FACTOR * length, errcode_ret);
            CLInfo.checkCLError(clSetKernelArg1p(clKernel, 0, clInput)); // input
            CLInfo.checkCLError(clSetKernelArg(clKernel, 1, size*size)); // size*size=length/2
            CLInfo.checkCLError(clSetKernelArg1p(clKernel, 2, clOutput));
            CLInfo.checkCLError(clSetKernelArg1i(clKernel, 3, size)); // N
            CLInfo.checkCLError(clSetKernelArg1i(clKernel, 4, length/2)); // length = N*2*N
            CLInfo.checkCLError(clSetKernelArg1i(clKernel, 5, direction));
        }
    }

    public float[] fft2Dim(final float[] input) throws OpenCLException {
        CLUtils.toFloatBuffer(input,hostInput);
        clEnqueueWriteBuffer(clQueue,clInput,true,OFFSET_ZERO,hostInput,null,null);
        fft2Dim(clKernel);
        clEnqueueReadBuffer(clQueue,clOutput,true,OFFSET_ZERO,hostOutput,null,null);

        return CLUtils.toFloatArray(hostOutput,length);
    }

    private void fft2Dim(final long clKernel) throws OpenCLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer clGlobalWorkSizeEdges = stack.callocPointer(WORK_DIM);

            int items_per_workgroup = length/2;
            clGlobalWorkSizeEdges.put(DIMENSION_ZERO,items_per_workgroup);

            PointerBuffer clLocalWorkSizeEdges = stack.callocPointer(WORK_DIM); // local_work_size
            int local_size = computeLocalSize(items_per_workgroup);
            clLocalWorkSizeEdges.put(DIMENSION_ZERO, local_size);
            System.out.println("local_size " + local_size);

            CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clKernel, WORK_DIM, null, clGlobalWorkSizeEdges, clLocalWorkSizeEdges, null, null));
            CLInfo.checkCLError(clFinish(clQueue));
        }
    }

    private int computeLocalSize(int items_per_workgroup) {
        int points_per_item = MAX_FFT_PER_WORK_ITEM;
        int local_size = items_per_workgroup / points_per_item;

        while (! (points_per_item >= MIN_ELEMENTS_PER_WORKITEM && items_per_workgroup > points_per_item)) {
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
                kernelSourceCode = CLUtils.ioResourceToByteBuffer("FFT2Dim.cl", SOURCE_BUFFER_SIZE); // TODO only use bufferSize that is needed
            } catch (IOException e) {
                throw new OpenCLException(e.getMessage());
            }

            strings.put(0, kernelSourceCode);
            lengths.put(0, kernelSourceCode.remaining()); // TODO

            clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
            CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));
            System.out.println(CLInfo.getProgramBuildInfoStringASCII(clProgram, clDevice, CL_PROGRAM_BUILD_LOG));
            clKernel = clCreateKernel(clProgram, "fft2Dim", errcode_ret);
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

    public void clearCL() throws OpenCLException {
        clearMemory();
        CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
        CLInfo.checkCLError(clReleaseProgram(clProgram));
        CLInfo.checkCLError(clReleaseContext(clContext));
        contextCB.free();
        programCB.free();
    }




}
