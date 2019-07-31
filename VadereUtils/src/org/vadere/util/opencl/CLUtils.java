package org.vadere.util.opencl;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.opencl.examples.InfoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.CL_KERNEL_LOCAL_MEM_SIZE;
import static org.lwjgl.opencl.CL10.CL_KERNEL_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetKernelWorkGroupInfo;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Utility-class without a state. This class offers method to interact with OpenCL e.g. memory management methods.
 *
 * @author Benedikt Zoennchen
 */
public class CLUtils {

	private static Logger log = Logger.getLogger(CLUtils.class);

	/**
	 * Returns true if there is some platform and some device supporting OpenCL.
	 *
	 * @return true if there is some platform and device supporting OpenCL, false otherwise
	 */
	public static boolean isOpenCLSupported() {
		return isOpenCLSupported(CL_DEVICE_TYPE_ALL);
	}

	/**
	 * Returns true if there is some platform and some device of the specified device type supporting OpenCL.
	 *
	 * @return true if there is some platform and device  of the specified device type supporting OpenCL, false otherwise
	 */
	public static boolean isOpenCLSupported(final int deviceType) {
		return !getSupportedPlatforms(deviceType).isEmpty();
	}

	/**
	 * Returns a list of device addresses pointing to the OpenCL platform which support
	 * OpenCL. This list is empty if there is no OpenCL support.
	 *
	 * @param deviceType the specific device type which should be supported by the platform.
	 *                   if it is CL_DEVICE_TYPE_ALL the device can be any device.
	 * @return a list of device addresses pointing to the OpenCL platform which support OpenCL and the specified device type
	 */
	public static List<Long> getSupportedPlatforms(final int deviceType) {
		List<Long> supportedPlatforms = new ArrayList<>(2);
		try (MemoryStack stack = stackPush()) {
			IntBuffer pi = stack.mallocInt(1);
			InfoUtils.checkCLError(clGetPlatformIDs(null, pi));
			if (pi.get(0) != 0) {
				PointerBuffer platforms = stack.mallocPointer(pi.get(0));
				InfoUtils.checkCLError(clGetPlatformIDs(platforms, (IntBuffer)null));

				IntBuffer errcode_ret = stack.callocInt(1);

				for (int p = 0; p < platforms.capacity(); p++) {
					long platform = platforms.get(p);
					InfoUtils.checkCLError(clGetDeviceIDs(platform, deviceType, null, pi));

					PointerBuffer devices = stack.mallocPointer(pi.get(0));
					InfoUtils.checkCLError(clGetDeviceIDs(platform, deviceType, devices, (IntBuffer)null));
					if(devices.capacity() > 0) {
						supportedPlatforms.add(platform);
					}
				}
			}

		} catch(Throwable e) {
			log.warn("No OpenCL support: " + e.getMessage());
		}
		return supportedPlatforms;

	}

