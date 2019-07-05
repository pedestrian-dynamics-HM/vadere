package org.vadere.util.opencl;


import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_MEM_ALLOC_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_COPY_HOST_PTR;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
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
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetDeviceInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseKernel;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * @author Benedikt Zoennchen
 *
 * This class implements the bitonic sort using the GPU via OpenCL.
 */
public class CLBitonicSort {
    private static Logger log = Logger.getLogger(CLBitonicSort.class);

    // CL ids
    private long clPlatform;
    private long clDevice;
    private long clContext;
    private long clQueue;
    private long clProgram;

    // CL Memory
    private long clInKeys;
    private long clOutKeys;
    private long clInValues;
    private long clOutValues;

    // Host Memory
    private IntBuffer inKeys;
    private IntBuffer outKeys;
	private IntBuffer inValues;
	private IntBuffer outValues;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;

    // CL kernel
    private long clBitonicSortLocal;
	private long clBitonicSortLocal1;
	private long clBitonicMergeGlobal;
    private long clBitonicMergeLocal;
    private long clKernel;

    private int[] resultValues;
    private int[] resultKeys;

	//Note: logically shared with BitonicSort.cl!
    private int max_work_group_size = 16;

    private boolean debug = false;

    public CLBitonicSort() throws OpenCLException {
	    if(debug) {
		    Configuration.DEBUG.set(true);
		    Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		    Configuration.DEBUG_STACK.set(true);
	    }
	    init();
    }

	public int[] getResultKeys() {
		return resultKeys;
	}

	public int[] getResultValues() {
		return resultValues;
	}

	/**
	 * Builds all OpenCL resources. This does not initialize or reserve any device or host memory.
	 *
	 * @throws OpenCLException
	 */
	private void init() throws OpenCLException {
        initCallbacks();
        initCL();
        buildProgram();
    }

