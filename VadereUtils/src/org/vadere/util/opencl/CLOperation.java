package org.vadere.util.opencl;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLProgramCallback;
import org.lwjgl.system.MemoryStack;
import org.vadere.util.logging.Logger;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Optional;

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_DEFAULT;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_END;
import static org.lwjgl.opencl.CL10.CL_PROFILING_COMMAND_START;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_STATUS;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clGetEventProfilingInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseProgram;
import static org.lwjgl.opencl.CL10.clWaitForEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

/**
 * This class implements operation to set up and to destroy an OpenCL context which
 * every OpenCL class requires.
 */
public abstract class CLOperation {
	private static Logger log = Logger.getLogger(CLOperation.class);

	protected long clPlatform;
	protected long clDevice;
	protected long clContext;
	protected long clQueue;
	protected boolean profiling;
	protected int deviceType;
	protected long clProgram;

	protected CLContextCallback contextCB;
	protected CLProgramCallback programCB;

	protected CLOperation() {
		this(CL_DEVICE_TYPE_ALL);
	}

	protected CLOperation(final int deviceType) {
		profiling = false;
		this.deviceType = deviceType;
	}

	protected void initCL() throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			IntBuffer numberOfPlatforms = stack.mallocInt(1);

			CLInfo.checkCLError(clGetPlatformIDs(null, numberOfPlatforms));

			// if all, find the possibly best
			Optional<Pair<Long, Long>> platformAndDevice;
			if(deviceType == CL_DEVICE_TYPE_ALL) {
				platformAndDevice = CLUtils.getFirstSupportedPlatformAndDevice(CL_DEVICE_TYPE_DEFAULT);
				if(!platformAndDevice.isPresent()) {
					platformAndDevice = CLUtils.getFirstSupportedPlatformAndDevice(CL_DEVICE_TYPE_ALL);
				}
			}
			else {
				platformAndDevice = CLUtils.getFirstSupportedPlatformAndDevice(deviceType);
			}


			if(!platformAndDevice.isPresent()) {
				log.debug("No support for OpenCl found.");
				throw new UnsupportedOpenCLException("No support for OpenCl found.");
			}

			clPlatform = platformAndDevice.get().getLeft();
			clDevice = platformAndDevice.get().getRight();

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

	protected long enqueueNDRangeKernel(final String name, long command_queue, long kernel, int work_dim, PointerBuffer global_work_offset, PointerBuffer global_work_size, PointerBuffer local_work_size, PointerBuffer event_wait_list, PointerBuffer event) throws OpenCLException {
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
				log.info(name + " event time " + "0x"+eventAddr + ": " + ((double)endTime.get() - startTime.get()) / 1_000_000.0 + " [ms]");
				endTime.clear();
				startTime.clear();
				return result;
			}
		}
		else {
			return clEnqueueNDRangeKernel(command_queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, event_wait_list, event);
		}
	}

	protected void clearCL() throws OpenCLException {
		CLInfo.checkCLError(clReleaseCommandQueue(clQueue));
		CLInfo.checkCLError(clReleaseProgram(clProgram));
		CLInfo.checkCLError(clReleaseContext(clContext));
		contextCB.free();
		programCB.free();
	}

	/**
	 * Returns a long number n which is greater or equals <tt>value</tt> such that n = k * <tt>multiple</tt>
	 * for some natural number k.
	 *
	 * @param value     the value
	 * @param multiple  the multiple
	 *
	 * @return a long number n such that n = k * <tt>multiple</tt>
	 */
	protected static long multipleOf(long value, long multiple) {
		long result = multiple;
		while (result < value) {
			result += multiple;
		}
		return result;
	}
}