	/**
	 * Returns a pair of addresses. The left one is the platform address and the second one the device address
	 * which supports OpenCL.
	 *
	 * @param deviceType the specific device type, if it is CL_DEVICE_TYPE_ALL the device can be any device
	 *
	 * @return a pair of addresses (platform address, device address)
	 */
	public static Optional<Pair<Long, Long>> getFirstSupportedPlatformAndDevice(final int deviceType) {

		try (MemoryStack stack = stackPush()) {
			IntBuffer pi = stack.mallocInt(1);
			InfoUtils.checkCLError(clGetPlatformIDs(null, pi));
			if (pi.get(0) == 0) {
				return Optional.empty();
			}

			PointerBuffer platforms = stack.mallocPointer(pi.get(0));
			InfoUtils.checkCLError(clGetPlatformIDs(platforms, (IntBuffer)null));

			for (int p = 0; p < platforms.capacity(); p++) {
				long platform = platforms.get(p);

				if(InfoUtils.checkCLSuccess(clGetDeviceIDs(platform, deviceType, null, pi))) {
					PointerBuffer devices = stack.mallocPointer(pi.get(0));

					if(InfoUtils.checkCLSuccess(clGetDeviceIDs(platform, deviceType, devices, (IntBuffer)null))) {

						if(devices.capacity() > 0) {
							return Optional.of(Pair.of(platform, devices.get(0)));
						}
					}
				}
			}
		} catch(Throwable e) {
			log.warn("No OpenCL device found: " + e.getMessage());
		}

		return Optional.empty();
	}

    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource   the resource to read
     * @param bufferSize the initial buffer size
     * @return the resource data
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer ioResourceToByteBuffer(final String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;

        Path path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = MemoryUtil.memCalloc((int)fc.size() + 1);
                while (fc.read(buffer) != -1) {}
            }
        } else {
            try (
                    InputStream source = CLUtils.class.getClassLoader().getResourceAsStream(resource);
                    ReadableByteChannel rbc = Channels.newChannel(source)
            ) {

                buffer = MemoryUtil.memCalloc(bufferSize);

                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                    }
                }
            }
        }

        buffer.flip();
        return buffer;
    }

    public static IntBuffer toIntBuffer(@NotNull final int[] array) {
    	IntBuffer intBuffer = MemoryUtil.memAllocInt(array.length);
    	return toIntBuffer(array, intBuffer);
    }

	public static IntBuffer toIntBuffer(@NotNull final int[] array, @NotNull final IntBuffer intBuffer) {
		for(int i = 0; i < array.length; i++) {
			intBuffer.put(i, array[i]);
		}
		return intBuffer;
	}

    public static FloatBuffer toFloatBuffer(@NotNull final float[] floats) {
        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(floats.length);
        return toFloatBuffer(floats, floatBuffer);
    }

    public static FloatBuffer toFloatBuffer(@NotNull final float[] floats, @NotNull final FloatBuffer floatBuffer) {
        for(int i = 0; i < floats.length; i++) {
            floatBuffer.put(i, floats[i]);
        }
        return floatBuffer;
    }

	public static int[] toIntArray(@NotNull final IntBuffer intBuffer, final int size) {
		int[] result = new int[size];
		for(int i = 0; i < size; i++) {
			result[i] = intBuffer.get(i);
		}
		return result;
	}


	public static float[] toFloatArray(@NotNull final FloatBuffer floatBuffer, final int size) {
	    float[] result = new float[size];
	    for(int i = 0; i < size; i++) {
	        result[i] = floatBuffer.get(i);
        }
        return result;
    }

    private static ByteBuffer resizeBuffer(@NotNull final ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = MemoryUtil.memCalloc(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
	    MemoryUtil.memFree(buffer);
        return newBuffer;
    }

	/**
	 * Returns an integer n such that n = <tt>base</tt>^k, where k > 0 is the smallest integer such that
	 * n >= <tt>value</tt>.
	 *
	 * @param value the value
	 * @param base  the base
	 *
	 * @return an integer n such that n = 2 * <tt>multiple</tt>^k
	 */
	public static long power(long value, long base) {
		assert value > 0 && base > 0;

		long result = base;
		while (result < value) {
			result *= base;
		}
		return result;
	}

	/**
	 * Returns an long n such that n = <tt>base</tt> * k, where k > 0 is the smallest long such that
	 * n >= <tt>value</tt>.
	 *
	 * @param value the value
	 * @param base  the multiple
	 *
	 * @return an integer n such that n = 2 * <tt>multiple</tt>^k
	 */
	public static long multiple(long value, long base) {
		long result = base;
		while (result < value) {
			result += base;
		}
		return result;
	}

	/**
	 * Computes the the factor radix which is 1 for all long of the form 2^k.
	 *
	 * @param L
	 * @return
	 */
	public static long factorRadix2(long L){
		if(L==0){
			return 0;
		}else{
			for(int log2L = 0; (L & 1) == 0; L >>= 1, log2L++);
			return L;
		}
	}

	public static long getMaxWorkGroupSizeForKernel(long clDevice, long clKernel, long workItemMem, long max_work_group_size, long max_local_memory_size) throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			LongBuffer pp = stack.mallocLong(1);
			CLInfo.checkCLError(clGetKernelWorkGroupInfo(clKernel, clDevice, CL_KERNEL_LOCAL_MEM_SIZE , pp, null));

			/*long kernelLocalMemory = pp.get(0);
			logger.debug("CL_KERNEL_LOCAL_MEM_SIZE = (" + clKernel + ") = " + kernelLocalMemory);
			logger.debug("memory for each  = " + (workItemMem + kernelLocalMemory));

			long maxWorkGroupSizeForLocalMemory = (workItemMem + kernelLocalMemory) == 0 ? 0 : (max_local_memory_size / (workItemMem + kernelLocalMemory));*/
			long maxWorkGroupSizeForLocalMemory = workItemMem == 0 ? max_work_group_size : (max_local_memory_size / (workItemMem));
			PointerBuffer ppp = stack.mallocPointer(1);
			CLInfo.checkCLError(clGetKernelWorkGroupInfo(clKernel, clDevice, CL_KERNEL_WORK_GROUP_SIZE , ppp, null));

			long maxWorkGroupSizeForPrivateMemory = ppp.get(0);
			log.debug("CL_KERNEL_WORK_GROUP_SIZE (" + clKernel + ") = " + maxWorkGroupSizeForPrivateMemory);
			//return Math.min(max_work_group_size, Math.min(maxWorkGroupSizeForLocalMemory, maxWorkGroupSizeForPrivateMemory));
			return Math.min(max_work_group_size, Math.min(maxWorkGroupSizeForLocalMemory, maxWorkGroupSizeForPrivateMemory));
		}
	}
}
