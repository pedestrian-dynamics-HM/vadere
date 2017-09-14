package org.vadere.util.opencl.examples;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Created by bzoennchen on 08.09.17.
 */
public class CLUtils {

	public static Logger log = LogManager.getLogger(CLUtils.class);

	public static ArrayList<Long> getCLPlatforms() {

		MemoryStack stack = stackPush();

		IntBuffer pi = stack.mallocInt(1);
		InfoUtils.checkCLError(clGetPlatformIDs(null, pi));

		if (pi.get(0) == 0) {
			log.warn("No OpenCL platform found.");
			return new ArrayList<>();
		}

		PointerBuffer platformIDs = stack.mallocPointer(pi.get(0));

		InfoUtils.checkCLError(clGetPlatformIDs(platformIDs, (IntBuffer)null));
		ArrayList<Long> platforms = new ArrayList<>(platformIDs.capacity());


		for (int i = 0; i < platformIDs.capacity(); i++) {
			long platform = platformIDs.get(i);
			CLCapabilities caps = CL.createPlatformCapabilities(platform);
			if (caps.cl_khr_gl_sharing || caps.cl_APPLE_gl_sharing) {
				platforms.add(platform);
			}
		}

		return platforms;
	}

}
