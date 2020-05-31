package org.vadere.simulator.models.osm.opencl;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.vadere.util.opencl.CLInfo;
import org.vadere.util.opencl.CLUtils;
import org.vadere.util.opencl.OpenCLException;

import java.nio.IntBuffer;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_WRITE;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateKernel;
import static org.lwjgl.opencl.CL10.clEnqueueReadBuffer;
import static org.lwjgl.opencl.CL10.clEnqueueWriteBuffer;
import static org.lwjgl.opencl.CL10.clSetKernelArg;
import static org.lwjgl.opencl.CL10.clSetKernelArg1i;
import static org.lwjgl.opencl.CL10.clSetKernelArg1p;
import static org.lwjgl.system.MemoryStack.stackPush;

public class CLScanDirectional extends CLAbstract {

	private long cl_scan_pow2;
	private long cl_scan_pad_to_pow2;
	private long cl_scan_subarrays;
	private long cl_scan_inc_subarrays;
	private int wx = 256; // workgroup size

	private long clData;
	int m = 2 * 256;     // length of each subarray ( = wx*2 )
	private final String fileName;

	public CLScanDirectional(@NotNull final String fileName) throws OpenCLException {
		super(CL_DEVICE_TYPE_GPU);
		this.fileName = fileName;
	}

	public CLScanDirectional() throws OpenCLException {
		this("ScanDirectional.cl");
	}

	private void recursive_scan(final long clData, int n, int dir) throws OpenCLException {
		int k = (int) Math.ceil((float) n / (float) m);
		long bufsize = 4 * m;

		// everything fits into one work group
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			if (k == 1) {
					PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
					PointerBuffer clLocalWorkSize = stack.callocPointer(1);
					clGlobalWorkSize.put(0, wx);
					clLocalWorkSize.put(0, wx);
					CLInfo.checkCLError(clSetKernelArg1p(cl_scan_pad_to_pow2, 0, clData));
					CLInfo.checkCLError(clSetKernelArg(cl_scan_pad_to_pow2, 1, bufsize));
					CLInfo.checkCLError(clSetKernelArg1i(cl_scan_pad_to_pow2, 2, n));
					CLInfo.checkCLError(clSetKernelArg1i(cl_scan_pad_to_pow2, 3, dir));
					CLInfo.checkCLError((int) enqueueNDRangeKernel("cl_scan_pad_to_pow2", clQueue, cl_scan_pad_to_pow2, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));

			} // use multiple work groups
			else {
				long gx = k * wx;
				long clPartial = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * k, errcode_ret);

				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_subarrays, 0, clData));
				CLInfo.checkCLError(clSetKernelArg(cl_scan_subarrays, 1, bufsize));
				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_subarrays, 2, clPartial));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_subarrays, 3, n));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_subarrays, 4, dir));
				PointerBuffer clGlobalWorkSize = stack.callocPointer(1);
				PointerBuffer clLocalWorkSize = stack.callocPointer(1);
				clGlobalWorkSize.put(0, gx);
				clLocalWorkSize.put(0, wx);
				CLInfo.checkCLError((int) enqueueNDRangeKernel("cl_scan_subarrays", clQueue, cl_scan_subarrays, 1, null, clGlobalWorkSize, clLocalWorkSize, null, null));

				recursive_scan(clPartial, k, dir);

				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_inc_subarrays, 0, clData));
				CLInfo.checkCLError(clSetKernelArg(cl_scan_inc_subarrays, 1, bufsize));
				CLInfo.checkCLError(clSetKernelArg1p(cl_scan_inc_subarrays, 2, clPartial));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_inc_subarrays, 3, n));
				CLInfo.checkCLError(clSetKernelArg1i(cl_scan_inc_subarrays, 4, dir));
				PointerBuffer clGlobalWorkSize2 = stack.callocPointer(1);
				PointerBuffer clLocalWorkSize2 = stack.callocPointer(1);
				clGlobalWorkSize2.put(0, gx);
				clLocalWorkSize2.put(0, wx);
				CLInfo.checkCLError((int) enqueueNDRangeKernel("cl_scan_inc_subarrays", clQueue, cl_scan_inc_subarrays, 1, null, clGlobalWorkSize2, clLocalWorkSize2, null, null));
				freeCLMemory(clPartial);
			}
		}
	}

	public int[] scan(int[] data, int dir) throws OpenCLException {
		int n = data.length + 1;
		int[] result = new int[n];
		init(fileName);
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);

			int k = (int) Math.ceil((float) n / (float) m);

			//| CL_MEM_ALLOC_HOST_PTR | CL_MEM_COPY_HOST_PTR
			clData = clCreateBuffer(clContext, CL_MEM_READ_WRITE, 4 * k * m, errcode_ret);
			IntBuffer memData = MemoryUtil.memAllocInt(n);
			memData = CLUtils.toIntBuffer(data, memData);
			clEnqueueWriteBuffer(clQueue, clData, true, 0, memData, null, null);
			recursive_scan(clData, n, dir);
			clEnqueueReadBuffer(clQueue, clData, true, 0, memData, null, null);
			freeCLMemory(clData);

			for(int i = 0; i < n; i++) {
				result[i] = memData.get(i);
			}

			MemoryUtil.memFree(memData);
		}
		clear();

		return result;
	}

	@Override
	protected void clearMemory() throws OpenCLException {}

	@Override
	protected void buildKernels() throws OpenCLException {
		try (MemoryStack stack = stackPush()) {
			IntBuffer errcode_ret = stack.callocInt(1);
			cl_scan_pow2 = clCreateKernel(clProgram, "scan_pow2_wrapper", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			cl_scan_pad_to_pow2 = clCreateKernel(clProgram, "scan_pad_to_pow2", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			cl_scan_subarrays = clCreateKernel(clProgram, "scan_subarrays", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
			cl_scan_inc_subarrays = clCreateKernel(clProgram, "scan_inc_subarrays", errcode_ret);
			CLInfo.checkCLError(errcode_ret);
		}
	}

	@Override
	protected void releaseKernels() throws OpenCLException {}
}
