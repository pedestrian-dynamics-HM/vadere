package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLInfo;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;
import org.vadere.util.opencl.examples.InfoUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_LOCAL_MEM_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_END;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_START;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_STATUS;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clBuildProgram;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clCreateProgramWithSource;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetEventProfilingInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clWaitForEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public abstract class CLAbstract {

	private static Logger log = Logger.getLogger(CLAbstract.class);

	static {
		log.setDebug();
	}

	// CL callbacks
	private CLContextCallback contextCB;
	private CLProgramCallback programCB;

	// CL ids
	protected long clPlatform;
	protected long clDevice;
	protected long clContext;
	protected long clQueue;
	protected long clProgram;

	private boolean profiling;
	private boolean debug;
	private long maxWorkGroupSize;
	private long maxLocalMemorySize;

	public CLAbstract() {
		profiling = false;
		debug = false;

		if(debug) {
			Configuration.DEBUG.set(true);
			Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
			Configuration.DEBUG_STACK.set(true);
		}
	}

	protected void init(@NotNull final String fileName) throws OpenCLException {
		initCallbacks();
		initCL();
		buildProgram(fileName);
	}

	protected void setDebug(final boolean debug) {
		this.debug = debug;
	}

	protected void setProfiling(final boolean profiling) {
		this.profiling = profiling;
	}

	// private helpers
	protected void initCallbacks() {
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

	public long getMaxLocalMemorySize() {
		return maxLocalMemorySize;
	}

	public long getMaxWorkGroupSize() {
		return maxWorkGroupSize;
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

			if(profiling) {
				clQueue = clCreateCommandQueue(clContext, clDevice, CL_QUEUE_PROFILING_ENABLE, errcode_ret);
			}
			else {
				clQueue = clCreateCommandQueue(clContext, clDevice, 0, errcode_ret);
			}

			CLInfo.checkCLError(errcode_ret);
		}
	}

	protected long enqueueNDRangeKernel(
			final String name,
			long command_queue,
			long kernel,
			int work_dim,
			PointerBuffer global_work_offset,
			PointerBuffer global_work_size,
			PointerBuffer local_work_size,
			PointerBuffer event_wait_list,
			PointerBuffer event,
			long[] time,
			boolean print) throws OpenCLException {
		if(profiling) {
			try (MemoryStack stack = stackPush()) {
				PointerBuffer clEvent = stack.mallocPointer(1);
				LongBuffer startTime = stack.mallocLong(1);
				LongBuffer endTime = stack.mallocLong(1);
				long result = clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, clEvent);
				clWaitForEvents(clEvent);
				long eventAddr = clEvent.get();
				CLInfo.checkCLError(clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_START, startTime, null));
				CLInfo.checkCLError(clGetEventProfilingInfo(eventAddr, CL_PROFILING_COMMAND_END, endTime, null));
				clEvent.clear();
				// in nanaSec
				long executionTime = endTime.get() - startTime.get();
				time[0] += executionTime;
				if(print) {
					log.info(name + " event time " + "0x"+eventAddr + ": " + toMillis(executionTime) + " [ms]");
				}
				endTime.clear();
				startTime.clear();
				return result;
			}
		}
		else {
			return clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, event);
		}
	}

	protected double toMillis(long nanos) {
		return nanos / 1_000_000.0;
	}

	protected long enqueueNDRangeKernel(
			final String name,
			long command_queue,
			long kernel,
			int work_dim,
			PointerBuffer global_work_offset,
			PointerBuffer global_work_size,
			PointerBuffer local_work_size,
			PointerBuffer event_wait_list,
			PointerBuffer event) throws OpenCLException {
		return enqueueNDRangeKernel(name, command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, event,
				new long[1], true);
	}

	private void buildProgram(@NotNull final String fileName) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);

			PointerBuffer strings = stack.mallocPointer(1);
			PointerBuffer lengths = stack.mallocPointer(1);

			ByteBuffer source;
			try {
				source = CLUtils.ioResourceToByteBuffer(fileName, 4096);
			} catch (IOException e) {
				throw new OpenCLException(e.getMessage());
			}

			strings.put(0, source);
			lengths.put(0, source.remaining());
			clProgram = clCreateProgramWithSource(clContext, strings, lengths, errcode_ret);
			log.debug(InfoUtils.getProgramBuildInfoStringASCII(clProgram, clDevice, CL_PROGRAM_BUILD_LOG));

			CLInfo.checkCLError(clBuildProgram(clProgram, clDevice, "", programCB, NULL));

			buildKernels();

			maxWorkGroupSize = InfoUtils.getDeviceInfoPointer(clDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
			log.debug("CL_DEVICE_MAX_WORK_GROUP_SIZE = " + maxWorkGroupSize);

			maxLocalMemorySize = InfoUtils.getDeviceInfoLong(clDevice, CL_DEVICE_LOCAL_MEM_SIZE);
			log.debug("CL_DEVICE_LOCAL_MEM_SIZE = " + maxLocalMemorySize);

			MemoryUtil.memFree(source);
		}
	}

	protected void freeCLMemory(long address)  throws OpenCLException {
		try {
			CLInfo.checkCLError(clReleaseMemObject(address));
		} catch (OpenCLException ex) {
			throw ex;
		}
	}

	protected void clearCL() throws OpenCLException {
		releaseKernels();
		CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
		CLInfo.checkCLError(clReleaseProgram(clProgram));
		CLInfo.checkCLError(clReleaseContext(clContext));
		contextCB.free();
		programCB.free();
	}

	public void clear() throws OpenCLException {
		clearMemory();
		clearCL();
	}

	protected abstract void clearMemory() throws OpenCLException;

	protected abstract void buildKernels() throws OpenCLException;

	protected abstract void releaseKernels() throws OpenCLException;
}
