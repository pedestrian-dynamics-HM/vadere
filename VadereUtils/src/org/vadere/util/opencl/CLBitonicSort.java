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
 * @author Benedikt Zoennchen
 */
public class CLBitonicSort {
    private static Logger log = LogManager.getLogger(CLBitonicSort.class);

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

    private ByteBuffer source;

    // CL callbacks
    private CLContextCallback contextCB;
    private CLProgramCallback programCB;

    // CL kernel
    private long clBitonicSortLocal;
	private long clBitonicSortLocal1;
	private long clBitonicMergeGlobal;
    private long clBitonicMergeLocal;
    private long clKernel;

    private int[] keys;
    private int[] values;

    private int[] resultValues;
    private int[] resultKeys;

	//Note: logically shared with BitonicSort.cl!
    private static final int LOCAL_SIZE_LIMIT = 16;

    private boolean debug = false;

    public enum KernelType {
        Separate,
        Col,
        Row,
        NonSeparate
    }

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

	public void init() throws OpenCLException {
        initCallbacks();
        initCL();
        buildProgram();
    }

    public void sort(@NotNull final int[] keys, @NotNull final int[] values) throws OpenCLException {
    	assert factorRadix2(keys.length) == 1 && keys.length == values.length;

	    inKeys = CLUtils.toIntBuffer(keys, CLUtils.toIntBuffer(keys));
	    outKeys = CLUtils.toIntBuffer(keys);
	    inValues = CLUtils.toIntBuffer(values, CLUtils.toIntBuffer(values));
	    outValues = CLUtils.toIntBuffer(values);

	    try (MemoryStack stack = stackPush()) {

	    	int dir = 1;

		    PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
		    PointerBuffer clLocalWorkSize = stack.callocPointer(1);
		    IntBuffer errcode_ret = stack.callocInt(1);
		    // host memory to gpu memory
		    clInKeys = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, inKeys, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clOutKeys = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * keys.length, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clInValues = clCreateBuffer(clContext, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR, inValues, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);
		    clOutValues = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * keys.length, errcode_ret);
		    CLInfo.checkCLError(errcode_ret);


	    	// small sorts
	    	if(keys.length <= LOCAL_SIZE_LIMIT)
		    {
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 0, clOutKeys));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 1, clOutValues));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 2, clInKeys));
			    CLInfo.checkCLError(clSetKernelArg1p(clBitonicSortLocal, 3, clInValues));
			    CLInfo.checkCLError(clSetKernelArg1i(clBitonicSortLocal, 4, keys.length));
			    CLInfo.checkCLError(clSetKernelArg1i(clBitonicSortLocal, 5, 1));
			    clGlobalWorkSize.put(0, keys.length / 2);
			    clLocalWorkSize.put(0, keys.length / 2);

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

			    clGlobalWorkSize = stack.callocPointer(1);
			    clLocalWorkSize = stack.callocPointer(1);
			    clGlobalWorkSize.put(0, keys.length / 2);
			    clLocalWorkSize.put(0, LOCAL_SIZE_LIMIT / 2);

			    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicSortLocal1, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
			    CLInfo.checkCLError(clFinish(clQueue));

			    for(int size = 2 * LOCAL_SIZE_LIMIT; size <= keys.length; size <<= 1)
			    {
				    for(int stride = size / 2; stride > 0; stride >>= 1)
				    {
					    if(stride >= LOCAL_SIZE_LIMIT)
					    {
						    //Launch bitonicMergeGlobal
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 0, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 1, clOutValues));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 2, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeGlobal, 3, clOutValues));

						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 4, keys.length));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 5, size));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 6, stride));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeGlobal, 7, dir));

						    clGlobalWorkSize = stack.callocPointer(1);
						    clLocalWorkSize = stack.callocPointer(1);
						    clGlobalWorkSize.put(0, keys.length / 2);
						    clLocalWorkSize.put(0, LOCAL_SIZE_LIMIT / 4);

						    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicMergeGlobal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
						    CLInfo.checkCLError(clFinish(clQueue));
					    }
					    else
					    {
						    //Launch bitonicMergeLocal
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 0, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 1, clOutValues));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 2, clOutKeys));
						    CLInfo.checkCLError(clSetKernelArg1p(clBitonicMergeLocal, 3, clOutValues));

						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 4, keys.length));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 5, stride));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 6, size));
						    CLInfo.checkCLError(clSetKernelArg1i(clBitonicMergeLocal, 7, dir));

						    clGlobalWorkSize = stack.callocPointer(1);
						    clLocalWorkSize = stack.callocPointer(1);
						    clGlobalWorkSize.put(0, keys.length / 2);
						    clLocalWorkSize.put(0, LOCAL_SIZE_LIMIT / 2);

						    CLInfo.checkCLError(clEnqueueNDRangeKernel(clQueue, clBitonicMergeLocal, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));
						    CLInfo.checkCLError(clFinish(clQueue));
						    break;
					    }
				    }
			    }
		    }
		    clEnqueueReadBuffer(clQueue, clOutKeys, true, 0, outKeys, null, null);
		    clEnqueueReadBuffer(clQueue, clOutValues, true, 0, outValues, null, null);
		    resultKeys = CLUtils.toIntArray(outKeys, keys.length);
		    resultValues = CLUtils.toIntArray(outValues, values.length);
	    }

	    clearCL();
    }

	static long factorRadix2(long L){
		if(L==0){
			return 0;
		}else{
			for(int log2L = 0; (L & 1) == 0; L >>= 1, log2L++);
			return L;
		}
	}

	public void clear() throws OpenCLException {
		clearMemory();
	}

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
		    MemoryUtil.memFree(inKeys);
		    MemoryUtil.memFree(outKeys);
		    MemoryUtil.memFree(inValues);
		    MemoryUtil.memFree(inKeys);
		    MemoryUtil.memFree(source);
	    }
    }

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

	    }

    }
}