    public void sort(@NotNull final int[] keys, @NotNull final int[] values) throws OpenCLException {
    	assert keys.length == values.length;

    	final int[] padKeys;
    	final int[] padValues;
    	boolean padding = CLUtils.factorRadix2(keys.length) != 1;

    	// padding is required!
    	if(padding) {
			int k = (int)CLUtils.power(keys.length, 2);
			assert k > keys.length && CLUtils.factorRadix2(k) == 1;
			padKeys = new int[k];
			padValues = new int[k];
			System.arraycopy(keys, 0, padKeys, 0, keys.length);
		    System.arraycopy(values, 0, padValues, 0, values.length);

		    for(int i = keys.length; i < padKeys.length; i++) {
		    	padKeys[i] = Integer.MAX_VALUE;
		    }

	    } else {
			padKeys = keys;
			padValues = values;
	    }

	    /**
	     * We use non-stack memory because the stack might be too small.
	     */
	    inKeys = CLUtils.toIntBuffer(padKeys);
	    outKeys = MemoryUtil.memAllocInt(padKeys.length);
	    inValues = CLUtils.toIntBuffer(padValues);
	    outValues = MemoryUtil.memAllocInt(padValues.length);

	    try (MemoryStack stack = stackPush()) {
	    	int dir = 1;

		    PointerBuffer clGlobalWorkSize = stack.mallocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.mallocPointer(1);
		    IntBuffer errcode_ret = stack.callocInt(1);
		    // host memory to gpu memory
		    clInKeys = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, inKeys, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clOutKeys = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * padKeys.length, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clInValues = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, inValues, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clOutValues = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * padKeys.length, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);

		    long ms = System.currentTimeMillis();
	    	// small sorts
	    	if(padKeys.length <= max_work_group_size)
		    {
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 0, clOutKeys));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 1, clOutValues));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 2, clInKeys));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 3, clInValues));
			    CLInfo.checkCLError(clSetKernelArg1i(clBitonicSortLocal, 4, padKeys.length));
			    CLInfo.checkCLError(clSetKernelArg1i(clBitonicSortLocal, 5, 1));
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal, 6, padKeys.length * 4)); // local memory
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal, 7, padKeys.length * 4)); // local memory
			    clGlobalWorkSize.put(0, padKeys.length / 2);
			    clLocalWorkSize.put(0, padKeys.length / 2);

			    // run the kernel and read the result
			    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicSortLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
			    CLInfo.checkCLError(clFinish(clQueue));
		    }
		    else {
			    //Launch bitonicSortLocal1
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 0, clOutKeys));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 1, clOutValues));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 2, clInKeys));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal1, 3, clInValues));
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal1, 4, max_work_group_size * 4)); // local memory
			    CLInfo.checkCLError(clSetKernelArg(clBitonicSortLocal1, 5, max_work_group_size * 4)); // local memory

			    clGlobalWorkSize = stack.mallocPointer(1);
			    clLocalWorkSize = stack.mallocPointer(1);
			    clGlobalWorkSize.put(0, padKeys.length / 2);
			    clLocalWorkSize.put(0, max_work_group_size / 2);

			    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicSortLocal1, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
			    CLInfo.checkCLError(clFinish(clQueue));

			    for(int size = 2 * max_work_group_size; size <= padKeys.length; size <<= 1)
			    {
				    for(int stride = size / 2; stride > 0; stride >>= 1)
				    {
					    if(stride >= max_work_group_size)
					    {
						    //Launch bitonicMergeGlobal
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 0, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 1, clOutValues));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 2, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 3, clOutValues));

						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 4, padKeys.length));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 5, size));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 6, stride));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 7, dir));

						    clGlobalWorkSize = stack.mallocPointer(1);
						    clLocalWorkSize = stack.mallocPointer(1);
						    clGlobalWorkSize.put(0, padKeys.length / 2);
						    clLocalWorkSize.put(0, max_work_group_size / 4);

						    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicMergeGlobal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
						    //CLInfo.checkCLError(clFinish(clQueue));
					    }
					    else
					    {
						    //Launch bitonicMergeLocal
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 0, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 1, clOutValues));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 2, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 3, clOutValues));

						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 4, padKeys.length));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 5, stride));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 6, size));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 7, dir));
						    CLInfo.checkCLError(clSetKernelArg(clBitonicMergeLocal, 8, max_work_group_size * 4 )); // local memory
						    CLInfo.checkCLError(clSetKernelArg(clBitonicMergeLocal, 9, max_work_group_size * 4)); // local memory

						    clGlobalWorkSize = stack.mallocPointer(1);
						    clLocalWorkSize = stack.mallocPointer(1);
						    clGlobalWorkSize.put(0, padKeys.length / 2);
						    clLocalWorkSize.put(0, max_work_group_size / 2);

						    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicMergeLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
						    //CLInfo.checkCLError(clFinish(clQueue));
						    break;
					    }
				    }
			    }
		    }
		    //System.out.println(System.currentTimeMillis() - ms);

		    clEnqueueReadBuffer(clQueue, clOutKeys, true, 0, outKeys, null, null);
		    clEnqueueReadBuffer(clQueue, clOutValues, true, 0, outValues, null, null);
		    resultKeys = CLUtils.toIntArray(outKeys, keys.length);
		    resultValues = CLUtils.toIntArray(outValues, values.length);
	    }

	    clearMemory();
    }

	public void clear() throws OpenCLException {
		clearCL();
		//clearMemory();
	}

	/**
	 * Clears the device and host memory.
	 *
	 * @throws OpenCLException
	 */
    private void clearMemory() throws OpenCLException {
        // release memory and devices
	    try {
		    CLInfo.checkCLError(clReleaseMemObject(clInKeys));
		    CLInfo.checkCLError(clReleaseMemObject(clOutKeys));
		    CLInfo.checkCLError(clReleaseMemObject(clInValues));
		    CLInfo.checkCLError(clReleaseMemObject(clOutValues));
	    }
	    catch (OpenCLException ex) {
			throw ex;
	    }
		finally {
	    	MemoryUtil.memFree(inValues);
	    	MemoryUtil.memFree(inKeys);
	    	MemoryUtil.memFree(outValues);
	    	MemoryUtil.memFree(outKeys);
	    	// release host memory.
	    }
    }

	/**
	 * Clears the OpenCL resources i.e. kernels, queues and programs.
	 *
	 * @throws OpenCLException
	 */
	private void clearCL() throws OpenCLException {
	    CLInfo.checkCLError(clReleaseKernel(clBitonicSortLocal));
	    CLInfo.checkCLError(clReleaseKernel(clBitonicSortLocal1));
	    CLInfo.checkCLError(clReleaseKernel(clBitonicMergeGlobal));
	    CLInfo.checkCLError(clReleaseKernel(clBitonicMergeLocal));

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
            IntBuffer errcode_ret = stack.mallocInt(1);
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
		    IntBuffer errcode_ret = stack.mallocInt(1);

		    PointerBuffer strings = stack.mallocPointer(1);
		    PointerBuffer lengths = stack.mallocPointer(1);
		    ByteBuffer source = null;

		    try {
			    source = CLUtils.ioResourceToByteBuffer("BitonicSort.cl", 4096);
		    } catch (IOException e) {
			    throw new OpenCLException(e.getMessage());
		    }


		    strings.put(0, source);
		    lengths.put(0, source.remaining());

		    clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
		    CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));
		    clBitonicSortLocal = clCreateKernel(clProgram, "bitonicSortLocal", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clBitonicSortLocal1 = clCreateKernel(clProgram, "bitonicSortLocal1", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clBitonicMergeGlobal = clCreateKernel(clProgram, "bitonicMergeGlobal", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clBitonicMergeLocal = clCreateKernel(clProgram, "bitonicMergeLocal", errcode_ret);
		    CLInfo.checkCLError(errcode_ret);

		    PointerBuffer pp = stack.mallocPointer(1);
		    clGetDeviceInfo(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE, pp, null);
		    max_work_group_size = (int)pp.get(0);
		    //System.out.println(max_work_group_size);
		    MemoryUtil.memFree(source);
	    }

    }
}
